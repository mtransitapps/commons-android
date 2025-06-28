package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
public class BaklavaAndroid16Support extends VanillaIceCreamAndroid15Support {

	private static final String LOG_TAG = BaklavaAndroid16Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}
}
