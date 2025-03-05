package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class TiramisuAndroid13Support extends SV2Android12Support {

	private static final String LOG_TAG = TiramisuAndroid13Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess"})
	public TiramisuAndroid13Support() {
		super();
	}
}
