package org.mtransit.android.commons;

import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused"})
public final class Constants {

	public static boolean DEBUG = false;
	// public static boolean DEBUG = true; // DEBUG

	// public static boolean FORCE_STRICT_MODE_OFF = false;
	public static boolean FORCE_STRICT_MODE_OFF = true; // DEBUG

	public static boolean FORCE_NEARBY_POI_LIST_OFF = false;
	// public static boolean FORCE_NEARBY_POI_LIST_OFF = true; // DEBUG

	public static boolean FORCE_FRAGMENT_USE_ARGS = false;
	// public static boolean FORCE_FRAGMENT_USE_ARGS = true; // DEBUG

	public static final boolean LOG_MT_QUERY = false;
	// public static final boolean LOG_MT_QUERY = true; // DEBUG

	public static final boolean LOG_LIVE_DATA_MEDIATOR = false;
	// public static final boolean LOG_LIVE_DATA_MEDIATOR = true; // DEBUG

	public static final boolean LOG_VIEW_LIFECYCLE = false;

	public static final boolean LOG_DATA_PARSING = false;

	public static final boolean LOG_LIFECYCLE = false;
	// public static final boolean LOG_LIFECYCLE = true; // DEBUG

	public static final boolean LOG_PROVIDER_LIFECYCLE = false;
	// public static final boolean LOG_PROVIDER_LIFECYCLE = true; // DEBUG

	public static final boolean LOG_ADAPTER_LIFECYCLE = false;

	public static final boolean LOG_TASK_LIFECYCLE = false;
	// public static final boolean LOG_TASK_LIFECYCLE = true; // DEBUG

	public static final boolean LOG_LOCATION = false;

	public static final boolean LOG_TIME_GENERATION = false;

	public static final String MAIN_APP_PACKAGE_NAME = BuildConfig.DEBUG ?
			"org.mtransit.android.debug" :
			"org.mtransit.android";

	public static final long ADAPTER_NOTIFY_THRESHOLD_IN_MS = TimeUnit.MILLISECONDS.toMillis(250L);

	private Constants() {
	}
}
