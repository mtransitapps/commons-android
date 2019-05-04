package org.mtransit.android.commons;

import java.util.Locale;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("unused")
public final class LocaleUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LocaleUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final String UNKNOWN = "und"; // show all
	public static final String MULTIPLE = "multi"; // try to detect language

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
