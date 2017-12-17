package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.O)
public class OreoSupport extends NougatSupportMR1 {

	private static final String TAG = OreoSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public OreoSupport() {
	}
}
