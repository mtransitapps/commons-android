package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.M)
public class MarshmallowSupport extends LollipopMR1Support {

	private static final String LOG_TAG = MarshmallowSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public MarshmallowSupport() {
		super();
	}
}
