package org.mtransit.android.commons;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public final class ThreadUtils implements MTLog.Loggable {

	private static final String TAG = ThreadUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * @deprecated not in production!
	 */
	@Deprecated
	public static void sleepInSec(long sleepDurationInSec) {
		sleepInMs(TimeUnit.SECONDS.toMillis(sleepDurationInSec));
	}

	/**
	 * @deprecated not in production!
	 */
	@Deprecated
	public static void sleepInMs(long sleepDurationInMs) {
		try {
			Thread.sleep(sleepDurationInMs);
		} catch (InterruptedException e) {
			MTLog.d(TAG, e, "Error while sleeping!");
		}
	}

}
