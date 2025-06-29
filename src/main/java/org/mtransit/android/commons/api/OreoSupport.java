package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.O)
public class OreoSupport extends NougatSupportMR1 {

	private static final String LOG_TAG = OreoSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}
}
