package org.mtransit.android.commons.api;

import android.annotation.TargetApi;

@TargetApi(18)
public class JellyBeanSupportMR2 extends JellyBeanSupportMR1 {

	private static final String TAG = JellyBeanSupportMR2.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public JellyBeanSupportMR2() {
	}

}
