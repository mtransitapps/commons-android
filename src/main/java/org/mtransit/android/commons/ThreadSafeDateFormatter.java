package org.mtransit.android.commons;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
		this.dateFormatter = new SimpleDateFormat(pattern);
	}

	public ThreadSafeDateFormatter(@NonNull String template, @NonNull Locale locale) {
		this.dateFormatter = new SimpleDateFormat(template, locale);
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
