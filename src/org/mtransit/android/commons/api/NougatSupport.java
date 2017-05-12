package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.N)
public class NougatSupport extends MarshmallowSupport {

	private static final String TAG = NougatSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public NougatSupport() {
	}
}
