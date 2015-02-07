package org.mtransit.android.commons;

public final class ThreadUtils implements MTLog.Loggable {

	private static final String TAG = ThreadUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * @deprecated not in production!
	 */
	@Deprecated
	public static void sleepInSec(int sleepDurationInSec) {
		sleepInMs(sleepDurationInSec * 1000l);
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
