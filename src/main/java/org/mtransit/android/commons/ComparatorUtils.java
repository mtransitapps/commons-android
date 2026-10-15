package org.mtransit.android.commons;

import androidx.annotation.NonNull;

public final class ComparatorUtils {

	public static final int SAME = 0;

	public static final int BEFORE = -1;

	public static final int AFTER = +1;

	public static boolean isSame(int another) {
		return another == SAME;
	}

	public static boolean isBefore(int another) {
		return another <= BEFORE;
	}

	public static boolean isAfter(int another) {
		return AFTER <= another;
	}

	@SuppressWarnings("unused")
	@NonNull
	public static String toString(int compareTo) {
		if (isAfter(compareTo)) return "AFTER";
		if (isBefore(compareTo)) return "BEFORE";
		return "SAME";
	}
}
