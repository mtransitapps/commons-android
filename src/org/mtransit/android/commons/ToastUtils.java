package org.mtransit.android.commons;

import org.mtransit.android.commons.api.SupportFactory;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public final class ToastUtils implements MTLog.Loggable {

	private static final String TAG = ToastUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ToastUtils() {
	}

	public static void makeTextAndShowCentered(@Nullable Context context, int resId) {
		makeTextAndShowCentered(context, resId, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShowCentered(@Nullable Context context, int resId, int duration) {
		if (context == null) {
			return;
		}
		Toast toast = Toast.makeText(context, resId, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	public static void makeTextAndShowCentered(@Nullable Context context, CharSequence text) {
		makeTextAndShowCentered(context, text, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShowCentered(@Nullable Context context, CharSequence text, int duration) {
		if (context == null) {
			return;
		}
		Toast toast = Toast.makeText(context, text, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	public static void makeTextAndShow(@Nullable Context context, int resId) {
		makeTextAndShow(context, resId, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShow(@Nullable Context context, int resId, int duration) {
		if (context == null) {
			return;
		}
		Toast toast = Toast.makeText(context, resId, duration);
		toast.show();
	}

	public static void makeTextAndShow(@NonNull Context context, CharSequence text) {
		makeTextAndShow(context, text, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShow(@NonNull Context context, CharSequence text, int duration) {
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	public static boolean showTouchableToast(@Nullable Context context, @Nullable PopupWindow touchableToast, @Nullable View parent) {
		if (context == null || touchableToast == null || parent == null) {
			return false;
		}
		int bottomPaddingInPx = (int) ResourceUtils.convertSPtoPX(context, 110);
		int leftMarginInPx = (int) ResourceUtils.convertSPtoPX(context, 10);
		touchableToast.showAtLocation(parent, Gravity.LEFT | Gravity.BOTTOM, leftMarginInPx, bottomPaddingInPx);
		return true;
	}

	@Nullable
	public static PopupWindow getNewTouchableToast(@Nullable Context context, int textResId) {
		if (context == null) {
			return null;
		}
		try {
			TextView contentView = new TextView(context);
			contentView.setText(textResId);
			contentView.setTextColor(Color.WHITE);
			PopupWindow newTouchableToast = new PopupWindow(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
			newTouchableToast.setContentView(contentView);
			newTouchableToast.setTouchable(true);
			newTouchableToast.setBackgroundDrawable(SupportFactory.get().getResourcesDrawable(context.getResources(), android.R.drawable.toast_frame, null));
			return newTouchableToast;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while creating touchable toast!");
			return null;
		}
	}
}
