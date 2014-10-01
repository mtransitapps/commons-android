package org.mtransit.android.commons.task;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;

public abstract class MTLoaderManagerLoaderCallbacks<D> implements LoaderManager.LoaderCallbacks<D>, MTLog.Loggable {

	@Override
	public Loader<D> onCreateLoader(int id, Bundle args) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateLoader(%s,%s)", id, args);
		}
		return onCreateLoaderMT(id, args);
	}

	/**
	 * @see LoaderCallbacks#onCreateLoader(int, Bundle)
	 */
	public abstract Loader<D> onCreateLoaderMT(int id, Bundle args);

	@Override
	public void onLoaderReset(Loader<D> loader) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onLoaderReset(%s)", loader);
		}
		onLoaderResetMT(loader);
	}

	/**
	 * @see LoaderCallbacks#onLoaderReset(Loader)
	 */
	public abstract void onLoaderResetMT(Loader<D> loader);

	@Override
	public void onLoadFinished(Loader<D> loader, D data) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onLoadFinished(%s,%s)", loader, data);
		}
		onLoadFinishedMT(loader, data);
	}

	/**
	 * @see LoaderCallbacks#onLoadFinished(Loader, Object)
	 */
	public abstract void onLoadFinishedMT(Loader<D> loader, D data);

}
