package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class LollipopMR1Support extends LollipopSupport {

	private static final String LOG_TAG = LollipopMR1Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public LollipopMR1Support() {
		super();
	}
}
