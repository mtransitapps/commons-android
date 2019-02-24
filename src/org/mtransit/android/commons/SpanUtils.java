package org.mtransit.android.commons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

public final class SpanUtils implements MTLog.Loggable {

	private static final String TAG = SpanUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public static StyleSpan getNewNormalStyleSpan() {
		return new StyleSpan(android.graphics.Typeface.NORMAL);
	}

	public static StyleSpan getNewBoldStyleSpan() {
		return new StyleSpan(android.graphics.Typeface.BOLD);
	}

	public static final String SANS_SERIF_TYPEFACE = "sans-serif";

	public static TypefaceSpan getNewSansSerifTypefaceSpan() {
		return getNewTypefaceSpan(SANS_SERIF_TYPEFACE);
	}

	public static final String SANS_SERIF_CONDENSED_TYPEFACE = "sans-serif-condensed";

	public static TypefaceSpan getNewSansSerifCondensedTypefaceSpan() {
		return getNewTypefaceSpan(SANS_SERIF_CONDENSED_TYPEFACE);
	}

	public static final String SANS_SERIF_LIGHT_TYPEFACE = "sans-serif-light";

	public static TypefaceSpan getNewSansSerifLightTypefaceSpan() {
		return getNewTypefaceSpan(SANS_SERIF_LIGHT_TYPEFACE);
	}

	public static TypefaceSpan getNewTypefaceSpan(String typeface) {
		return new TypefaceSpan(typeface);
	}

	public static RelativeSizeSpan getNew10PercentSizeSpan() {
		return getNewPercentSizeSpan(0.10f);
	}

	public static RelativeSizeSpan getNew25PercentSizeSpan() {
		return getNewPercentSizeSpan(0.25f);
	}

	public static RelativeSizeSpan getNew50PercentSizeSpan() {
		return getNewPercentSizeSpan(0.50f);
	}

	public static RelativeSizeSpan getNew200PercentSizeSpan() {
		return getNewPercentSizeSpan(2.00f);
	}

	public static RelativeSizeSpan getNewPercentSizeSpan(float percent) {
		return new RelativeSizeSpan(percent);
	}

	@NonNull
	public static TextAppearanceSpan getNewLargeTextAppearance(@NonNull Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Large);
	}

	public static TextAppearanceSpan getNewMediumTextAppearance(@NonNull Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Medium);
	}

	public static TextAppearanceSpan getNewSmallTextAppearance(@NonNull Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Small);
	}

	public static ForegroundColorSpan getNewTextColor(int color) {
		return new ForegroundColorSpan(color);
	}

	public static CharSequence setAll(CharSequence cs, Object... spans) {
		if (cs instanceof SpannableStringBuilder) {
			return setAll((SpannableStringBuilder) cs, spans);
		} else {
			return setAll(new SpannableStringBuilder(cs), spans);
		}
	}

	public static SpannableStringBuilder setAll(SpannableStringBuilder ssb, Object... spans) {
		return set(ssb, 0, ssb == null ? 0 : ssb.length(), spans);
	}

	public static CharSequence set(CharSequence cs, int start, int end, Object... spans) {
		if (cs instanceof SpannableStringBuilder) {
			return set((SpannableStringBuilder) cs, start, end, spans);
		} else {
			return set(new SpannableStringBuilder(cs), start, end, spans);
		}
	}

	public static SpannableStringBuilder set(SpannableStringBuilder ssb, int start, int end, Object... spans) {
		if (ssb == null || ssb.length() == 0 || start >= end) {
			MTLog.w(TAG, "Trying to set span on empty string '%s' or %d not before %d!", ssb, start, end);
			return null;
		}
		if (spans != null && spans.length > 0) {
			for (Object span : spans) {
				ssb.setSpan(span, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			}
		}
		return ssb;
	}
}
