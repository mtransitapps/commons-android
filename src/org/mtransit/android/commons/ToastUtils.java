package org.mtransit.android.commons;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public final class ToastUtils implements MTLog.Loggable {

	private static final String TAG = ToastUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ToastUtils() {
	}

	public static void makeTextAndShowCentered(Context context, int resId) {
		makeTextAndShowCentered(context, resId, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShowCentered(Context context, int resId, int duration) {
		if (context == null) {
			return;
		}
		Toast toast = Toast.makeText(context, resId, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	public static void makeTextAndShow(Context context, int resId) {
		makeTextAndShow(context, resId, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShow(Context context, int resId, int duration) {
		if (context == null) {
			return;
		}
		Toast toast = Toast.makeText(context, resId, duration);
		toast.show();
	}

	public static void makeTextAndShow(Context context, CharSequence text) {
		makeTextAndShow(context, text, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShow(Context context, CharSequence text, int duration) {
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

}
