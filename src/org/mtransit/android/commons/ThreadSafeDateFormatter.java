package org.mtransit.android.commons;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.SuppressLint;

@SuppressLint("SimpleDateFormat")
public class ThreadSafeDateFormatter {

	public static final int DEFAULT = SimpleDateFormat.DEFAULT;

	public static final int FULL = SimpleDateFormat.FULL;

	public static final int LONG = SimpleDateFormat.LONG;

	public static final int MEDIUM = SimpleDateFormat.MEDIUM;

	public static final int SHORT = SimpleDateFormat.SHORT;

	private DateFormat dateFormatter;

	public ThreadSafeDateFormatter(String pattern) {
		this.dateFormatter = new SimpleDateFormat(pattern);
	}

	public ThreadSafeDateFormatter(String template, Locale locale) {
		this.dateFormatter = new SimpleDateFormat(template, locale);
	}

	public ThreadSafeDateFormatter(DateFormat dateFormatter) {
		this.dateFormatter = dateFormatter;
	}

	public void setTimeZone(TimeZone timeZone) {
		if (this.dateFormatter == null) {
			throw new IllegalStateException("No date formatter!");
		}
		this.dateFormatter.setTimeZone(timeZone);
	}

	public synchronized String formatThreadSafe(Date date) {
		if (this.dateFormatter == null) {
			return date == null ? null : date.toString();
		}
		return this.dateFormatter.format(date);
	}

	public String formatThreadSafe(long timestamp) {
		return formatThreadSafe(new Date(timestamp));
	}

	public synchronized Date parseThreadSafe(String string) throws ParseException {
		if (this.dateFormatter == null) {
			throw new ParseException("No date formatter!", -1);
		}
		return this.dateFormatter.parse(string);
	}

	public String toPattern() {
		if (this.dateFormatter instanceof SimpleDateFormat) {
			return ((SimpleDateFormat) this.dateFormatter).toPattern();
		}
		return null;
	}

	public static ThreadSafeDateFormatter getDateInstance(int style) {
		return new ThreadSafeDateFormatter(SimpleDateFormat.getDateInstance(style));
	}
}
