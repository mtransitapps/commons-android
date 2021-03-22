package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.core.util.PatternsCompat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public final class HtmlUtils implements MTLog.Loggable {

	private static final String LOG_TAG = HtmlUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final String URL_PARAM_AND = "&";

	public static final String URL_PARAM_EQ = "=";

	public static final String BR = "<BR/>";

	public static final String B1 = "<B>";
	public static final String B2 = "</B>";

	private static final String BOLD_FORMAT = B1 + "%s" + B2;

	@NonNull
	public static String applyBold(@NonNull CharSequence html) {
		return String.format(BOLD_FORMAT, html);
	}

	private static final String FONT_COLOR_1_FORMAT = "<FONT COLOR=\"#%s\">";

	private static final String FONT2 = "</FONT>";

	private static final String FONT_COLOR_FORMAT = FONT_COLOR_1_FORMAT + "%s" + FONT2;

	@NonNull
	public static String applyFontColor(@NonNull CharSequence html, @NonNull CharSequence color) {
		return String.format(FONT_COLOR_FORMAT, color, html);
	}

	private static final String LINKIFY = "<A HREF=\"%s\">%s</A>";

	@NonNull
	public static String linkify(@NonNull CharSequence url) {
		return linkify(url, url);
	}

	@NonNull
	public static String linkify(@NonNull CharSequence url, @NonNull CharSequence text) {
		return String.format(LINKIFY, url, text);
	}

	@NonNull
	public static String linkifyAllURLs(@NonNull String text) {
		try {
			final Matcher matcher = PatternsCompat.WEB_URL.matcher(text.toLowerCase(Locale.ENGLISH));
			while (matcher.find()) {
				String url = text.substring(
						matcher.start(),
						matcher.end()
				);
				text = text.replace(url, linkify(url));
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Unexpected error while adding links to '%s'.", text);
		}
		return text;
	}

	private static final Pattern NEW_LINE_REGEX = Pattern.compile("(\n)");

	@NonNull
	public static String toHTML(@NonNull String html) {
		return NEW_LINE_REGEX.matcher(html).replaceAll(BR);
	}

	private static final String DIV1_REGEX = "<div[^>]*>";
	private static final String DIV2_REGEX = "</div>";

	private static final String H1_REGEX = "<h[1-6][^>]*>";
	private static final String H2_REGEX = "</h[1-6]>";

	private static final String LI1_REGEX = "<li[^>]*>";
	private static final String LI2_REGEX = "</li>";

	private static final String P1_REGEX = "<p[^>]*>";
	private static final String P2_REGEX = "</p>";

	private static final String SPAN1_REGEX = "<span[^>]*>";
	private static final String SPAN2_REGEX = "</span>";

	private static final String STRONG1_REGEX = "<strong[^>]*>";
	private static final String STRONG2_REGEX = "</strong>";

	private static final String SUB1_REGEX = "<sub[^>]*>";
	private static final String SUB2_REGEX = "</sub>";

	private static final String SUP1_REGEX = "<sup[^>]*>";
	private static final String SUP2_REGEX = "</sup>";

	private static final String UL1_REGEX = "<ul[^>]*>";
	private static final String UL2_REGEX = "</ul>";

	@NonNull
	public static String removeTables(@NonNull String html) {
		try {
			int tableStart = html.indexOf("<table");
			while (tableStart >= 0) {
				int tableEnds = html.indexOf("</table>", tableStart);
				if (tableEnds >= 0) {
					tableEnds += "</table>".length();
					html = html.substring(0, tableStart) + html.substring(tableEnds);
				} else {
					html = html.substring(0, tableStart);
				}
				tableStart = html.indexOf("<table");
			}
			return html;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing table!");
			return html;
		}
	}

	private static final Pattern REMOVE_STYLE = Pattern.compile("(<style[^>]*>[^<]*</style>)", Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_STYLE_REPLACEMENT = StringUtils.EMPTY;

	@NonNull
	public static String removeStyle(@NonNull String html) {
		try {
			return REMOVE_STYLE.matcher(html).replaceAll(REMOVE_STYLE_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing style!");
			return html;
		}
	}

	private static final Pattern REMOVE_BOLD = Pattern.compile(
			"(" + STRONG1_REGEX + "|" + STRONG2_REGEX + "|" + H1_REGEX + "|" + H2_REGEX + "|" + SPAN1_REGEX + "|" + SPAN2_REGEX
					+ "|font-weight:[\\s]*bold[;]?)", Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_BOLD_REPLACEMENT = StringUtils.EMPTY;

	@NonNull
	public static String removeBold(@NonNull String html) {
		try {
			return REMOVE_BOLD.matcher(html).replaceAll(REMOVE_BOLD_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing bold!");
			return html;
		}
	}

	private static final Pattern REMOVE_SUP_SUB =
			Pattern.compile("(" + SUB1_REGEX + "|" + SUB2_REGEX + "|" + SUP1_REGEX + "|" + SUP2_REGEX + ")", Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_SUP_SUB_REPLACEMENT = StringUtils.EMPTY;

	@NonNull
	public static String removeSupSub(@NonNull String html) {
		try {
			return REMOVE_SUP_SUB.matcher(html).replaceAll(REMOVE_SUP_SUB_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing sub/sup!");
			return html;
		}
	}

	private static final Pattern LINE_BREAKS = Pattern.compile("([\\n\\r])", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIX_TEXT_VIEW_BR = Pattern.compile(
			"(" + UL1_REGEX + "|" + UL2_REGEX + "|" + LI2_REGEX + "|" + H1_REGEX + "|" + H2_REGEX + "|" + P1_REGEX + "|" + P2_REGEX + "|" + DIV1_REGEX + "|"
					+ DIV2_REGEX + ")", Pattern.CASE_INSENSITIVE);

	private static final String FIX_TEXT_VIEW_BR_REPLACEMENT = BR;

	private static final Pattern FIX_TEXT_VIEW_BR2 = Pattern.compile("(" + LI1_REGEX + ")", Pattern.CASE_INSENSITIVE);
	private static final String FIX_TEXT_VIEW_BR_REPLACEMENT2 = "- ";

	private static final String BRS_REGEX = "(<br />|<br/>|<br>)";

	private static final Pattern FIX_TEXT_VIEW_BR_DUPLICATE = Pattern.compile("((" + BRS_REGEX + "(\\s|&nbsp;)*)+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern BR_START_ENDS = Pattern.compile("((^" + BRS_REGEX + ")|(" + BRS_REGEX + "$))", Pattern.CASE_INSENSITIVE);

	@NonNull
	public static String fixTextViewBR(@NonNull String html) {
		try {
			html = LINE_BREAKS.matcher(html).replaceAll(StringUtils.EMPTY);
			html = FIX_TEXT_VIEW_BR.matcher(html).replaceAll(FIX_TEXT_VIEW_BR_REPLACEMENT);
			html = FIX_TEXT_VIEW_BR2.matcher(html).replaceAll(FIX_TEXT_VIEW_BR_REPLACEMENT2);
			html = fixTextViewBRDuplicates(html);
			html = BR_START_ENDS.matcher(html).replaceAll(StringUtils.EMPTY);
			return html;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while fixing TextView BR!");
			return html;
		}
	}

	@NonNull
	public static String fixTextViewBRDuplicates(@NonNull String html) {
		try {
			html = FIX_TEXT_VIEW_BR_DUPLICATE.matcher(html).replaceAll(FIX_TEXT_VIEW_BR_REPLACEMENT);
			return html;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while fixing BR duplicates!");
			return html;
		}
	}
}
