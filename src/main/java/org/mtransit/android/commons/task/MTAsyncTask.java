package org.mtransit.android.commons.task;

import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.util.Arrays;

/**
 * NO LOGIC HERE, just logs.
 */
@Deprecated
public abstract class MTAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements MTLog.Loggable {

	public MTAsyncTask() {
		super();
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	protected void onPreExecute() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onPreExecute()");
		}
		super.onPreExecute();
	}

	@SuppressWarnings("unchecked")
	@WorkerThread
	@Nullable
	@Override
	protected Result doInBackground(@Nullable Params... params) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "doInBackground(%s)", (params == null ? null : Arrays.asList(params)));
		}
		return doInBackgroundMT(params);
	}

	@WorkerThread
	@SuppressWarnings("unchecked")
	@Nullable
	protected abstract Result doInBackgroundMT(@Nullable Params... params);

	@SuppressWarnings("unchecked")
	@Override
	protected void onProgressUpdate(@Nullable Progress... values) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onProgressUpdate(%s)", (values == null ? null : Arrays.asList(values)));
		}
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(@Nullable Result result) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onPostExecute(%s)", result);
		}
		super.onPostExecute(result);
	}

	@Override
	protected void onCancelled() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onCancelled()");
		}
		super.onCancelled();
	}

	@Override
	protected void onCancelled(@Nullable Result result) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onCancelled(%s)", result);
		}
		super.onCancelled(result);
	}
}
