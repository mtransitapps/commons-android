package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public final class KeyboardUtils implements MTLog.Loggable {

	private static final String TAG = KeyboardUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void showKeyboard(Activity activity, View view) {
		if (activity == null || view == null) {
			return;
		}
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	public static void hideKeyboard(Activity activity, View view) {
		if (activity == null || view == null) {
			return;
		}
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

}
