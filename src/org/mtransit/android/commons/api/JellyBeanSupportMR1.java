package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class JellyBeanSupportMR1 extends JellyBeanSupport {

	private static final String TAG = JellyBeanSupportMR1.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public JellyBeanSupportMR1() {
	}
}
