package org.mtransit.android.commons;

import java.util.concurrent.TimeUnit;

public final class Constants {

	public static boolean DEBUG = false;

	public static final boolean LOG_VIEW_LIFECYCLE = false;

	public static final boolean LOG_DATA_PARSING = false;

	public static final boolean LOG_LIFECYCLE = false;

	public static final boolean LOG_PROVIDER_LIFECYCLE = false;

	public static final boolean LOG_ADAPTER_LIFECYCLE = false;

	public static final boolean LOG_TASK_LIFECYCLE = false;

	public static final boolean LOG_LOCATION = false;

	public static final boolean LOG_TIME_GENERATION = false;

	public static final String MAIN_APP_PACKAGE_NAME = "org.mtransit.android";

	public static final long ADAPTER_NOTIFY_THRESHOLD_IN_MS = TimeUnit.MILLISECONDS.toMillis(250);

	private Constants() {
	}
}
