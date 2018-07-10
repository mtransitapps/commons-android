package org.mtransit.android.commons;

import java.util.concurrent.Executor;

import org.mtransit.android.commons.task.MTAsyncTask;

import android.support.annotation.Nullable;

public final class TaskUtils implements MTLog.Loggable {

	private static final String TAG = TaskUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final Executor THREAD_POOL_EXECUTOR = MTAsyncTask.THREAD_POOL_EXECUTOR;

	@SuppressWarnings("unchecked")
	public static <Params, Progress, Result> void execute(@Nullable MTAsyncTask<Params, Progress, Result> asyncTask, Params... params) {
		if (asyncTask == null) {
			return;
		}
		asyncTask.executeOnExecutor(THREAD_POOL_EXECUTOR, params);
	}

	public static <Params, Progress, Result> boolean cancelQuietly(@Nullable MTAsyncTask<Params, Progress, Result> asyncTask, boolean mayInterruptIfRunning) {
		try {
			if (asyncTask == null) {
				return false;
			}
			return asyncTask.cancel(mayInterruptIfRunning);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while cancelling task!");
			return false;
		}
	}
}
