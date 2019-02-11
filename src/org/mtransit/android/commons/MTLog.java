package org.mtransit.android.commons;

import android.support.annotation.NonNull;
import android.util.Log;

public final class MTLog {

	public static final String MAIN_TAG = "MT";

	public static final int MAX_LOG_LENGTH = 234;

	public static boolean isLoggable(int level) {
		return Constants.DEBUG || Log.isLoggable(MAIN_TAG, level);
	}

	public static void v(@NonNull Loggable loggable, @NonNull String msg) {
		v(loggable.getLogTag(), msg);
	}

	public static void v(String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, log(tag, msg));
		}
	}

	public static void v(@NonNull Loggable loggable, @NonNull String msg, Object... args) {
		v(loggable.getLogTag(), msg, args);
	}

	public static void v(String tag, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void d(@NonNull Loggable loggable, @NonNull String msg) {
		d(loggable.getLogTag(), msg);
	}

	public static void d(String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg));
		}
	}

	public static void d(@NonNull Loggable loggable, @NonNull String msg, Object... args) {
		d(loggable.getLogTag(), msg, args);
	}

	public static void d(String tag, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void d(@NonNull Loggable loggable, Throwable t, @NonNull String msg, Object... args) {
		d(loggable.getLogTag(), t, msg, args);
	}

	public static void d(String tag, Throwable t, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	public static void i(@NonNull Loggable loggable, @NonNull String msg) {
		i(loggable.getLogTag(), msg);
	}

	public static void i(String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, log(tag, msg));
		}
	}

	public static void i(@NonNull Loggable loggable, @NonNull String msg, Object... args) {
		i(loggable.getLogTag(), msg, args);
	}

	public static void i(String tag, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void w(@NonNull Loggable loggable, @NonNull String msg, Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	public static void w(String tag, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void w(@NonNull Loggable loggable, Throwable t, @NonNull String msg, Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	public static void w(String tag, Throwable t, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	public static void e(@NonNull Loggable loggable, @NonNull String msg, Object... args) {
		e(loggable.getLogTag(), msg, args);
	}

	public static void e(String tag, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void e(@NonNull Loggable loggable, Throwable t, @NonNull String msg, Object... args) {
		e(loggable.getLogTag(), t, msg, args);
	}

	public static void e(String tag, Throwable t, @NonNull String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	private static String log(String tag, @NonNull String msg) {
		return StringUtils.ellipsize(getLogMsg(tag, msg), MAX_LOG_LENGTH);
	}

	private static String log(String tag, @NonNull String msg, Object... args) {
		return log(tag, String.format(msg, args));
		// return StringUtils.ellipsize(getLogMsg(tag, String.format(msg, args)), MAX_LOG_LENGTH);
	}

	private static String getLogMsg(String tag, String logMsg) {
		return String.format("%s:%s>%s", System.currentTimeMillis(), tag, logMsg);
	}

	public interface Loggable {
		@NonNull
		String getLogTag();
	}
}
