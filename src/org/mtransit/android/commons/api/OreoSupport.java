package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.O)
public class OreoSupport extends NougatSupportMR1 {

	private static final String LOG_TAG = OreoSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public OreoSupport() {
		super();
	}
}
