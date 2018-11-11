package org.mtransit.android.commons;

import org.mtransit.android.commons.api.SupportFactory;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.TypedValue;

public final class ThemeUtils {

	@ColorInt
	public static int resolveColorAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return SupportFactory.get().getColor(context.getResources(), tv.resourceId, context.getTheme());
	}

	public static float resolveDimensionAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return context.getResources().getDimension(tv.resourceId);
	}

	public static Drawable resolveDrawableAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return SupportFactory.get().getResourcesDrawable(context.getResources(), tv.resourceId, context.getTheme());
	}

	private static TypedValue resolveAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(attrId, tv, true);
		return tv;
	}

	public static Drawable obtainStyledDrawable(@NonNull Context themedContext, int attrId) {
		int[] attrs = new int[] { attrId };
		TypedArray ta = themedContext.obtainStyledAttributes(attrs);
		Drawable drawableFromTheme = ta.getDrawable(0);
		ta.recycle();
		return drawableFromTheme;
	}
}
