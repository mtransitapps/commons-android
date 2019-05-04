package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class JellyBeanSupportMR1 extends JellyBeanSupport {

	private static final String LOG_TAG = JellyBeanSupportMR1.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public JellyBeanSupportMR1() {
		super();
	}
}
