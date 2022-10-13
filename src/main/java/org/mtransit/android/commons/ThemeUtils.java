package org.mtransit.android.commons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.core.content.res.ResourcesCompat;

public final class ThemeUtils {

	@ColorInt
	public static int resolveColorAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return ResourcesCompat.getColor(context.getResources(), tv.resourceId, context.getTheme());
	}

	@SuppressWarnings("unused")
	public static float resolveDimensionAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return context.getResources().getDimension(tv.resourceId);
	}

	@SuppressWarnings("unused")
	@Nullable
	public static Drawable resolveDrawableAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return ResourcesCompat.getDrawable(context.getResources(), tv.resourceId, context.getTheme());
	}

	@NonNull
	private static TypedValue resolveAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(attrId, tv, true);
		return tv;
	}

	@Nullable
	public static Drawable obtainStyledDrawable(@NonNull Context themedContext, int attrId) {
		TypedArray ta = themedContext.obtainStyledAttributes(new int[]{attrId});
		Drawable drawableFromTheme;
		try {
			drawableFromTheme = ta.getDrawable(0);
		} finally {
			ta.recycle();
		}
		return drawableFromTheme;
	}

	public static int obtainStyledInteger(@NonNull Context themedContext,
										  @Nullable AttributeSet set,
										  @NonNull @StyleableRes int[] attrs,
										  @StyleableRes int attrId,
										  int defValue) {
		TypedArray a = themedContext.getTheme().obtainStyledAttributes(
				set,
				attrs,
				0, 0);
		int integer;
		try {
			integer = a.getInteger(attrId, defValue);
		} finally {
			a.recycle();
		}
		return integer;
	}
}
