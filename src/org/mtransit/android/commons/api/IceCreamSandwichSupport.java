package org.mtransit.android.commons.api;

import android.annotation.TargetApi;

@TargetApi(14)
public class IceCreamSandwichSupport implements SupportUtil {

	private static final String TAG = IceCreamSandwichSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public IceCreamSandwichSupport() {
	}


}
