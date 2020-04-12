package org.mtransit.android.commons;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class TimeUtils implements MTLog.Loggable {

	private static final String LOG_TAG = TimeUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	protected static final String FORMAT_HOUR_12_PATTERN = "h a_";
	private static final String FORMAT_TIME_12_PATTERN = "h:mm a_";
	protected static final String FORMAT_TIME_12_W_TZ_PATTERN = "h:mm a_z";
	private static final String FORMAT_TIME_12_PRECISE_PATTERN = "h:mm:ss a_";
	protected static final String FORMAT_TIME_12_PRECISE_W_TZ_PATTERN = "h:mm:ss a_z";

	protected static final String FORMAT_HOUR_24_PATTERN = "HH_";
	private static final String FORMAT_TIME_24_PATTERN = "HH:mm_";
	protected static final String FORMAT_TIME_24_W_TZ_PATTERN = "HH:mm_z";
	private static final String FORMAT_TIME_24_PRECISE_PATTERN = "HH:mm:ss_";
	protected static final String FORMAT_TIME_24_PRECISE_W_TZ_PATTERN = "HH:mm:ss_z";

	@Nullable
	private static ThreadSafeDateFormatter formatTime;

	@NonNull
	private static ThreadSafeDateFormatter getFormatTime(@NonNull Context context) {
		if (formatTime == null) {
			formatTime = getNewFormatTime(context);
		}
		return formatTime;
	}

	@NonNull
	protected static ThreadSafeDateFormatter getNewFormatTime(@NonNull Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_PATTERN, Locale.getDefault());
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_PATTERN, Locale.getDefault());
		}
	}

	@Nullable
	private static ThreadSafeDateFormatter formatTimePrecise;

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimePrecise(@NonNull Context context) {
		if (formatTimePrecise == null) {
			formatTimePrecise = getNewFormatTimePrecise(context);
		}
		return formatTimePrecise;
	}

	@NonNull
	protected static ThreadSafeDateFormatter getNewFormatTimePrecise(@NonNull Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_PRECISE_PATTERN, Locale.getDefault());
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_PRECISE_PATTERN, Locale.getDefault());
		}
	}

	static int millisToSec(long millis) {
		return (int) (millis / 1000L);
	}

	public static int currentTimeSec() {
		return millisToSec(currentTimeMillis());
	}

	public static long currentTimeToTheMinuteMillis() {
		long currentTime = currentTimeMillis();
		return timeToTheMinuteMillis(currentTime);
	}

	public static long timeToTheMinuteMillis(long time) {
		time -= time % DateUtils.MINUTE_IN_MILLIS;
		return time;
	}

	private static final long TO_THE_TENS_SECONDS = TimeUnit.SECONDS.toMillis(10L);

	public static long timeToTheTensSecondsMillis(long time) {
		time -= time % TO_THE_TENS_SECONDS;
		return time;
	}

	public static long currentTimeMillis() { // USEFUL FOR DEBUG
		return System.currentTimeMillis();
	}

	protected static boolean isMorePreciseThanMinute(long timeInMs) {
		return timeInMs % DateUtils.MINUTE_IN_MILLIS > 0L;
	}

	@NonNull
	protected static ThreadSafeDateFormatter getFormatTime(@NonNull Context context, long timeInMs) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePrecise(context);
		}
		return getFormatTime(context);
	}

	@NonNull
	public static String formatTime(@NonNull Context context, @NonNull Date date) {
		return getFormatTime(context, date.getTime()).formatThreadSafe(date);
	}

	@NonNull
	public static Calendar getNewCalendar(long timestamp) {
		return getNewCalendar(TimeZone.getDefault(), timestamp);
	}

	@NonNull
	public static Calendar getNewCalendar(@NonNull TimeZone timeZone, long timestamp) {
		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.setTimeInMillis(timestamp);
		return calendar;
	}

	private static final String M = "m";

	@SuppressWarnings("unused")
	@Nullable
	public static ThreadSafeDateFormatter removeMinutes(@Nullable ThreadSafeDateFormatter input) {
		String pattern = input == null ? null : input.toPattern();
		if (pattern == null) {
			return null;
		}
		if (pattern.contains(M)) {
			pattern = pattern.replace(M, StringUtils.EMPTY);
		}
		return new ThreadSafeDateFormatter(pattern, Locale.getDefault());
	}

	protected static boolean is24HourFormat(@NonNull Context context) {
		return android.text.format.DateFormat.is24HourFormat(context);
	}
}
