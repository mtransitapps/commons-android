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

	public static int getTextColorPrimary(Context context) {
		return getThemeAttribute(context, android.R.attr.textColorPrimary);
	}

	public static int getTextColorSecondary(Context context) {
		return getThemeAttribute(context, android.R.attr.textColorSecondary);
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
