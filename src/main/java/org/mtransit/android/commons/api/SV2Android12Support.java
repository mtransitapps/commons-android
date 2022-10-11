package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.S_V2)
public class SV2Android12Support extends SAndroid12Support {

	private static final String LOG_TAG = SV2Android12Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess"})
	public SV2Android12Support() {
		super();
	}
}
