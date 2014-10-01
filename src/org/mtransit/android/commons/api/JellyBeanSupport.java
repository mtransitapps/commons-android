package org.mtransit.android.commons.api;

import android.annotation.TargetApi;

@TargetApi(16)
public class JellyBeanSupport extends IceCreamSandwichSupportMR1 {

	private static final String TAG = JellyBeanSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public JellyBeanSupport() {
	}


}
