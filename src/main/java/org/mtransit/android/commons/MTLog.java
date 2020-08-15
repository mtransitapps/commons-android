package org.mtransit.android.commons;

import android.util.Log;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Date;

// adb logcat -s "MT"
// adb logcat -s "MTD"
public final class MTLog {

	private static final String MAIN_TAG = BuildConfig.DEBUG ? "MTD" : "MT";

	private static final int MAX_LOG_LENGTH = Constants.DEBUG ? 12345 : 1234;

	public static boolean isLoggable(int level) {
		return Constants.DEBUG || Log.isLoggable(MAIN_TAG, level);
	}

	@NonNull
	public static String formatDateTime(long timeInMs) {
		return "[" + timeInMs + " - " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date(timeInMs)) + "]";
	}

	@NonNull
	public static String formatDateTime(@NonNull Date date) {
		return "[" + date.getTime() + " - " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(date) + "]";
	}

	public static void v(Loggable loggable, String msg) {
		v(loggable.getLogTag(), msg);
	}

	public static void v(String tag, String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, log(tag, msg));
		}
	}

	public static void v(Loggable loggable, String msg, Object... args) {
		v(loggable.getLogTag(), msg, args);
	}

	public static void v(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void d(Loggable loggable, String msg) {
		d(loggable.getLogTag(), msg);
	}

	public static void d(String tag, String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg));
		}
	}

	public static void d(Loggable loggable, String msg, Object... args) {
		d(loggable.getLogTag(), msg, args);
	}

	public static void d(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void d(Loggable loggable, Throwable t, String msg, Object... args) {
		d(loggable.getLogTag(), t, msg, args);
	}

	public static void d(String tag, Throwable t, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	public static void i(Loggable loggable, String msg) {
		i(loggable.getLogTag(), msg);
	}

	public static void i(String tag, String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, log(tag, msg));
		}
	}

	public static void i(Loggable loggable, String msg, Object... args) {
		i(loggable.getLogTag(), msg, args);
	}

	public static void i(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void w(Loggable loggable, String msg, Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	public static void w(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void w(Loggable loggable, Throwable t, String msg, Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	public static void w(String tag, Throwable t, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	public static void e(Loggable loggable, String msg, Object... args) {
		e(loggable.getLogTag(), msg, args);
	}

	public static void e(String tag, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void e(Loggable loggable, Throwable t, String msg, Object... args) {
		e(loggable.getLogTag(), t, msg, args);
	}

	public static void e(String tag, Throwable t, String msg, Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	private static String log(String tag, String msg) {
		return StringUtils.ellipsize(getLogMsg(tag, msg), MAX_LOG_LENGTH);
	}

	private static String log(String tag, String msg, Object... args) {
		return StringUtils.ellipsize(getLogMsg(tag, String.format(msg, args)), MAX_LOG_LENGTH);
	}

	private static String getLogMsg(String tag, String logMsg) {
		if (Constants.DEBUG) {
			logMsg = StringUtils.oneLineOneSpace(logMsg);
		}
		return String.format("%s:%s>%s", System.currentTimeMillis(), tag, logMsg);
	}

	public interface Loggable {
		@NonNull
		String getLogTag();
	}
}
