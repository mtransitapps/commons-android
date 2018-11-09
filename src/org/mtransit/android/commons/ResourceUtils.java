package org.mtransit.android.commons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;

public final class ResourceUtils {

	public static float getDimension(@Nullable Context context, int unit, int value) {
		if (context == null) {
			return value;
		}
		return TypedValue.applyDimension(unit, value, context.getResources().getDisplayMetrics());
	}

	public static float convertSPtoPX(@Nullable Context context, int sp) {
		return getDimension(context, TypedValue.COMPLEX_UNIT_SP, sp);
	}

	public static float convertDPtoPX(@Nullable Context context, int sp) {
		return getDimension(context, TypedValue.COMPLEX_UNIT_DIP, sp);
	}

	public static int convertPXtoDP(@NonNull Context context, int px) {
		return (int) (px / context.getResources().getDisplayMetrics().density);
	}
}
