package org.mtransit.android.commons;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

// adb logcat -s "MT"
// adb logcat -s "MTD"
@SuppressWarnings({"unused", "WeakerAccess"})
public final class MTLog {

	private static final String MAIN_TAG = BuildConfig.DEBUG ? "MTD" : "MT";

	private static final int MAX_LOG_CAT_LENGTH = 4000; // depends on device // adb logcat -g

	private static final int MAX_LOG_LENGTH = Constants.DEBUG ? MAX_LOG_CAT_LENGTH : 1234;

	public static boolean isLoggable(int level) {
		return Constants.DEBUG || Log.isLoggable(MAIN_TAG, level);
	}

	@Nullable
	public static String formatDuration(@Nullable Long durationInMs) {
		return durationInMs == null ? null : formatDuration(durationInMs.longValue());
	}

	@NonNull
	public static String formatDuration(long durationInMs) {
		return "[" + durationInMs + " - " + TimeUtils.formatSimpleDuration(durationInMs) + "]";
	}

	@Nullable
	public static String formatDateTime(@Nullable Calendar calendar) {
		return calendar == null ? null : formatDateTime(calendar.getTimeInMillis());
	}

	@Nullable
	public static String formatDateTime(@Nullable Long timeInMs) {
		return timeInMs == null ? null : formatDateTime(timeInMs.longValue());
	}

	@NonNull
	public static String formatDateTime(long timeInMs) {
		return "[" + timeInMs + " - " + TimeUtils.formatSimpleDateTime(new Date(timeInMs)) + "]";
	}

	@Nullable
	public static String formatDateTimeN(@Nullable Date date) {
		return date == null ? null : formatDateTime(date);
	}

	@NonNull
	public static String formatDateTime(@NonNull Date date) {
		return "[" + date.getTime() + " - " + TimeUtils.formatSimpleDateTime(date) + "]";
	}

	// VERBOSE

	public static void v(@NonNull Loggable loggable, @NonNull String msg) {
		v(loggable.getLogTag(), msg);
	}

	public static void v(@NonNull Object object, @NonNull String msg) {
		v(object.getClass(), msg);
	}

	public static void v(@NonNull Class<?> clazz, @NonNull String msg) {
		v(clazz.getSimpleName(), msg);
	}

	public static void v(@NonNull String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, log(tag, msg));
		}
	}

	public static void v(@NonNull Object object, @NonNull String msg, @NonNull Object... args) {
		v(object.getClass(), msg, args);
	}

	public static void v(@NonNull Class<?> clazz, @NonNull String msg, @NonNull Object... args) {
		v(clazz.getSimpleName(), msg, args);
	}

	public static void v(@NonNull Loggable loggable, @NonNull String msg, @NonNull Object... args) {
		v(loggable.getLogTag(), msg, args);
	}

	public static void v(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.VERBOSE)) {
			Log.v(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void v(@NonNull Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		v(loggable.getLogTag(), t, msg, args);
	}

	public static void dv(@NonNull Object object, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		v(object.getClass(), t, msg, args);
	}

	public static void v(@NonNull Class<?> clazz, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		v(clazz.getSimpleName(), t, msg, args);
	}

	public static void v(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	// DEBUG

	public static void d(@NonNull Loggable loggable, @NonNull String msg) {
		d(loggable.getLogTag(), msg);
	}

	public static void d(@NonNull Object object, @NonNull String msg) {
		d(object.getClass(), msg);
	}

	public static void d(@NonNull Class<?> clazz, @NonNull String msg) {
		d(clazz.getSimpleName(), msg);
	}

	public static void d(@NonNull String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg));
		}
	}

	public static void d(@NonNull Loggable loggable, @NonNull String msg, @NonNull Object... args) {
		d(loggable.getLogTag(), msg, args);
	}

	public static void d(@NonNull Object object, @NonNull String msg, @NonNull Object... args) {
		d(object.getClass(), msg, args);
	}

	public static void d(@NonNull Class<?> clazz, @NonNull String msg, @NonNull Object... args) {
		d(clazz.getSimpleName(), msg, args);
	}

	public static void d(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void d(@NonNull Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		d(loggable.getLogTag(), t, msg, args);
	}

	public static void d(@NonNull Object object, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		d(object.getClass(), t, msg, args);
	}

	public static void d(@NonNull Class<?> clazz, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		d(clazz.getSimpleName(), t, msg, args);
	}

	public static void d(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	// INFO

	public static void i(@NonNull Loggable loggable, @NonNull String msg) {
		i(loggable.getLogTag(), msg);
	}

	public static void i(@NonNull Object object, @NonNull String msg) {
		i(object.getClass(), msg);
	}

	public static void i(@NonNull Class<?> clazz, @NonNull String msg) {
		i(clazz.getSimpleName(), msg);
	}

	public static void i(@NonNull String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, log(tag, msg));
		}
	}

	public static void i(@NonNull Loggable loggable, @NonNull String msg, @NonNull Object... args) {
		i(loggable.getLogTag(), msg, args);
	}

	public static void i(@NonNull Object object, @NonNull String msg, @NonNull Object... args) {
		i(object.getClass(), msg, args);
	}

	public static void i(@NonNull Class<?> clazz, @NonNull String msg, @NonNull Object... args) {
		i(clazz.getSimpleName(), msg, args);
	}

	public static void i(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.INFO)) {
			Log.i(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void i(@NonNull Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		i(loggable.getLogTag(), t, msg, args);
	}

	public static void i(@NonNull Object object, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		i(object.getClass(), t, msg, args);
	}

	public static void i(@NonNull Class<?> clazz, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		i(clazz.getSimpleName(), t, msg, args);
	}

	public static void i(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
			Log.d(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	// WARNING

	public static void w(@NonNull Loggable loggable, @NonNull String msg, @NonNull Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	public static void w(@NonNull Object object, @NonNull String msg) {
		w(object.getClass(), msg);
	}

	public static void w(@NonNull Class<?> clazz, @NonNull String msg) {
		w(clazz.getSimpleName(), msg);
	}

	public static void w(@NonNull String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg));
		}
	}

	public static void w(@NonNull Object object, @NonNull String msg, @NonNull Object... args) {
		w(object.getClass(), msg, args);
	}

	public static void w(@NonNull Class<?> clazz, @NonNull String msg, @NonNull Object... args) {
		i(clazz.getSimpleName(), msg, args);
	}

	public static void w(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void w(@NonNull Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	public static void w(@NonNull Object object, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		w(object.getClass(), t, msg, args);
	}

	public static void w(@NonNull Class<?> clazz, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		w(clazz.getSimpleName(), t, msg, args);
	}

	public static void w(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.WARN)) {
			Log.w(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	// ERROR

	public static void w(@NonNull Loggable loggable, @NonNull String msg) {
		e(loggable.getLogTag(), msg);
	}

	public static void e(@NonNull Object object, @NonNull String msg) {
		e(object.getClass(), msg);
	}

	public static void e(@NonNull Class<?> clazz, @NonNull String msg) {
		e(clazz.getSimpleName(), msg);
	}

	public static void e(@NonNull String tag, @NonNull String msg) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg));
		}
	}

	public static void e(@NonNull Loggable loggable, @NonNull String msg, @NonNull Object... args) {
		e(loggable.getLogTag(), msg, args);
	}

	public static void e(@NonNull Object object, @NonNull String msg, @NonNull Object... args) {
		e(object.getClass(), msg, args);
	}

	public static void e(@NonNull Class<?> clazz, @NonNull String msg, @NonNull Object... args) {
		e(clazz.getSimpleName(), msg, args);
	}

	public static void e(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg, args));
		}
	}

	public static void e(@NonNull Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		e(loggable.getLogTag(), t, msg, args);
	}

	public static void e(@NonNull Object object, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		e(object.getClass(), t, msg, args);
	}

	public static void e(@NonNull Class<?> clazz, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		w(clazz.getSimpleName(), t, msg, args);
	}

	public static void e(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		if (Constants.DEBUG || Log.isLoggable(MAIN_TAG, Log.ERROR)) {
			Log.e(MAIN_TAG, log(tag, msg, args), t);
		}
	}

	private static String log(@NonNull String tag, @NonNull String msg) {
		return StringUtils.ellipsize(getLogMsg(tag, msg), MAX_LOG_LENGTH);
	}

	private static String log(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		return StringUtils.ellipsize(getLogMsg(tag, String.format(msg, args)), MAX_LOG_LENGTH);
	}

	private static String getLogMsg(@NonNull String tag, @NonNull String logMsg) {
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