package org.mtransit.android.commons;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;

public final class LoaderUtils implements MTLog.Loggable {

	private static final String TAG = LoaderUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void restartLoader(LoaderManager loaderManager, int loaderId, Bundle args, LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			if (loaderManager == null) {
				return;
			}
			if (loaderCallbacks == null) {
				return;
			}
			loaderManager.restartLoader(loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while restarting loader ID '%s' for '%s'", loaderId, loaderCallbacks);
		}
	}

}
