package org.mtransit.android.commons;

public final class ThreadUtils {

	/**
	 * @deprecated not in production!
	 */
	@Deprecated
	public static final void sleepInSec(int sleepDurationInSec) {
		try {
			Thread.sleep(sleepDurationInSec * 1000l);
		} catch (InterruptedException e) {
		}
	}

}
