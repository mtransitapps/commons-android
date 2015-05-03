package org.mtransit.android.commons;

import java.util.Locale;

public final class LocaleUtils implements MTLog.Loggable {

	private static final String TAG = LocaleUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String UNKNOWN = "und"; // show all
	public static final String MULTIPLE = "multi"; // try to detect language

	public static boolean isFR() {
		return isFR(Locale.getDefault().getLanguage());
	}

	public static boolean isFR(String language) {
		return Locale.FRENCH.getLanguage().equals(language);
	}

	public static boolean isEN(String language) {
		return Locale.ENGLISH.getLanguage().equals(language);
	}

	public static String getDefaultLanguage() {
		return Locale.getDefault().getLanguage();
	}
}
