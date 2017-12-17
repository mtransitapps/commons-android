package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class OreoSupportMR1 extends OreoSupport {

	private static final String TAG = OreoSupportMR1.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public OreoSupportMR1() {
	}
}
