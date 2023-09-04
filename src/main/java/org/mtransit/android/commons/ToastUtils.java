package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

@SuppressWarnings({"SameParameterValue", "WeakerAccess", "unused"})
public final class ToastUtils implements MTLog.Loggable {

	private static final String LOG_TAG = ToastUtils.class.getSimpleName();

	private static final int TOAST_MARGIN_IN_DP = 10;
	private static final int NAVIGATION_HEIGHT_IN_DP = 48;

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
		setGravityTextCenter(toast);
		toast.show();
	}

	public static void makeTextAndShowCentered(@Nullable Context context, @NonNull CharSequence text) {
		makeTextAndShowCentered(context, text, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShowCentered(@Nullable Context context, @NonNull CharSequence text, int duration) {
		if (context == null) {
			return;
		}
		Toast toast = Toast.makeText(context, text, duration);
		setGravityTextCenter(toast);
		toast.show();
	}

	/**
	 * Android SDK:
	 * <p><strong>Warning:</strong> Starting from Android {@link Build.VERSION_CODES#R}, for apps
	 * targeting API level {@link Build.VERSION_CODES#R} or higher, this method is a no-op when
	 * called on text toasts.
	 */
	private static void setGravityTextCenter(Toast toast) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return;
		}
		toast.setGravity(Gravity.CENTER, 0, 0);
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

	public static void makeTextAndShow(@NonNull Context context, @NonNull CharSequence text) {
		makeTextAndShow(context, text, Toast.LENGTH_SHORT);
	}

	public static void makeTextAndShow(@NonNull Context context, @NonNull CharSequence text, int duration) {
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	public static boolean showTouchableToast(@Nullable Activity activity, @Nullable PopupWindow touchableToast, @Nullable View parent) {
		int additionalBottomMarginInDp = 90; // smart ad banner max height
		return showTouchableToast(activity, touchableToast, parent, additionalBottomMarginInDp);
	}

	public static boolean showTouchableToast(@Nullable Activity activity, @Nullable PopupWindow touchableToast, @Nullable View parent, int additionalBottomMarginInDp) {
		return showTouchableToastPx(activity, touchableToast, parent,
				(int) ResourceUtils.convertDPtoPX(activity, additionalBottomMarginInDp) // additional bottom margin
		);
	}

	public static boolean showTouchableToastPx(@Nullable Activity activity, @Nullable PopupWindow touchableToast, @Nullable View parent, int additionalBottomMarginInPx) {
		return showTouchableToastPx(activity, touchableToast, parent,
				(int) ResourceUtils.convertDPtoPX(activity, NAVIGATION_HEIGHT_IN_DP + TOAST_MARGIN_IN_DP) + additionalBottomMarginInPx, // bottom
				(int) ResourceUtils.convertDPtoPX(activity, TOAST_MARGIN_IN_DP) // start
		);
	}

	public static boolean showTouchableToast(@Nullable Activity activity, @Nullable PopupWindow touchableToast, @Nullable View parent, int bottomMarginInDp, int startMarginInDp) {
		if (activity == null || touchableToast == null || parent == null) {
			return false;
		}
		int bottomMarginInPx = (int) ResourceUtils.convertDPtoPX(activity, bottomMarginInDp);
		int startMarginInPx = (int) ResourceUtils.convertDPtoPX(activity, startMarginInDp);
		return showTouchableToastPx(activity, touchableToast, parent, bottomMarginInPx, startMarginInPx);
	}

	public static boolean showTouchableToastPx(@Nullable Activity activity,
											   @Nullable PopupWindow touchableToast,
											   @Nullable View parent,
											   int bottomMarginInPx,
											   int startMarginInPx) {
		if (activity == null || touchableToast == null || parent == null) {
			return false;
		}
		if (activity.isFinishing()
				|| activity.isDestroyed()
		) {
			return false;
		}
		parent.post(() -> {
					if (activity.isFinishing()
							|| activity.isDestroyed()
					) {
						return;
					}
					touchableToast.showAtLocation(
							parent,
							Gravity.START | Gravity.BOTTOM,
							startMarginInPx,
							bottomMarginInPx
					);
				}
		);
		return true;
	}

	@Nullable
	public static PopupWindow getNewTouchableToast(@Nullable Context context, @StringRes int textResId) {
		return getNewTouchableToast(context, android.R.drawable.toast_frame, textResId);
	}

	@Nullable
	public static PopupWindow getNewTouchableToast(@Nullable Context context, @DrawableRes int toastResId, @StringRes int textResId) {
		return getNewTouchableToast(context, toastResId, context.getText(textResId));
	}

	@Nullable
	public static PopupWindow getNewTouchableToast(@Nullable Context context, @DrawableRes int toastResId, @Nullable CharSequence text) {
		if (context == null) {
			return null;
		}
		try {
			TextView contentView = new TextView(context);
			contentView.setText(text);
			contentView.setTextColor(Color.WHITE);
			PopupWindow newTouchableToast = new PopupWindow(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
			newTouchableToast.setContentView(contentView);
			newTouchableToast.setTouchable(true);
			newTouchableToast.setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(), toastResId, context.getTheme()));
			return newTouchableToast;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while creating touchable toast!");
			return null;
		}
	}
}
