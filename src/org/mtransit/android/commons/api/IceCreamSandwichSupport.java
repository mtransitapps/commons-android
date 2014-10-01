package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class IceCreamSandwichSupport implements SupportUtil {

	private static final String TAG = IceCreamSandwichSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public IceCreamSandwichSupport() {
	}


}
