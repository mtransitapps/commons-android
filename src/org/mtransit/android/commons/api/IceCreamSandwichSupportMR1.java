package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class IceCreamSandwichSupportMR1 extends IceCreamSandwichSupport {

	private static final String TAG = IceCreamSandwichSupportMR1.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public IceCreamSandwichSupportMR1() {
	}
}
