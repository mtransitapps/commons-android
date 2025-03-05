package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.N_MR1)
public class NougatSupportMR1 extends NougatSupport {

	private static final String LOG_TAG = NougatSupportMR1.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public NougatSupportMR1() {
		super();
	}
}
