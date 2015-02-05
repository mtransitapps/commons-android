package org.mtransit.android.commons;

import android.content.Context;

public class StringUtils {

	public static final String EMPTY = "";

	public static final char SPACE_CAR = ' ';
	public static final String SPACE_STRING = " ";

	private static final String ELLIPSIZE = "\u2026";

	public static String ellipsize(String string, int size) {
		if (string == null || string.length() < size) {
			return string;
		}
		return string.substring(0, size - ELLIPSIZE.length()) + ELLIPSIZE;
	}

	public static String trim(String string) {
		if (string == null) {
			return null;
		}
		return string.trim();
	}

	public static boolean equals(String str1, String str2) {
		return str1 == null ? str2 == null : str1.equals(str2);
	}

	public static boolean equalsIgnoreCase(String str1, String str2) {
		return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
	}

	public static int getStringIdentifier(Context context, String name) {
		return context.getResources().getIdentifier(name, "string", context.getPackageName());
	}

	public static int getPluralsIdentifier(Context context, String name) {
		return context.getResources().getIdentifier(name, "plurals", context.getPackageName());
	}

	public static String getEmptyOrPlurals(Context context, int emptyRes, int pluralsRes, int quantity) {
		if (quantity == 0) {
			return context.getString(emptyRes);
		} else {
			return context.getResources().getQuantityString(pluralsRes, quantity, quantity);
		}
	}

	public static String getEmptyOrPluralsIdentifier(Context context, String emptyRes, String pluralsRes, int quantity) {
		if (quantity == 0) {
			return context.getString(getStringIdentifier(context, emptyRes));
		} else {
			return context.getResources().getQuantityString(getPluralsIdentifier(context, pluralsRes), quantity, quantity);
		}
	}

	public static String removeNewLine(String string) {
		if (string == null) {
			return null;
		}
		return string.replaceAll("[\n\r]", EMPTY);
	}

	public static String removeStartWith(String string, String[] removeChars) {
		if (string == null || string.length() == 0) {
			return string;
		}
		if (removeChars != null) {
			for (String removeChar : removeChars) {
				if (string.startsWith(removeChar)) {
					return string.substring(removeChar.length());
				}
			}
		}
		return string;
	}

	public static String replaceStartWith(String string, String[] removeChars, String replacement) {
		if (string == null || string.length() == 0) {
			return string;
		}
		if (removeChars != null) {
			for (String removeChar : removeChars) {
				if (string.startsWith(removeChar)) {
					return replacement + string.substring(removeChar.length());
				}
			}
		}
		return string;
	}

	public static String removeStartWith(String string, String[] removeChars, int keepLast) {
		if (string == null || string.length() == 0) {
			return string;
		}
		if (removeChars != null) {
			for (String removeChar : removeChars) {
				if (string.startsWith(removeChar)) {
					return string.substring(removeChar.length() - keepLast);
				}
			}
		}
		return string;
	}

	public static String replaceAll(String string, String[] replaceChars, String replacement) {
		if (string == null || string.length() == 0) {
			return string;
		}
		if (replaceChars != null) {
			for (String replaceChar : replaceChars) {
				string = string.replace(replaceChar, replacement);
			}
		}
		return string;
	}
}
