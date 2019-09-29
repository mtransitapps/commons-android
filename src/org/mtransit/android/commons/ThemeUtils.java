package org.mtransit.android.commons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import android.util.TypedValue;

@SuppressWarnings("unused")
public final class ThemeUtils {

	@ColorInt
	public static int resolveColorAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return ResourcesCompat.getColor(context.getResources(), tv.resourceId, context.getTheme());
	}

	public static float resolveDimensionAttribute(@NonNull Context context, int attrId) {
		TypedValue tv = resolveAttribute(context, attrId);
		return context.getResources().getDimension(tv.resourceId);
	}

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
		int[] attrs = new int[]{attrId};
		TypedArray ta = themedContext.obtainStyledAttributes(attrs);
		Drawable drawableFromTheme = ta.getDrawable(0);
		ta.recycle();
		return drawableFromTheme;
	}
}
