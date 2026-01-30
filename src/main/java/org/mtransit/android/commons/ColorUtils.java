package org.mtransit.android.commons;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class ColorUtils implements MTLog.Loggable {

	private static final String LOG_TAG = ColorUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@ColorInt
	private static final int ANDROID_GREEN = Color.rgb(164, 198, 57); // #a4c639
	public static final String BLACK = "000000";
	public static final String WHITE = "FFFFFF";

	private static final String NUMBER_SIGN = "#";

	private static final double TOO_LIGHT_LUMINANCE = 0.90d;
	private static final double TOO_LIGHT_TO_DARKEN = 0.97d;

	private static final double TOO_DARK_LUMINANCE = 0.10d;
	private static final double TOO_DARK_TO_LIGHTEN = 0.03d;

	@NonNull
	private static final ArrayMap<String, Integer> colorMap = new ArrayMap<>();

	@ColorInt
	public static int parseColor(@NonNull String color) {
		try {
			if (!color.startsWith(NUMBER_SIGN)) {
				color = NUMBER_SIGN + color;
			}
			Integer colorResId = colorMap.get(color);
			if (colorResId == null) {
				colorResId = Color.parseColor(color);
				colorMap.put(color, colorResId);
			}
			return colorResId;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while parsing color '%s'!", color);
			return Color.BLACK;
		}
	}

	private static final String TO_RGB = "#%06X";

	@NonNull
	public static String toRGBColor(@ColorInt int colorInt) {
		return String.format(TO_RGB, 0xFFFFFF & colorInt);
	}

	public static float extractHue(@ColorInt int colorInt) {
		float[] hsv = new float[3];
		Color.colorToHSV(colorInt, hsv);
		return hsv[0];
	}

	public static float extractSaturation(@ColorInt int colorInt) {
		float[] hsv = new float[3];
		Color.colorToHSV(colorInt, hsv);
		return hsv[1];
	}

	public static float extractValue(@ColorInt int colorInt) {
		float[] hsv = new float[3];
		Color.colorToHSV(colorInt, hsv);
		return hsv[2];
	}

	@NonNull
	public static Paint getNewPaintColorFilter(@ColorInt int colorInt) {
		Paint paint = new Paint();
		paint.setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.MULTIPLY));
		return paint;
	}

	@Nullable
	public static Bitmap replaceColor(@Nullable Bitmap src, int replaceColor, int targetColor) {
		if (src == null) return null;
		// real all pixels once
		final int width = src.getWidth();
		final int height = src.getHeight();
		final int[] pixels = new int[width * height];
		src.getPixels(pixels, 0, width, 0, 0, width, height);
		// replace pixels with target color
		for (int x = 0; x < pixels.length; ++x) {
			if (pixels[x] == replaceColor) {
				pixels[x] = targetColor;
			} else if ((pixels[x] & 0x00FFFFFF) == replaceColor) {
				pixels[x] = (pixels[x] & 0xFF000000) | targetColor;
			}
		}
		// make new bitmap with updated pixels
		final Bitmap result = Bitmap.createBitmap(width, height, src.getConfig() != null ? src.getConfig() : Bitmap.Config.ARGB_8888);
		result.setPixels(pixels, 0, width, 0, 0, width, height);
		return result;
	}

	@Nullable
	public static Bitmap colorizeBitmapResource(@Nullable Context context,
												@ColorInt int markerColor,
												@DrawableRes int bitmapResId,
												boolean replaceColor) {
		if (context == null) {
			return null;
		}
		if (replaceColor) {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inScaled = false; // do not create new colors by scaling image
			return replaceColor(
					BitmapFactory.decodeResource(context.getResources(), bitmapResId, bitmapOptions),
					ANDROID_GREEN,
					markerColor
			);
		}
		return colorizeBitmap(
				markerColor,
				BitmapFactory.decodeResource(context.getResources(), bitmapResId)
		);
	}

	@ColorInt
	public static int manipulateColor(int color, float factor) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * factor);
		int g = Math.round(Color.green(color) * factor);
		int b = Math.round(Color.blue(color) * factor);
		return Color.argb(a,
				Math.min(r, 255),
				Math.min(g, 255),
				Math.min(b, 255));
	}

	@ColorInt
	public static int getThemeContrastColor(@Nullable Context context) {
		return context != null && ColorUtils.isDarkTheme(context) ? Color.WHITE : Color.BLACK;
	}

	@NonNull
	public static Bitmap colorizeBitmap(@ColorInt int markerColor, @NonNull Bitmap bitmap) {
		try {
			Bitmap obm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(obm);
			canvas.drawBitmap(bitmap, 0f, 0f, getNewPaintColorFilter(markerColor));
			return obm;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while colorizing bitmap!");
			return bitmap;
		}
	}

	@Nullable
	@ColorInt
	private static Integer textColorPrimary = null;

	@ColorInt
	public static int getTextColorPrimary(@NonNull Context context) {
		if (textColorPrimary == null) {
			textColorPrimary = ThemeUtils.resolveColorAttribute(context, android.R.attr.textColorPrimary);
		}
		return textColorPrimary;
	}

	@Nullable
	@ColorInt
	private static Integer textColorSecondary = null;

	@ColorInt
	public static int getTextColorSecondary(@NonNull Context context) {
		if (textColorSecondary == null) {
			textColorSecondary = ThemeUtils.resolveColorAttribute(context, android.R.attr.textColorSecondary);
		}
		return textColorSecondary;
	}

	@Nullable
	@ColorInt
	private static Integer textColorTertiary = null;

	@ColorInt
	public static int getTextColorTertiary(@NonNull Context context) {
		if (textColorTertiary == null) {
			textColorTertiary = ThemeUtils.resolveColorAttribute(context, android.R.attr.textColorTertiary);
		}
		return textColorTertiary;
	}

	public static void resetColorCache() {
		textColorPrimary = null;
		textColorSecondary = null;
		textColorTertiary = null;
	}

	@NonNull
	public static int[] getColorScheme(@NonNull Context context, @Nullable int... attrIds) {
		if (attrIds == null || attrIds.length == 0) {
			return new int[0];
		}
		int[] colorScheme = new int[attrIds.length];
		for (int i = 0; i < attrIds.length; i++) {
			colorScheme[i] = ThemeUtils.resolveColorAttribute(context, attrIds[i]);
		}
		return colorScheme;
	}

	@ColorInt
	public static int getDarkerColor(@ColorInt int color1, @ColorInt int color2) {
		return color1 < color2 ? color1 : color2;
	}

	@ColorInt
	public static int getLighterColor(@ColorInt int color1, @ColorInt int color2) {
		return color1 > color2 ? color1 : color2;
	}

	@ColorInt
	public static int blendColors(int color1, int color2, float ratio) {
		float inverseRation = 1f - ratio;
		float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
		float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
		float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
		return Color.rgb((int) r, (int) g, (int) b);
	}

	@ColorInt
	public static int adaptColorToTheme(@Nullable Context context, @ColorInt int color) {
		if (context == null) {
			return color;
		}
		if (isDarkTheme(context)) {
			if (isTooDark(color)) {
				color = lightenColor(color);
			}
		} else { // isLightTheme()
			if (isTooLight(color)) {
				color = darkenColor(color);
			}
		}
		return color;
	}

	public static boolean isTooDarkForDarkTheme(@NonNull Context context, @ColorInt int color) {
		if (!isDarkTheme(context)) {
			return false;
		}
		return isTooDark(color);
	}

	public static boolean isTooDark(@ColorInt int color) {
		return calculateLuminance(color) < TOO_DARK_LUMINANCE;
	}

	public static boolean isTooLightForLightTheme(@NonNull Context context, @ColorInt int color) {
		if (!isLightTheme(context)) {
			return false;
		}
		return isTooLight(color);
	}

	public static boolean isTooLight(@ColorInt int color) {
		return calculateLuminance(color) > TOO_LIGHT_LUMINANCE;
	}

	public static double calculateLuminance(@ColorInt int color) {
		return androidx.core.graphics.ColorUtils.calculateLuminance(color);
	}

	public static boolean isLightTheme(@NonNull Context context) {
		return !isDarkTheme(context);
	}

	public static boolean isLightTheme(int uiMode) {
		return !isDarkTheme(uiMode);
	}

	public static boolean isDarkTheme(@NonNull Context context) {
		return isDarkTheme(context.getResources().getConfiguration().uiMode);
	}

	public static boolean isDarkTheme(int uiMode) {
		return getUiModeFlag(uiMode) == Configuration.UI_MODE_NIGHT_YES;
	}

	public static int getUiModeFlag(int uiMode) {
		return uiMode & Configuration.UI_MODE_NIGHT_MASK;
	}

	@ColorInt
	public static int darkenColor(@ColorInt int color) {
		if (calculateLuminance(color) > TOO_LIGHT_TO_DARKEN) {
			return Color.DKGRAY;
		}
		return darkenColor(color, 0.1F);
	}

	private static final int LIGHTNESS = 2;

	@ColorInt
	public static int darkenColor(@ColorInt int color, float value) {
		float[] outHsl = new float[3];
		androidx.core.graphics.ColorUtils.colorToHSL(color, outHsl);
		outHsl[LIGHTNESS] -= value;
		outHsl[LIGHTNESS] = Math.max(0f, Math.min(outHsl[LIGHTNESS], 1f));
		return androidx.core.graphics.ColorUtils.HSLToColor(outHsl);
	}

	@ColorInt
	public static int lightenColor(@ColorInt int color) {
		if (calculateLuminance(color) < TOO_DARK_TO_LIGHTEN) {
			return Color.LTGRAY;
		}
		return lightenColor(color, 0.1F);
	}

	@ColorInt
	public static int lightenColor(@ColorInt int color, float value) {
		float[] outHsl = new float[3];
		androidx.core.graphics.ColorUtils.colorToHSL(color, outHsl);
		outHsl[2] += value;
		outHsl[2] = Math.max(0f, Math.min(outHsl[2], 1f));
		return androidx.core.graphics.ColorUtils.HSLToColor(outHsl);
	}

	@NonNull
	public static List<Integer> filterColors(@NonNull List<Integer> colorArray, int max) {
		boolean first = true;
		while (colorArray.size() > max) {
			colorArray.remove(first ? 0 : colorArray.size() - 1);
			first = !first;
		}
		return colorArray;
	}
}
