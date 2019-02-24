package org.mtransit.android.commons;

import org.mtransit.android.commons.api.SupportFactory;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public final class ToastUtils implements MTLog.Loggable {

	private static final String LOG_TAG = ToastUtils.class.getSimpleName();

	public static final int TOAST_MARGIN_IN_DP = 10;
	public static final int NAVIGATION_HEIGHT_IN_DP = 48;

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private ToastUtils() {
	}

	public static void makeTextAndShowCentered(@Nullable Context context, @StringRes int resId) {
		makeTextAndShowCentered(context, resId, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShowCentered(@Nullable Context context, @StringRes int resId, int duration) {
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

	public static void makeTextAndShow(@Nullable Context context, @StringRes int resId) {
		makeTextAndShow(context, resId, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShow(@Nullable Context context, @StringRes int resId, int duration) {
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
		int additionalBottomMarginInDp = 90; // smart ad banner max height
		return showTouchableToast(context, touchableToast, parent, additionalBottomMarginInDp);
	}

	public static boolean showTouchableToast(@Nullable Context context, @Nullable PopupWindow touchableToast, @Nullable View parent, int additionalBottomMarginInDp) {
		return showTouchableToastPx(context, touchableToast, parent,
				(int) ResourceUtils.convertDPtoPX(context, additionalBottomMarginInDp) // additional bottom margin
		);
	}

	public static boolean showTouchableToastPx(@Nullable Context context, @Nullable PopupWindow touchableToast, @Nullable View parent, int additionalBottomMarginInPx) {
		return showTouchableToastPx(context, touchableToast, parent,
				(int) ResourceUtils.convertDPtoPX(context, NAVIGATION_HEIGHT_IN_DP + TOAST_MARGIN_IN_DP) + additionalBottomMarginInPx, // bottom
				(int) ResourceUtils.convertDPtoPX(context, TOAST_MARGIN_IN_DP) // left
		);
	}

	public static boolean showTouchableToast(@Nullable Context context, @Nullable PopupWindow touchableToast, @Nullable View parent, int bottomMarginInDp, int leftMarginInDp) {
		if (context == null || touchableToast == null || parent == null) {
			return false;
		}
		int bottomMarginInPx = (int) ResourceUtils.convertDPtoPX(context, bottomMarginInDp);
		int leftMarginInPx = (int) ResourceUtils.convertDPtoPX(context, leftMarginInDp);
		return showTouchableToastPx(context, touchableToast, parent, bottomMarginInPx, leftMarginInPx);
	}

	public static boolean showTouchableToastPx(@Nullable Context context, @Nullable PopupWindow touchableToast, @Nullable View parent, int bottomMarginInPx, int leftMarginInPx) {
		if (context == null || touchableToast == null || parent == null) {
			return false;
		}
		touchableToast.showAtLocation(parent, Gravity.LEFT | Gravity.BOTTOM, leftMarginInPx, bottomMarginInPx);
		return true;
	}

	@Nullable
	public static PopupWindow getNewTouchableToast(@Nullable Context context, @StringRes int textResId) {
		return getNewTouchableToast(context, android.R.drawable.toast_frame, textResId);
	}

	@Nullable
	public static PopupWindow getNewTouchableToast(@Nullable Context context, @DrawableRes int toastResId, @StringRes int textResId) {
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
			newTouchableToast.setBackgroundDrawable(SupportFactory.get().getResourcesDrawable(context.getResources(), toastResId, null));
			return newTouchableToast;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while creating touchable toast!");
			return null;
		}
	}
}
