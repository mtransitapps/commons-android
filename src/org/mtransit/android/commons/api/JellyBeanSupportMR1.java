package org.mtransit.android.commons.api;

import android.annotation.TargetApi;

@TargetApi(17)
public class JellyBeanSupportMR1 extends JellyBeanSupport {

	private static final String TAG = JellyBeanSupportMR1.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public JellyBeanSupportMR1() {
	}

}
