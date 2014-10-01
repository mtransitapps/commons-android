package org.mtransit.android.commons.task;

import java.util.concurrent.Callable;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

public abstract class MTCallable<V> implements Callable<V>, MTLog.Loggable {

	@Override
	public V call() throws Exception {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "call()");
		}
		return callMT();
	}

	public abstract V callMT() throws Exception;

}
