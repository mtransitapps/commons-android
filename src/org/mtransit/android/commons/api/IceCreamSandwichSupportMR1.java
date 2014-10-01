package org.mtransit.android.commons.api;

import android.annotation.TargetApi;

@TargetApi(15)
public class IceCreamSandwichSupportMR1 extends IceCreamSandwichSupport {

	private static final String TAG = IceCreamSandwichSupportMR1.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public IceCreamSandwichSupportMR1() {
	}

}
