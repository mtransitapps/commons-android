package org.mtransit.android.commons;

import org.mtransit.android.commons.api.SupportFactory;

import android.content.Context;

public final class StringUtils implements MTLog.Loggable {

	private static final String TAG = StringUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

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

	public static boolean equalsAlphabeticsAndDigits(String str1, String str2) {
		if (str1 == str2) {
			return true;
		}
		int str1Count = str1.length();
		if (str1Count != str2.length()) {
			return false;
		}
		for (int i = 0; i < str1Count; ++i) {
			char c1 = str1.charAt(i);
			char c2 = str2.charAt(i);
			if ((SupportFactory.get().isCharacterAlphabetic(c1) || Character.isDigit(c1)) //
					&& (SupportFactory.get().isCharacterAlphabetic(c2) || Character.isDigit(c2))) {
				if (c1 != c2 && foldCase(c1) != foldCase(c2)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isAlphabeticsOnly(CharSequence str, boolean allowWhitespace) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!SupportFactory.get().isCharacterAlphabetic(str.charAt(i))) {
				if (allowWhitespace && Character.isWhitespace(str.charAt(i))) {
					continue;
				}
				return false;
			}
		}
		return true;
	}

	private static char foldCase(char ch) {
		if (ch < 128) {
			if ('A' <= ch && ch <= 'Z') {
				return (char) (ch + ('a' - 'A'));
			}
			return ch;
		}
		return Character.toLowerCase(Character.toUpperCase(ch));
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
