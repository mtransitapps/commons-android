package org.mtransit.android.commons.api;

import android.app.Activity;
import android.os.Build;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
@RequiresApi(Build.VERSION_CODES.R)
public class RAndroid11Support extends QAndroid10Support {

	private static final String LOG_TAG = RAndroid11Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess"})
	public RAndroid11Support() {
		super();
	}

	@Nullable
	@Override
	public Display getDefaultDisplay(@NonNull Activity activity) {
		return activity.getDisplay();
	}
}
