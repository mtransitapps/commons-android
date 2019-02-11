package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class LollipopMR1Support extends LollipopSupport {

	private static final String TAG = LollipopMR1Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public LollipopMR1Support() {
	}
}
