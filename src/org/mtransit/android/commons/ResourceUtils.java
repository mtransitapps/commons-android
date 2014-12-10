package org.mtransit.android.commons;

import android.content.Context;
import android.util.TypedValue;

public final class ResourceUtils {

	public static float getDimension(Context context, int unit, int value) {
		if (context == null) {
			return value;
		}
		return TypedValue.applyDimension(unit, value, context.getResources().getDisplayMetrics());
	}

	public static float convertSPtoPX(Context context, int sp) {
		return getDimension(context, TypedValue.COMPLEX_UNIT_SP, sp);
	}

}
