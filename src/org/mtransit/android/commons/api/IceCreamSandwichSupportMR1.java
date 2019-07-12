package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class IceCreamSandwichSupportMR1 extends IceCreamSandwichSupport {

	private static final String LOG_TAG = IceCreamSandwichSupportMR1.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public IceCreamSandwichSupportMR1() {
		super();
	}
}
