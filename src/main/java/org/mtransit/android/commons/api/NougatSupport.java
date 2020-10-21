package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.N)
public class NougatSupport extends MarshmallowSupport {

	private static final String LOG_TAG = NougatSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public NougatSupport() {
		super();
	}
}
