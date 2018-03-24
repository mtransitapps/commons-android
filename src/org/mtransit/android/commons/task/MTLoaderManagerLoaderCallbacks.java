package org.mtransit.android.commons.task;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

public abstract class MTLoaderManagerLoaderCallbacks<D> implements LoaderManager.LoaderCallbacks<D>, MTLog.Loggable {

	@NonNull
	@Override
	public Loader<D> onCreateLoader(int id, Bundle args) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onCreateLoader(%s,%s)", id, args);
		}
		return onCreateLoaderMT(id, args);
	}

	/**
	 * @see LoaderManager.LoaderCallbacks#onCreateLoader(int, Bundle)
	 */
	public abstract Loader<D> onCreateLoaderMT(int id, Bundle args);

	@Override
	public void onLoaderReset(@NonNull Loader<D> loader) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onLoaderReset(%s)", loader);
		}
		onLoaderResetMT(loader);
	}

	/**
	 * @see LoaderManager.LoaderCallbacks#onLoaderReset(Loader)
	 */
	public abstract void onLoaderResetMT(Loader<D> loader);

	@Override
	public void onLoadFinished(@NonNull Loader<D> loader, D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onLoadFinished(%s,%s)", loader, data);
		}
		onLoadFinishedMT(loader, data);
	}

	/**
	 * @see LoaderManager.LoaderCallbacks#onLoadFinished(Loader, Object)
	 */
	public abstract void onLoadFinishedMT(Loader<D> loader, D data);
}
