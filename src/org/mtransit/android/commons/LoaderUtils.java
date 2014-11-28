package org.mtransit.android.commons;

import org.mtransit.android.commons.task.MTAsyncTask;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;

public final class LoaderUtils implements MTLog.Loggable {

	private static final String TAG = LoaderUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void restartLoader(final LoaderManager loaderManager, final int loaderId, final Bundle args,
			final LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		new MTAsyncTask<Void, Void, Void>() {
			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				loaderManager.restartLoader(loaderId, args, loaderCallbacks);
			}

		}.execute();
	}

}
