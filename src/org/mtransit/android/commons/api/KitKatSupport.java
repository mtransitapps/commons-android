package org.mtransit.android.commons.api;

import android.annotation.TargetApi;

@TargetApi(19)
public class KitKatSupport extends JellyBeanSupportMR2 {

	private static final String TAG = KitKatSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public KitKatSupport() {
	}

}
