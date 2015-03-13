package org.mtransit.android.commons;

import java.util.HashSet;
import java.util.Locale;

public final class LocaleUtils implements MTLog.Loggable {

	private static final String TAG = LocaleUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String UNKNOWN = "und"; // show all
	public static final String MULTIPLE = "multi"; // try to detect language

	private static final HashSet<Locale> FR;
	static {
		HashSet<Locale> set = new HashSet<Locale>();
		set.add(Locale.FRENCH); // fr
		set.add(Locale.CANADA_FRENCH); // fr_CA
		set.add(Locale.FRANCE); // fr_FR
		FR = set;
	}

	public static boolean isFR() {
		return FR.contains(Locale.getDefault());
	}

	private static final HashSet<String> FR_LANG;
	static {
		HashSet<String> set = new HashSet<String>();
		set.add(Locale.FRENCH.getLanguage()); // fr
		set.add(Locale.CANADA_FRENCH.getLanguage()); // fr_CA
		set.add(Locale.FRANCE.getLanguage()); // fr_FR
		FR_LANG = set;
	}

	public static boolean isFR(String language) {
		return FR_LANG.contains(language);
	}

	private static final HashSet<String> EN_LANG;
	static {
		HashSet<String> set = new HashSet<String>();
		set.add(Locale.ENGLISH.getLanguage()); // en
		set.add(Locale.CANADA.getLanguage()); // en_CA
		set.add(Locale.UK.getLanguage()); // en_GB
		set.add(Locale.US.getLanguage()); // en_US
		EN_LANG = set;
	}

	public static boolean isEN(String language) {
		return EN_LANG.contains(language);
	}
}
