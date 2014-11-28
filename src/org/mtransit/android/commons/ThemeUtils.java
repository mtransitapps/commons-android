package org.mtransit.android.commons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public final class ThemeUtils {

	public static int resolveColorAttribute(Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return context.getResources().getColor(tv.resourceId);
	}

	public static float resolveDimensionAttribute(Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return context.getResources().getDimension(tv.resourceId);
	}

	public static Drawable resolveDrawableAttribute(Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return context.getResources().getDrawable(tv.resourceId);
	}

	private static TypedValue resolveAttribute(Context context, int attrId) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(attrId, tv, true);
		return tv;
	}

	public static Drawable obtainStyledDrawable(Context themedContext, int attrId) {
		int[] attrs = new int[] { attrId };
		TypedArray ta = themedContext.obtainStyledAttributes(attrs);
		Drawable drawableFromTheme = ta.getDrawable(0);
		ta.recycle();
		return drawableFromTheme;
	}
}
