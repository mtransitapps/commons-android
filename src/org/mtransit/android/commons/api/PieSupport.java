package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.P)
public class PieSupport extends OreoSupportMR1 {

	private static final String TAG = PieSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public PieSupport() {
	}
}
