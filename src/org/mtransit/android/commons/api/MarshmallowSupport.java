package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;

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
