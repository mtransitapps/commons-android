package org.mtransit.android.commons;

import java.util.regex.Pattern;

public final class HtmlUtils implements MTLog.Loggable {

	private static final String TAG = HtmlUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

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

	private static final Pattern REMOVE_BOLD = Pattern.compile(
			"(<strong[^>]*>|</strong>|<h[1-6]{1}>|</h[1-6]{1}>|<span[^>]*>|</span>|font\\-weight\\:[\\s]*bold[;]?)", Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_BOLD_REPLACEMENT = StringUtils.EMPTY;

	public static String removeBold(String html) {
		try {
			return REMOVE_BOLD.matcher(html).replaceAll(REMOVE_BOLD_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while removing bold!");
			return html;
		}
	}

	private static final Pattern LINE_BREAKS = Pattern.compile("(\\n|\\r)", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIX_TEXT_VIEW_BR = Pattern.compile("(<ul[^>]*>|</ul>|</li>|</h[1-6]{1}>|<p[^>]*>|</p>|<div[^>]*>|</div>)",
			Pattern.CASE_INSENSITIVE);

	private static final String FIX_TEXT_VIEW_BR_REPLACEMENT = BR;

	private static final Pattern FIX_TEXT_VIEW_BR2 = Pattern.compile("(<li[^>]*>)", Pattern.CASE_INSENSITIVE);
	private static final String FIX_TEXT_VIEW_BR_REPLACEMENT2 = "- ";

	private static final String BRS_REGEX = "(<br />|<br/>|<br>)";

	private static final Pattern FIX_TEXT_VIEW_BR_DUPLICATE = Pattern.compile("((" + BRS_REGEX + "(\\s|&nbsp;)*)+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern BR_START_ENDS = Pattern.compile("((^" + BRS_REGEX + ")|(" + BRS_REGEX + "$))", Pattern.CASE_INSENSITIVE);

	public static String fixTextViewBR(String html) {
		try {
			html = LINE_BREAKS.matcher(html).replaceAll(StringUtils.EMPTY);
			html = FIX_TEXT_VIEW_BR.matcher(html).replaceAll(FIX_TEXT_VIEW_BR_REPLACEMENT);
			html = FIX_TEXT_VIEW_BR2.matcher(html).replaceAll(FIX_TEXT_VIEW_BR_REPLACEMENT2);
			html = FIX_TEXT_VIEW_BR_DUPLICATE.matcher(html).replaceAll(FIX_TEXT_VIEW_BR_REPLACEMENT);
			html = BR_START_ENDS.matcher(html).replaceAll(StringUtils.EMPTY);
			return html;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while fixing TextView BR!");
			return html;
		}
	}
}
