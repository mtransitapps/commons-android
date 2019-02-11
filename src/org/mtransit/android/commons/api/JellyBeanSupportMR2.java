package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellyBeanSupportMR2 extends JellyBeanSupportMR1 {

	private static final String TAG = JellyBeanSupportMR2.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public JellyBeanSupportMR2() {
	}
}
