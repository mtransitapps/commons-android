package org.mtransit.android.commons;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.task.MTAsyncTask;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;

public final class LoaderUtils implements MTLog.Loggable {

	private static final String TAG = LoaderUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void restartLoader(LoaderManager loaderManager, int loaderId, Bundle args, LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		new RestartLoaderTask(loaderManager, loaderId, args, loaderCallbacks).execute();
	}

	private static class RestartLoaderTask extends MTAsyncTask<Void, Void, Void> {
		@Override
		public String getLogTag() {
			return TAG;
		}

		private int loaderId;
		private Bundle args;
		private WeakReference<LoaderManager> loaderManagerWR;
		private WeakReference<LoaderManager.LoaderCallbacks<?>> loaderCallbackWR;

		public RestartLoaderTask(LoaderManager loaderManager, int loaderId, Bundle args, LoaderManager.LoaderCallbacks<?> loaderCallback) {
			this.loaderId = loaderId;
			this.args = args;
			this.loaderManagerWR = new WeakReference<LoaderManager>(loaderManager);
			this.loaderCallbackWR = new WeakReference<LoaderManager.LoaderCallbacks<?>>(loaderCallback);
		}

		@Override
		protected Void doInBackgroundMT(Void... params) {
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				LoaderManager loaderManager = this.loaderManagerWR == null ? null : this.loaderManagerWR.get();
				if (loaderManager == null) {
					return;
				}
				LoaderManager.LoaderCallbacks<?> loaderCallbacks = this.loaderCallbackWR == null ? null : this.loaderCallbackWR.get();
				if (loaderCallbacks == null) {
					return;
				}
				loaderManager.restartLoader(loaderId, args, loaderCallbacks);
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while restarting loader ID '%s' for '%s'", this.loaderId,
						this.loaderCallbackWR == null ? null : this.loaderCallbackWR.get());
			}
		}
	}

}
