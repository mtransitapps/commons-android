package org.mtransit.android.commons;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

public final class SpanUtils implements MTLog.Loggable {

	private static final String TAG = SpanUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final StyleSpan BOLD_STYLE_SPAN = new StyleSpan(android.graphics.Typeface.BOLD);

	public static final TypefaceSpan SANS_SERIF_TYPEFACE_SPAN = new TypefaceSpan("sans-serif");

	public static final TypefaceSpan SANS_SERIF_CONDENSED_TYPEFACE_SPAN = new TypefaceSpan("sans-serif-condensed");

	public static final TypefaceSpan SANS_SERIF_LIGHT_TYPEFACE_SPAN = new TypefaceSpan("sans-serif-light");

	public static final RelativeSizeSpan FIFTY_PERCENT_SIZE_SPAN = new RelativeSizeSpan(0.50f);

	public static final RelativeSizeSpan TWENTY_FIVE_PERCENT_SIZE_SPAN = new RelativeSizeSpan(0.25f);

	public static TextAppearanceSpan getLargeTextAppearance(Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Large);
	}

	public static TextAppearanceSpan getMediumTextAppearance(Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Medium);
	}

	public static TextAppearanceSpan getSmallTextAppearance(Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Small);
	}

	public static ForegroundColorSpan getTextColor(int color) {
		return new ForegroundColorSpan(color);
	}

	public static void set(SpannableStringBuilder ssb, Object span) {
		if (ssb == null || ssb.length() == 0) {
			MTLog.w(TAG, "Trying to set span on empty string!");
			return;
		}
		ssb.setSpan(span, 0, ssb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
	}

	public static void set(SpannableStringBuilder ssb, Object span, int start, int end) {
		if (ssb == null || ssb.length() == 0 || start >= end) {
			MTLog.w(TAG, "Trying to set span on empty string or %s not before %s!", start, end);
			return;
		}
		ssb.setSpan(span, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
	}
}
