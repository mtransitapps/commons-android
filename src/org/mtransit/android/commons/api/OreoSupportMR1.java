package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class OreoSupportMR1 extends OreoSupport {

	private static final String LOG_TAG = OreoSupportMR1.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public OreoSupportMR1() {
		super();
	}
}
