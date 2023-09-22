package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
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

	private static final int TOAST_MARGIN_IN_DP = 8;
	private static final int NAVIGATION_HEIGHT_IN_DP = 88;

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
	public static PopupWindow getNewTouchableToast(
			@Nullable Context context,
			@DrawableRes int toastResId,
			@Nullable CharSequence labelText,
			@Nullable CharSequence actionText
	) {
		if (context == null) {
			return null;
		}
		try {
			LinearLayout viewGroup = new LinearLayout(context);
			viewGroup.setOrientation(LinearLayout.HORIZONTAL);
			final int dp8 = (int) ResourceUtils.convertDPtoPX(context, 8);
			final int dp16 = (int) ResourceUtils.convertDPtoPX(context, 16);
			final int dp32 = (int) ResourceUtils.convertDPtoPX(context, 32);
			TextView labelTv = new TextView(context);
			labelTv.setText(labelText);
			labelTv.setTextColor(Color.WHITE);
			viewGroup.addView(labelTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
			if (actionText != null && actionText.length() > 0) {
				TextView actionTv = new TextView(context);
				actionTv.setText(actionText);
				actionTv.setTextColor(Color.WHITE);
				actionTv.setTypeface(actionTv.getTypeface(), Typeface.BOLD);
				LinearLayout.LayoutParams actionTvLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
				actionTvLP.setMarginStart(dp32);
				viewGroup.addView(actionTv, actionTvLP);
			}
			PopupWindow newTouchableToast = new PopupWindow(
					WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.WRAP_CONTENT
			);
			newTouchableToast.setContentView(viewGroup);
			newTouchableToast.setTouchable(true);
			newTouchableToast.setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(), toastResId, context.getTheme()));
			return newTouchableToast;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while creating touchable toast!");
			return null;
		}
	}
}
