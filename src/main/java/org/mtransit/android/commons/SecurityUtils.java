package org.mtransit.android.commons;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public final class SecurityUtils implements MTLog.Loggable {

	private static final String LOG_TAG = SecurityUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void logCertPathValidatorException(@NonNull SSLHandshakeException sslhe) {
		try {
			Throwable innerCause = sslhe.getCause();
			if (innerCause instanceof CertificateException) {
				innerCause = innerCause.getCause();
			}
			if (innerCause instanceof CertPathValidatorException) {
				CertPathValidatorException cpve = (CertPathValidatorException) innerCause;
				String msg = "[CERT PATH: " + cpve.getCertPath() + "]";
				android.util.Log.d(LOG_TAG, msg); // breaks String formatter => android.util.Log
			}
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "Error while trying to log Certificate Path Validator exception!");
		}
	}

	@Nullable
	public static SSLSocketFactory getSSLSocketFactory(@NonNull Context context, @RawRes int certRawId) {
		try {
			// Load CAs from an InputStream
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			// From a CRT file
			Certificate ca;
			try (InputStream caInput = new BufferedInputStream(context.getResources().openRawResource(certRawId))) {
				ca = cf.generateCertificate(caInput);
				MTLog.d(LOG_TAG, "ca=" + ((X509Certificate) ca).getSubjectDN());
			}
			// Create a KeyStore containing our trusted CAs
			String keyStoreType = KeyStore.getDefaultType();
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);
			// Create a TrustManager that trusts the CAs in our KeyStore
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);
			// Create an SSLContext that uses our TrustManager
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
			return sslContext.getSocketFactory();
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "Error while loading SSL certificate!");
			return null;
		}
	}
}
