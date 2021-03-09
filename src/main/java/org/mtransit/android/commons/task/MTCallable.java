package org.mtransit.android.commons.task;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.util.concurrent.Callable;

public abstract class MTCallable<V> implements Callable<V>, MTLog.Loggable {

	@Override
	public V call() throws Exception {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "call()");
		}
		return callMT();
	}

	public abstract V callMT() throws Exception;

}
