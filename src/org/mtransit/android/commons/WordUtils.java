package org.mtransit.android.commons;

public final class WordUtils {

	// Apache Commons Lang
	public static String capitalize(String str, char[] delimiters) {
		if (str == null || str.length() == 0) {
			return str;
		}
		int strLen = str.length();
		StringBuffer buffer = new StringBuffer(strLen);

		int delimitersLen = 0;
		if (delimiters != null) {
			delimitersLen = delimiters.length;
		}

		boolean capitalizeNext = true;
		for (int i = 0; i < strLen; i++) {
			char ch = str.charAt(i);

			boolean isDelimiter = false;
			if (delimiters == null) {
				isDelimiter = Character.isWhitespace(ch);
			} else {
				for (int j = 0; j < delimitersLen; j++) {
					if (ch == delimiters[j]) {
						isDelimiter = true;
						break;
					}
				}
			}

			if (isDelimiter) {
				buffer.append(ch);
				capitalizeNext = true;
			} else if (capitalizeNext) {
				buffer.append(Character.toTitleCase(ch));
				capitalizeNext = false;
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}

}
