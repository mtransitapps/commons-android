package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
@RequiresApi(Build.VERSION_CODES.S)
public class SAndroid12Support extends RAndroid11Support {

	private static final String LOG_TAG = SAndroid12Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess"})
	public SAndroid12Support() {
		super();
	}
}
