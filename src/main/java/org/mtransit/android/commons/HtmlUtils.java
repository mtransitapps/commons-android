package org.mtransit.android.commons;

import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.util.PatternsCompat;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	@NonNull
	public static Spanned fromHtml(@NonNull String source) {
		return HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	@NonNull
	public static Spanned fromHtmlCompact(@NonNull String source) {
		return HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_COMPACT);
	}

	private static final Pattern IMG_SRC_URL = Pattern.compile("src=\"([^\"]+(\\.png|\\.jpg|\\.jpeg|\\.gif))\"", Pattern.CASE_INSENSITIVE);

	@NonNull
	public static List<String> extractImagesUrls(@NonNull String from, @NonNull CharSequence textHTML) {
		try {
			return extractImagesUrls(URI.create(from), textHTML);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Unexpected error while parsing URI'%s'.", from);
			return Collections.emptyList();
		}
	}

	@NonNull
	public static List<String> extractImagesUrls(@NonNull URI fromURI, @NonNull CharSequence textHTML) {
		try {
			return extractImagesUrls(fromURI.toURL(), textHTML);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Unexpected error while parsing URL'%s'.", fromURI);
			return Collections.emptyList();
		}
	}

	@NonNull
	public static List<String> extractImagesUrls(@NonNull URL fromURL, @NonNull CharSequence textHTML) {
		Matcher matcher;
		try {
			final List<String> imagesUrls = new ArrayList<>();
			matcher = IMG_SRC_URL.matcher(textHTML);
			while (matcher.find()) {
				final String url = matcher.group(1);
				if (url == null || url.isEmpty()) {
					continue;
				}
				imagesUrls.add(new URL(fromURL, url).toString());
			}
			return imagesUrls;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Unexpected error while extracting images URL from '%s'.", textHTML);
			return Collections.emptyList();
		}
	}

	@NonNull
	public static List<String> fromHtmlUrls(@NonNull List<String> htmlUrls) {
		List<String> urls = new ArrayList<>(htmlUrls.size());
		for (String htmlUrl : htmlUrls) {
			try {
				urls.add(HtmlUtils.fromHtmlCompact(htmlUrl).toString());
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Unexpected error while cleaning HTML images URL from '%s'.", htmlUrl);
			}
		}
		return urls;
	}

	private static final Pattern REMOVE_COMMENT = Pattern.compile("(<!--.*?-->)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_COMMENT_REPLACEMENT = StringUtils.EMPTY;

	@NonNull
	public static String removeComments(@NonNull String html) {
		try {
			return REMOVE_COMMENT.matcher(html).replaceAll(REMOVE_COMMENT_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing comment!");
			return html;
		}
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

	private static final Pattern REMOVE_STYLE = Pattern.compile("(<style.*?</style>)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

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

	private static final Pattern REPLACE_IMG = Pattern.compile("(\\s*<img.*?src=\"(.*?)\".*?>\\s*)", Pattern.CASE_INSENSITIVE);

	@NonNull
	public static String replaceImgTagWithUrlLink(@NonNull String from, @NonNull String textHTML) {
		try {
			return replaceImgTagWithUrlLink(URI.create(from), textHTML);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Unexpected error while parsing URI'%s'.", from);
			return textHTML;
		}
	}

	@NonNull
	public static String replaceImgTagWithUrlLink(@NonNull URI fromURI, @NonNull String textHTML) {
		try {
			return replaceImgTagWithUrlLink(fromURI.toURL(), textHTML);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Unexpected error while parsing URL'%s'.", fromURI);
			return textHTML;
		}
	}

	@NonNull
	public static String replaceImgTagWithUrlLink(@NonNull URL fromURL, @NonNull String textHTML) {
		Matcher matcher;
		try {
			StringBuilder textHTMLSb = new StringBuilder();
			int index = 0;
			textHTML = REMOVE_IMG_DATA.matcher(textHTML).replaceAll(REMOVE_IMG_DATA_REPLACEMENT);
			matcher = REPLACE_IMG.matcher(textHTML);
			while (matcher.find()) {
				final String url = matcher.group(2);
				if (url == null || url.isEmpty()) {
					continue;
				}
				String fixedURL = new URL(fromURL, url).toString();
				textHTMLSb
						.append(textHTML.substring(index, matcher.start(1)))
						.append(BR).append(linkify(fixedURL)).append(BR);
				index = matcher.end(1);
			}
			if (index < textHTML.length()) {
				textHTMLSb.append(textHTML.substring(index));
			}
			return textHTMLSb.toString();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while replacing img tag with URL!");
			return textHTML;
		}
	}

	private static final Pattern REMOVE_IMG = Pattern.compile("(<img.*?>)", Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_IMG_REPLACEMENT = StringUtils.EMPTY;

	private static final Pattern REMOVE_IMG_DATA = Pattern.compile("(\\s*<img.*?src=\"data(.*?)\".*?>\\s*)", Pattern.CASE_INSENSITIVE);
	private static final String REMOVE_IMG_DATA_REPLACEMENT = BR;

	@NonNull
	public static String removeImg(@NonNull String html) {
		try {
			return REMOVE_IMG.matcher(html).replaceAll(REMOVE_IMG_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing img tags!");
			return html;
		}
	}

	private static final Pattern REMOVE_SCRIPT = Pattern.compile("(<script.*?</script>)", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private static final String REMOVE_SCRIPT_REPLACEMENT = StringUtils.EMPTY;

	@NonNull
	public static String removeScript(@NonNull String html) {
		try {
			return REMOVE_SCRIPT.matcher(html).replaceAll(REMOVE_SCRIPT_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing style!");
			return html;
		}
	}

	private static final Pattern REMOVE_BOLD = Pattern.compile(
			"(" + STRONG1_REGEX + "|" + STRONG2_REGEX + "|" + H1_REGEX + "|" + H2_REGEX + "|" + SPAN1_REGEX + "|" + SPAN2_REGEX
					+ "|font-weight:\\s*bold;?)", Pattern.CASE_INSENSITIVE);

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
