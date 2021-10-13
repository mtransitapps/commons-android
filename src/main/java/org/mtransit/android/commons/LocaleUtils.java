package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

@SuppressWarnings({"WeakerAccess"})
public final class LocaleUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LocaleUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final String UNKNOWN = "und"; // show all
	public static final String MULTIPLE = "multi"; // try to detect language

	@Nullable
	private static Locale defaultLocale;

	public static void setDefaultLocale(@NonNull Locale newDefaultLocale) {
		defaultLocale = newDefaultLocale;
	}

	@NonNull
	public static Locale getDefaultLocale() {
		if (defaultLocale == null) {
			defaultLocale = Locale.getDefault();
		}
		return defaultLocale;
	}

	@NonNull
	public static Context attachBaseContextApplication(@NonNull Context newBase) {
		setDefaultLocale(getPrimaryLocale(newBase));
		return newBase;
	}

	@NonNull
	public static Context attachBaseContextActivity(@NonNull Context newBase) {
		setDefaultLocale(getPrimaryLocale(newBase));
		// DO NOTHING
		return newBase;
	}

	public static void attachBaseContextActivityAfter(@SuppressWarnings("unused") @NonNull Activity activity) {
		final Configuration configuration = new Configuration();
		activity.applyOverrideConfiguration(
				fixDefaultLocale(configuration)
		);
	}

	public static void onCreateActivity(@SuppressWarnings("unused") @NonNull Activity activity) {
		// DO NOTHING
	}

	@NonNull
	public static Configuration fixDefaultLocale(@NonNull Configuration configuration) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // fix default locale after Chrome's WebView mess with it
			Locale defaultLocale = getDefaultLocale();
			configuration.setLocale(defaultLocale);
			Locale.setDefault(defaultLocale);
		}
		return configuration;
	}

	@NonNull
	private static LocaleListCompat getLocales(@NonNull Context context) {
		return ConfigurationCompat.getLocales(context.getResources().getConfiguration());
	}

	@NonNull
	private static Locale getPrimaryLocale(@NonNull Context context) {
		return getLocales(context).get(0);
	}

	public static boolean isFR() {
		return isFR(Locale.getDefault().getLanguage());
	}

	public static boolean isFR(@NonNull Locale locale) {
		return isFR(locale.getLanguage());
	}

	public static boolean isFR(@Nullable String language) {
		return Locale.FRENCH.getLanguage().equals(language);
	}

	public static boolean isEN(@NonNull Locale locale) {
		return isEN(locale.getLanguage());
	}

	public static boolean isEN(@Nullable String language) {
		return Locale.ENGLISH.getLanguage().equals(language);
	}

	@NonNull
	public static String getDefaultLanguage() {
		return Locale.getDefault().getLanguage();
	}

	@NonNull
	public static Locale getSupportedDefaultLocale() {
		Locale defaultLocale = Locale.getDefault();
		if (isFR(defaultLocale.getLanguage())
				|| isEN(defaultLocale.getLanguage())) {
			return defaultLocale;
		}
		return Locale.ENGLISH;
	}
}
