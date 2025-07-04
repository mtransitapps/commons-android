package org.mtransit.android.commons;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <a href="https://developer.android.com/reference/java/text/SimpleDateFormat.html#date-and-time-patterns">Date and Time Patterns</a>:
 * - "z" General time zone (Pacific Standard Time; PST; GMT-08:00)
 * - "Z" RFC 822 time zone (-0800)
 * - "X" ISO 8601 time zone	(-08; -0800; -08:00) API Level 24+
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@SuppressLint("SimpleDateFormat")
public class ThreadSafeDateFormatter {

	public static final int DEFAULT = SimpleDateFormat.DEFAULT;

	public static final int FULL = SimpleDateFormat.FULL;

	public static final int LONG = SimpleDateFormat.LONG;

	public static final int MEDIUM = SimpleDateFormat.MEDIUM;

	public static final int SHORT = SimpleDateFormat.SHORT;

	@NonNull
	private final DateFormat dateFormatter;

	@Deprecated
	public ThreadSafeDateFormatter(@NonNull String pattern) {
		this(new SimpleDateFormat(pattern));
	}

	public ThreadSafeDateFormatter(@NonNull String pattern, @NonNull Locale locale) {
		this(new SimpleDateFormat(pattern, locale));
	}

	public ThreadSafeDateFormatter(@NonNull DateFormat dateFormatter) {
		this.dateFormatter = dateFormatter;
	}

	public void setTimeZone(@NonNull TimeZone timeZone) {
		this.dateFormatter.setTimeZone(timeZone);
	}

	@NonNull
	public synchronized String formatThreadSafe(@NonNull Date date) {
		return this.dateFormatter.format(date);
	}

	@NonNull
	public String formatThreadSafe(@NonNull Calendar calendar) {
		return formatThreadSafe(calendar.getTime());
	}

	@NonNull
	public String formatThreadSafe(long timestamp) {
		return formatThreadSafe(new Date(timestamp));
	}

	@Nullable
	public synchronized Date parseThreadSafe(@NonNull String string) throws ParseException {
		return this.dateFormatter.parse(string);
	}

	@Nullable
	public String toPattern() {
		if (this.dateFormatter instanceof SimpleDateFormat) {
			return ((SimpleDateFormat) this.dateFormatter).toPattern();
		}
		return null;
	}

	@NonNull
	public static ThreadSafeDateFormatter getDateInstance(int style) {
		return new ThreadSafeDateFormatter(SimpleDateFormat.getDateInstance(style));
	}
}
