package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ViewTreeObserver;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class IceCreamSandwichSupport implements SupportUtil {

	private static final String TAG = IceCreamSandwichSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public IceCreamSandwichSupport() {
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeGlobalOnLayoutListener(onGlobalLayoutListener);
	}

}
