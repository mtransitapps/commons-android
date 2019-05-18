package org.mtransit.android.commons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class SpanUtils implements MTLog.Loggable {

	private static final String LOG_TAG = SpanUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static StyleSpan getNewNormalStyleSpan() {
		return new StyleSpan(android.graphics.Typeface.NORMAL);
	}

	@NonNull
	public static StyleSpan getNewBoldStyleSpan() {
		return new StyleSpan(android.graphics.Typeface.BOLD);
	}

	public static final String SANS_SERIF_TYPEFACE = "sans-serif";

	@NonNull
	public static TypefaceSpan getNewSansSerifTypefaceSpan() {
		return getNewTypefaceSpan(SANS_SERIF_TYPEFACE);
	}

	public static final String SANS_SERIF_CONDENSED_TYPEFACE = "sans-serif-condensed";

	@NonNull
	public static TypefaceSpan getNewSansSerifCondensedTypefaceSpan() {
		return getNewTypefaceSpan(SANS_SERIF_CONDENSED_TYPEFACE);
	}

	public static final String SANS_SERIF_LIGHT_TYPEFACE = "sans-serif-light";

	@NonNull
	public static TypefaceSpan getNewSansSerifLightTypefaceSpan() {
		return getNewTypefaceSpan(SANS_SERIF_LIGHT_TYPEFACE);
	}

	@NonNull
	public static TypefaceSpan getNewTypefaceSpan(@Nullable String typeface) {
		return new TypefaceSpan(typeface);
	}

	@NonNull
	public static RelativeSizeSpan getNew10PercentSizeSpan() {
		return getNewPercentSizeSpan(0.10f);
	}

	@NonNull
	public static RelativeSizeSpan getNew25PercentSizeSpan() {
		return getNewPercentSizeSpan(0.25f);
	}

	@NonNull
	public static RelativeSizeSpan getNew50PercentSizeSpan() {
		return getNewPercentSizeSpan(0.50f);
	}

	@NonNull
	public static RelativeSizeSpan getNew200PercentSizeSpan() {
		return getNewPercentSizeSpan(2.00f);
	}

	@NonNull
	public static RelativeSizeSpan getNewPercentSizeSpan(float percent) {
		return new RelativeSizeSpan(percent);
	}

	@NonNull
	public static TextAppearanceSpan getNewLargeTextAppearance(@NonNull Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Large);
	}

	@NonNull
	public static TextAppearanceSpan getNewMediumTextAppearance(@NonNull Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Medium);
	}

	@NonNull
	public static TextAppearanceSpan getNewSmallTextAppearance(@NonNull Context context) {
		return new TextAppearanceSpan(context, android.R.style.TextAppearance_Small);
	}

	@NonNull
	public static ForegroundColorSpan getNewTextColor(int color) {
		return new ForegroundColorSpan(color);
	}

	@Nullable
	public static CharSequence setAll(@Nullable CharSequence cs, @Nullable Object... spans) {
		if (cs instanceof SpannableStringBuilder) {
			return setAll((SpannableStringBuilder) cs, spans);
		} else {
			return setAll(new SpannableStringBuilder(cs), spans);
		}
	}

	@Nullable
	public static SpannableStringBuilder setAll(@Nullable SpannableStringBuilder ssb, @Nullable Object... spans) {
		return set(ssb, 0, ssb == null ? 0 : ssb.length(), spans);
	}

	@Nullable
	public static CharSequence set(@Nullable CharSequence cs, int start, int end, @Nullable Object... spans) {
		if (cs instanceof SpannableStringBuilder) {
			return set((SpannableStringBuilder) cs, start, end, spans);
		} else {
			return set(new SpannableStringBuilder(cs), start, end, spans);
		}
	}

	@Nullable
	public static SpannableStringBuilder set(@Nullable SpannableStringBuilder ssb, int start, int end, @Nullable Object... spans) {
		if (ssb == null || ssb.length() == 0 || start >= end) {
			MTLog.w(LOG_TAG, "Trying to set span on empty string or %s not before %s!", start, end);
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
