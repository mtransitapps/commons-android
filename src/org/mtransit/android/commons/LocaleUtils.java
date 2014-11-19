package org.mtransit.android.commons;

import java.util.HashSet;
import java.util.Locale;

public final class LocaleUtils implements MTLog.Loggable {

	private static final String TAG = LocaleUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

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

}
