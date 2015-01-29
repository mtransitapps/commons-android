package org.mtransit.android.commons;

import android.util.Log;

public class MTLog {

	public static final String MAIN_TAG = "MT";

	public static final int MAX_LOG_LENGTH = 234;

	public static boolean isLoggable(int level) {
		return Constants.DEBUG || Log.isLoggable(MAIN_TAG, level);
	}

	public static void v(Loggable loggable, String msg) {
		v(loggable.getLogTag(), msg);
	}

	public static void v(String tag, String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, msg), MAX_LOG_LENGTH));
		}
	}

	public static void v(Loggable loggable, String msg, Object... args) {
		v(loggable.getLogTag(), msg, args);
	}

	public static void v(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH));
		}
	}

	public static void d(Loggable loggable, String msg) {
		d(loggable.getLogTag(), msg);
	}

	public static void d(String tag, String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, msg), MAX_LOG_LENGTH));
		}
	}

	public static void d(Loggable loggable, String msg, Object... args) {
		d(loggable.getLogTag(), msg, args);
	}

	public static void d(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH));
		}
	}

	public static void d(Loggable loggable, Throwable t, String msg, Object... args) {
		d(loggable.getLogTag(), t, msg, args);
	}

	public static void d(String tag, Throwable t, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH), t);
		}
	}

	public static void i(Loggable loggable, String msg) {
		i(loggable.getLogTag(), msg);
	}

	public static void i(String tag, String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, msg), MAX_LOG_LENGTH));
		}
	}

	public static void i(Loggable loggable, String msg, Object... args) {
		i(loggable.getLogTag(), msg, args);
	}

	public static void i(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH));
		}
	}

	public static void w(Loggable loggable, String msg, Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	public static void w(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH));
		}
	}

	public static void w(Loggable loggable, Throwable t, String msg, Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	public static void w(String tag, Throwable t, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH), t);
		}
	}

	public static void e(Loggable loggable, String msg, Object... args) {
		e(loggable.getLogTag(), msg, args);
	}

	public static void e(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH));
		}
	}

	public static void e(Loggable loggable, Throwable t, String msg, Object... args) {
		e(loggable.getLogTag(), t, msg, args);
	}

	public static void e(String tag, Throwable t, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, StringUtils.ellipsize(String.format("%s>%s", tag, String.format(msg, args)), MAX_LOG_LENGTH), t);
		}
	}

	public interface Loggable {
		public abstract String getLogTag();
	}

}
