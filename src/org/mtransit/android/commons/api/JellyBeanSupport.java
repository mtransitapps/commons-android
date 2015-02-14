package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class JellyBeanSupport extends IceCreamSandwichSupportMR1 {

	private static final String TAG = JellyBeanSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public JellyBeanSupport() {
	}

	@Override
	public void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener);
	}

	@Override
	public void setBackground(View view, Drawable background) {
		view.setBackground(background);
	}
}
