package org.mtransit.android.commons;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

public final class ColorUtils implements MTLog.Loggable {

	private static final String TAG = ColorUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

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

	public static String toRGBColor(int colorInt) {
		return String.format("#%06X", 0xFFFFFF & colorInt);
	}

	public static float extractHue(int colorInt) {
		float[] hsv = new float[3];
		Color.colorToHSV(colorInt, hsv);
		return hsv[0];
	}

	public static float extractSaturation(int colorInt) {
		float[] hsv = new float[3];
		Color.colorToHSV(colorInt, hsv);
		return hsv[1];
	}

	public static float extractValue(int colorInt) {
		float[] hsv = new float[3];
		Color.colorToHSV(colorInt, hsv);
		return hsv[2];
	}

	public static final Paint getNewPaintColorFilter(int colorInt) {
		Paint paint = new Paint();
		paint.setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.MULTIPLY));
		return paint;
	}

	public static Bitmap colorizeBitmapResource(Context context, int markerColor, int bitmapResId) {
		return colorizeBitmap(markerColor, BitmapFactory.decodeResource(context.getResources(), bitmapResId));
	}

	public static Bitmap colorizeBitmap(int markerColor, Bitmap bitmap) {
		try {
			Bitmap obm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
			Canvas canvas = new Canvas(obm);
			canvas.drawBitmap(bitmap, 0f, 0f, getNewPaintColorFilter(markerColor));
			return obm;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while colorizing bitmap!");
			return bitmap;
		}
	}

	private static int textColorPrimary = -1;

	public static int getTextColorPrimary(Context context) {
		if (textColorPrimary < 0) {
			textColorPrimary = ThemeUtils.resolveColorAttribute(context, android.R.attr.textColorPrimary);
		}
		return textColorPrimary;
	}

	private static int textColorSecondary = -1;

	public static int getTextColorSecondary(Context context) {
		if (textColorSecondary < 0) {
			textColorSecondary = ThemeUtils.resolveColorAttribute(context, android.R.attr.textColorSecondary);
		}
		return textColorSecondary;
	}

	private static int textColorTertiary = -1;

	public static int getTextColorTertiary(Context context) {
		if (textColorTertiary < 0) {
			textColorTertiary = ThemeUtils.resolveColorAttribute(context, android.R.attr.textColorTertiary);
		}
		return textColorTertiary;
	}

	public static int[] getColorScheme(Context context, int... attrIds) {
		if (attrIds == null || attrIds.length == 0) {
			return new int[0];
		}
		int[] colorScheme = new int[attrIds.length];
		for (int i = 0; i < attrIds.length; i++) {
			colorScheme[i] = ThemeUtils.resolveColorAttribute(context, attrIds[i]);
		}
		return colorScheme;
	}

	public static int getDarkerColor(int color1, int color2) {
		return color1 < color2 ? color1 : color2;
	}

	public static int getLighterColor(int color1, int color2) {
		return color1 > color2 ? color1 : color2;
	}

	public static int blendColors(int color1, int color2, float ratio) {
		float inverseRation = 1f - ratio;
		float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
		float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
		float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
		return Color.rgb((int) r, (int) g, (int) b);
	}

}
