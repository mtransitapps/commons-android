package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.task.MTAsyncTask;

import java.util.concurrent.Executor;

public final class TaskUtils implements MTLog.Loggable {

	private static final String LOG_TAG = TaskUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
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
			MTLog.w(LOG_TAG, e, "Error while cancelling task!");
			return false;
		}
	}
}
