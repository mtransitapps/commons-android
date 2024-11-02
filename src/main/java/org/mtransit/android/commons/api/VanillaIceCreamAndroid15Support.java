package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class VanillaIceCreamAndroid15Support extends UpsideDownCakeAndroid14Support {

	private static final String LOG_TAG = VanillaIceCreamAndroid15Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings({"WeakerAccess"})
	public VanillaIceCreamAndroid15Support() {
		super();
	}
}
