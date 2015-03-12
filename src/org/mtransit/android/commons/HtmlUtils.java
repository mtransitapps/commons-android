package org.mtransit.android.commons;

public final class HtmlUtils {

	public static final String URL_PARAM_AND = "&";

	public static final String URL_PARAM_EQ = "=";

	public static final String BR = "<BR/>";

	public static final String B1 = "<B>";
	public static final String B2 = "</B>";

	public static String applyBold(String html) {
		return B1 + html + B2;
	}

	public static String getFONT_COLOR1(String color) {
		return "<FONT COLOR=\"#" + color + "\">";
	}

	public static String getFONT_COLOR2() {
		return "</FONT>";
	}

	public static String applyFontColor(String html, String color) {
		return getFONT_COLOR1(color) + html + getFONT_COLOR2();
	}
}
