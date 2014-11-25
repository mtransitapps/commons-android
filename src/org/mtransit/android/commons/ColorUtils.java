package org.mtransit.android.commons;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

public final class ColorUtils {

	private static HashMap<String, Integer> colorMap = new HashMap<String, Integer>();

	public static int parseColor(String color) {
		if (!colorMap.containsKey(color)) {
			if (color.startsWith("#")) {
				colorMap.put(color, Color.parseColor(color));
			} else {
				colorMap.put(color, Color.parseColor("#" + color));
			}
		}
		return colorMap.get(color);
	}

	public static String toRGBColor(int intColor) {
		return String.format("#%06X", 0xFFFFFF & intColor);
	}

	private static int textColorPrimary = -1;

	public static int getTextColorPrimary(Context context) {
		if (textColorPrimary < 0) {
			textColorPrimary = getThemeAttribute(context, android.R.attr.textColorPrimary);
		}
		return textColorPrimary;
	}

	private static int textColorSecondary = -1;

	public static int getTextColorSecondary(Context context) {
		if (textColorSecondary < 0) {
			textColorSecondary = getThemeAttribute(context, android.R.attr.textColorSecondary);
		}
		return textColorSecondary;
	}

	private static int textColorTertiary = -1;

	public static int getTextColorTertiary(Context context) {
		if (textColorTertiary < 0) {
			textColorTertiary = getThemeAttribute(context, android.R.attr.textColorTertiary);
		}
		return textColorTertiary;
	}

	public static int getThemeAttribute(Context context, int attrId) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(attrId, tv, true);
		return context.getResources().getColor(tv.resourceId);
	}

	public static int getDarkerColor(int color1, int color2) {
		return color1 < color2 ? color1 : color2;
	}

	public static int getLighterColor(int color1, int color2) {
		return color1 > color2 ? color1 : color2;
	}

	public static int blendColors(int color1, int color2, float ratio) {
		final float inverseRation = 1f - ratio;
		float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
		float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
		float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
		return Color.rgb((int) r, (int) g, (int) b);
	}

}
