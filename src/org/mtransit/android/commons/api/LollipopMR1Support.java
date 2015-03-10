package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class LollipopMR1Support extends LollipopSupport {

	private static final String TAG = LollipopMR1Support.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public LollipopMR1Support() {
	}
}
