package org.mtransit.android.commons;

import java.util.regex.Pattern;

public final class HtmlUtils {

	public static final String URL_PARAM_AND = "&";

	public static final String URL_PARAM_EQ = "=";

	public static final String BR = "<BR/>";

	public static final String B1 = "<B>";
	public static final String B2 = "</B>";

	private static final String BOLD_FORMAT = B1 + "%s" + B2;

	public static String applyBold(CharSequence html) {
		return String.format(BOLD_FORMAT, html);
	}

	private static final String FONT_COLOR_1_FORMAT = "<FONT COLOR=\"#%s\">";

	private static final String FONT2 = "</FONT>";

	private static final String FONT_COLOR_FORMAT = FONT_COLOR_1_FORMAT + "%s" + FONT2;

	public static String applyFontColor(CharSequence html, CharSequence color) {
		return String.format(FONT_COLOR_FORMAT, color, html);
	}

	private static final String LINKIFY = "<A HREF=\"%s\">%s</A>";

	public static String linkify(CharSequence url) {
		return String.format(LINKIFY, url, url);
	}

	private static final Pattern NEW_LINE_REGEX = Pattern.compile("(\n)");

	public static String toHTML(String html) {
		return NEW_LINE_REGEX.matcher(html).replaceAll(BR);
	}
}
