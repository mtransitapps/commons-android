package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.M)
public class MarshmallowSupport extends LollipopMR1Support {

	private static final String TAG = MarshmallowSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public MarshmallowSupport() {
	}

	@ColorInt
	@Override
	public int getColor(Resources resources, int id, Resources.Theme theme) {
		return resources.getColor(id, theme);
	}
}
