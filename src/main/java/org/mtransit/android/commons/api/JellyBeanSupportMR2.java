package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellyBeanSupportMR2 extends JellyBeanSupportMR1 {

	private static final String LOG_TAG = JellyBeanSupportMR2.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public JellyBeanSupportMR2() {
		super();
	}
}
