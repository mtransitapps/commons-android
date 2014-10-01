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

	public static final void makeTextAndShowCentered(Context context, int resId) {
		makeTextAndShowCentered(context, resId, Toast.LENGTH_SHORT);
	}

	public static final void makeTextAndShowCentered(Context context, int resId, int duration) {
		final Toast toast = Toast.makeText(context, resId, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	public static final void makeTextAndShow(Context context, int resId) {
		makeTextAndShow(context, resId, Toast.LENGTH_SHORT);
	}

	public static final void makeTextAndShow(Context context, int resId, int duration) {
		final Toast toast = Toast.makeText(context, resId, duration);
		toast.show();
	}

	public static final void makeTextAndShow(Context context, CharSequence text) {
		makeTextAndShow(context, text, Toast.LENGTH_SHORT);
	}

	public static final void makeTextAndShow(Context context, CharSequence text, int duration) {
		final Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

}
