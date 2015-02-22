package org.mtransit.android.commons.task;

import java.util.Arrays;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.os.AsyncTask;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements MTLog.Loggable {

	public MTAsyncTask() {
		super();
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	protected void onPreExecute() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPreExecute()");
		}
		super.onPreExecute();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Result doInBackground(Params... params) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "doInBackground(%s)", Arrays.asList(params));
		}
		return doInBackgroundMT(params);
	}

	@SuppressWarnings("unchecked")
	protected abstract Result doInBackgroundMT(Params... params);

	@SuppressWarnings("unchecked")
	@Override
	protected void onProgressUpdate(Progress... values) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onProgressUpdate(%s)", Arrays.asList(values));
		}
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(Result result) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPostExecute(%s)", result);
		}
		super.onPostExecute(result);
	}

	@Override
	protected void onCancelled() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCancelled()");
		}
		super.onCancelled();
	}

	@Override
	protected void onCancelled(Result result) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCancelled(%s)", result);
		}
		super.onCancelled(result);
	}
}
