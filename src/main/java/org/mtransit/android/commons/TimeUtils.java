package org.mtransit.android.commons;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.data.Schedule.Timestamp;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "unused"})
public class TimeUtils implements MTLog.Loggable {

	private static final String LOG_TAG = TimeUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final char REAL_TIME_CHAR = '_';
	public static final String REAL_TIME_STRING = "_";

	protected static final String FORMAT_HOUR_12_PATTERN = "h a" + REAL_TIME_STRING;
	protected static final String FORMAT_HOUR_12_FIXED_LENGTH_PATTERN = "hh a" + REAL_TIME_STRING;
	private static final String FORMAT_TIME_12_PATTERN = "h:mm a" + REAL_TIME_STRING;
	protected static final String FORMAT_TIME_12_W_TZ_PATTERN = "h:mm a" + REAL_TIME_STRING + "z";
	private static final String FORMAT_TIME_12_PRECISE_PATTERN = "h:mm:ss a" + REAL_TIME_STRING;
	protected static final String FORMAT_TIME_12_PRECISE_W_TZ_PATTERN = "h:mm:ss a" + REAL_TIME_STRING + "z";

	protected static final String FORMAT_HOUR_24_PATTERN = "HH" + REAL_TIME_STRING;
	private static final String FORMAT_TIME_24_PATTERN = "HH:mm" + REAL_TIME_STRING;
	protected static final String FORMAT_TIME_24_W_TZ_PATTERN = "HH:mm" + REAL_TIME_STRING + "z";
	private static final String FORMAT_TIME_24_PRECISE_PATTERN = "HH:mm:ss" + REAL_TIME_STRING;
	protected static final String FORMAT_TIME_24_PRECISE_W_TZ_PATTERN = "HH:mm:ss" + REAL_TIME_STRING + "z";

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
	public static ThreadSafeDateFormatter getNewFormatTime(@NonNull Context context) {
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
	public static ThreadSafeDateFormatter getNewFormatTimePrecise(@NonNull Context context) {
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
	public static String formatTime(boolean realTime, @NonNull Context context, @NonNull Date date) {
		return cleanNoRealTime(realTime,
				formatTime(context, date)
		);
	}

	@NonNull
	public static String formatTime(@NonNull Context context, @NonNull Date date) {
		return getFormatTime(context, date.getTime()).formatThreadSafe(date);
	}

	@NonNull
	public static String formatTime(@NonNull Context context, @NonNull Timestamp t) {
		return cleanNoRealTime(t.isRealTime(),
				formatTime(context, t.t)
		);
	}

	@NonNull
	public static String formatTime(boolean realTime, @NonNull Context context, long timeInMs) {
		return cleanNoRealTime(realTime,
				formatTime(context, timeInMs)
		);
	}

	@NonNull
	public static String formatTime(@NonNull Context context, long timeInMs) {
		return getFormatTime(context, timeInMs).formatThreadSafe(timeInMs);
	}

	@NonNull
	public static String cleanNoRealTime(boolean realTime, @NonNull String fTime) {
		if (!realTime) {
			fTime = fTime.replace(REAL_TIME_STRING, StringUtils.EMPTY);
		}
		return fTime;
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

	public static boolean is24HourFormat(@NonNull Context context) {
		return android.text.format.DateFormat.is24HourFormat(context);
	}

	@NonNull
	public static String formatSimpleDateTime(@NonNull Date date) {
		return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(date);
	}

	@NonNull
	public static String formatSimpleDuration(long durationInMs) {
		StringBuilder sb = new StringBuilder();
		if (durationInMs < 0) {
			durationInMs = Math.abs(durationInMs);
		}
		sb.append("-");
		final long days = durationInMs / TimeUnit.DAYS.toMillis(1L);
		if (days > 0) {
			sb.append(" ").append(days).append(" days");
			durationInMs = durationInMs % days;
		}
		final long hours = durationInMs / TimeUnit.HOURS.toMillis(1L);
		if (hours > 0) {
			sb.append(" ").append(hours).append(" h");
			durationInMs = durationInMs % hours;
		}
		final long minutes = durationInMs / TimeUnit.MINUTES.toMillis(1L);
		if (minutes > 0) {
			sb.append(" ").append(minutes).append(" min");
			durationInMs = durationInMs % minutes;
		}
		final long seconds = durationInMs / TimeUnit.MINUTES.toMillis(1L);
		if (seconds > 0) {
			sb.append(" ").append(seconds).append(" sec");
			durationInMs = durationInMs % seconds;
		}
		if (durationInMs > 0) {
			sb.append(" ").append(durationInMs).append(" ms");
		}
		return sb.toString();
	}
}
