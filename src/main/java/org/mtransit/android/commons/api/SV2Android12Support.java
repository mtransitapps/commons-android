package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
@RequiresApi(Build.VERSION_CODES.S_V2)
public class SV2Android12Support extends SAndroid12Support {

	private static final String LOG_TAG = SV2Android12Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}
}
