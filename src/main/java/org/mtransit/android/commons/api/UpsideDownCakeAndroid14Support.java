package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class UpsideDownCakeAndroid14Support extends TiramisuAndroid13Support {

	private static final String LOG_TAG = UpsideDownCakeAndroid14Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess"})
	public UpsideDownCakeAndroid14Support() {
		super();
	}
}
