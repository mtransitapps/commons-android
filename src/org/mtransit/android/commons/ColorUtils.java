package org.mtransit.android.commons;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

public class ColorUtils {

	static Map<String, Integer> colorMap = new HashMap<String, Integer>();

	public static int parseColor(String color) {
		if (!colorMap.containsKey(color)) {
			colorMap.put(color, Color.parseColor("#" + color));
		}
		return colorMap.get(color);
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

	public static int getThemeAttribute(Context context, int resId) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(resId, tv, true);
		return context.getResources().getColor(tv.resourceId);
	}

	public static int getDarkerColor(int color1, int color2) {
		return color1 < color2 ? color1 : color2;
	}
	
	public static int getLighterColor(int color1, int color2) {
		return color1 > color2 ? color1 : color2;
	}

}
