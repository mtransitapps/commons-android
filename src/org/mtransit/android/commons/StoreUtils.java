package org.mtransit.android.commons;

import android.app.Activity;
import android.net.Uri;

public final class StoreUtils implements MTLog.Loggable {

	private static final String TAG = StoreUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String GOOGLE_PLAY_BASE_URI_AND_PKG = "market://details?id=%s";

	public static void viewAppPage(Activity activity, String pkg, String label) {
		Uri uri = Uri.parse(String.format(GOOGLE_PLAY_BASE_URI_AND_PKG, pkg));
		LinkUtils.open(activity, uri, label);
	}
}
