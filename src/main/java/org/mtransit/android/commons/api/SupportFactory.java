package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;

public class SupportFactory implements MTLog.Loggable {

	private static final String LOG_TAG = SupportFactory.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private static SupportUtil instance;

	@NonNull
	public static SupportUtil get() {
		if (instance == null) {
			final Package thePackage = SupportFactory.class.getPackage();
			if (thePackage == null) {
				MTLog.e(LOG_TAG, "Can NOT get support factory package!");
				throw new RuntimeException("Can NOT get support factory package!");
			}
			String className = thePackage.getName();
			switch (Build.VERSION.SDK_INT) {
			case Build.VERSION_CODES.BASE: // unsupported versions
			case Build.VERSION_CODES.BASE_1_1:
			case Build.VERSION_CODES.CUPCAKE:
			case Build.VERSION_CODES.DONUT:
			case Build.VERSION_CODES.ECLAIR:
			case Build.VERSION_CODES.ECLAIR_0_1:
			case Build.VERSION_CODES.ECLAIR_MR1:
			case Build.VERSION_CODES.FROYO:
			case Build.VERSION_CODES.GINGERBREAD:
			case Build.VERSION_CODES.GINGERBREAD_MR1:
			case Build.VERSION_CODES.HONEYCOMB:
			case Build.VERSION_CODES.HONEYCOMB_MR1:
			case Build.VERSION_CODES.HONEYCOMB_MR2:
			case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
			case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
			case Build.VERSION_CODES.JELLY_BEAN:
			case Build.VERSION_CODES.JELLY_BEAN_MR1:
			case Build.VERSION_CODES.JELLY_BEAN_MR2:
			case Build.VERSION_CODES.KITKAT:
			case Build.VERSION_CODES.KITKAT_WATCH: // not really supporting this
			case Build.VERSION_CODES.LOLLIPOP:
			case Build.VERSION_CODES.LOLLIPOP_MR1:
			case Build.VERSION_CODES.M:
				MTLog.d(LOG_TAG, "Unknown API Level: " + Build.VERSION.SDK_INT);
				break;
			case Build.VERSION_CODES.N:
				className += ".NougatSupport"; // 24
				break;
			case Build.VERSION_CODES.N_MR1:
				className += ".NougatSupportMR1"; // 25
				break;
			case Build.VERSION_CODES.O:
				className += ".OreoSupport"; // 26
				break;
			case Build.VERSION_CODES.O_MR1:
				className += ".OreoSupportMR1"; // 27
				break;
			case Build.VERSION_CODES.P:
				className += ".PieSupport"; // 28
				break;
			case Build.VERSION_CODES.Q:
				className += ".QAndroid10Support"; // 29
				break;
			case Build.VERSION_CODES.R:
				className += ".RAndroid11Support"; // 30
				break;
			case Build.VERSION_CODES.S:
				className += ".SAndroid12Support"; // 31
				break;
			case Build.VERSION_CODES.S_V2:
				className += ".SV2Android12Support"; // 32
				break;
			case Build.VERSION_CODES.TIRAMISU:
				className += ".TiramisuAndroid13Support"; // 33
				break;
			case Build.VERSION_CODES.UPSIDE_DOWN_CAKE:
				className += ".UpsideDownCakeAndroid14Support"; // 34
				break;
			case Build.VERSION_CODES.VANILLA_ICE_CREAM:
				className += ".VanillaIceCreamAndroid15Support"; // 35
				break;
			case Build.VERSION_CODES.BAKLAVA: // 36
				className += ".BaklavaAndroid16Support"; // 36
				break;
			default:
				MTLog.w(LOG_TAG, "Unknown API Level: %s", Build.VERSION.SDK_INT);
				className += ".BaklavaAndroid16Support"; // default for newer SDK
				break;
			}
			try {
				Class<?> detectorClass = Class.forName(className);
				instance = (SupportUtil) detectorClass.getConstructor().newInstance();
			} catch (Exception e) {
				MTLog.e(LOG_TAG, e, "INTERNAL ERROR!");
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
}
