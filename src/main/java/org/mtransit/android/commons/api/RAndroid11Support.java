package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.R)
public class RAndroid11Support extends QAndroid10Support {

	private static final String LOG_TAG = RAndroid11Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
	public RAndroid11Support() {
		super();
	}
}
