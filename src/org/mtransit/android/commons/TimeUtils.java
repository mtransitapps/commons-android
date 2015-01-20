package org.mtransit.android.commons;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;

public class TimeUtils implements MTLog.Loggable {

	private static final String TAG = TimeUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}


	public static final long RECENT_IN_MILLIS = TimeUnit.HOURS.toMillis(1);

	public static IntentFilter TIME_CHANGED_INTENT_FILTER;
	static {
		TIME_CHANGED_INTENT_FILTER = new IntentFilter();
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
	}

	private static final String FORMAT_HOUR_12_PATTERN = "h a";
	private static final String FORMAT_TIME_12_PATTERN = "h:mm a";
	private static final String FORMAT_TIME_12_PRECISE_PATTERN = "h:mm:ss a";

	private static final String FORMAT_HOUR_24_PATTERN = "HH";
	private static final String FORMAT_TIME_24_PATTERN = "HH:mm";
	private static final String FORMAT_TIME_24_PRECISE_PATTERN = "HH:mm:ss";

	private static ThreadSafeDateFormatter formatTime;

	private static ThreadSafeDateFormatter getFormatTime(Context context) {
		if (formatTime == null) {
			formatTime = getNewFormatTime(context);
		}
		return formatTime;
	}

	private static ThreadSafeDateFormatter getNewFormatTime(Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_PATTERN);
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_PATTERN);
		}
	}

	private static ThreadSafeDateFormatter formatTimePrecise;

	private static ThreadSafeDateFormatter getFormatTimePrecise(Context context) {
		if (formatTimePrecise == null) {
			formatTimePrecise = getNewFormatTimePrecise(context);
		}
		return formatTimePrecise;
	}

	private static ThreadSafeDateFormatter getNewFormatTimePrecise(Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_PRECISE_PATTERN);
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_PRECISE_PATTERN);
		}
	}

	public static ThreadSafeDateFormatter getNewHourFormat(Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_HOUR_24_PATTERN);
		} else {
			return new ThreadSafeDateFormatter(FORMAT_HOUR_12_PATTERN);
		}
	}

	public static int millisToSec(long millis) {
		return (int) (millis / 1000l);
	}

	public static int currentTimeSec() {
		return millisToSec(currentTimeMillis());
	}


	public static long currentTimeToTheMinuteMillis() {
		long currentTime = currentTimeMillis();
		return timeToTheMinuteMillis(currentTime);
	}

	public static long timeToTheMinuteMillis(long time) {
		time -= time % (60 * 1000);
		return time;
	}

	public static long currentTimeMillis() { // USEFUL FOR DEBUG
		return System.currentTimeMillis();
	}

	public static long getBeginningOfTodayInMs() {
		return getBeginningOfTodayCal().getTimeInMillis();
	}

	public static Calendar getBeginningOfTodayCal() {
		Calendar today = getNewCalendarInstance(currentTimeMillis());
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		return today;
	}

	public static boolean isMorePreciseThanMinute(long timeInMs) {
		return timeInMs % TimeUnit.MINUTES.toMillis(1) > 0;
	}

	public static String formatTime(Context context, long timeInMs) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePrecise(context).formatThreadSafe(timeInMs);
		}
		return getFormatTime(context).formatThreadSafe(timeInMs);
	}

	public static String formatTime(Context context, long timeInMs, String timeZone) {
		ThreadSafeDateFormatter df;
		if (isMorePreciseThanMinute(timeInMs)) {
			df = getNewFormatTimePrecise(context);
		}
		df = getNewFormatTime(context);
		df.setTimeZone(TimeZone.getTimeZone(timeZone));
		return df.formatThreadSafe(timeInMs);
	}

	public static String formatTime(Context context, Date date) {
		if (isMorePreciseThanMinute(date.getTime())) {
			return getFormatTimePrecise(context).formatThreadSafe(date);
		}
		return getFormatTime(context).formatThreadSafe(date);
	}

	public static boolean isToday(long timeInMs) {
		return timeInMs >= getBeginningOfTodayCal().getTimeInMillis() && timeInMs < getBeginningOfTomorrowCal().getTimeInMillis();
	}

	public static boolean isTomorrow(long timeInMs) {
		return timeInMs >= getBeginningOfTomorrowCal().getTimeInMillis() && timeInMs < getBeginningOfDayRelativeToTodayCal(+2).getTimeInMillis();
	}

	public static boolean isYesterday(long timeInMs) {
		return timeInMs >= getBeginningOfYesterdayCal().getTimeInMillis() && timeInMs < getBeginningOfTodayCal().getTimeInMillis();
	}

	public static Calendar getBeginningOfDayRelativeToTodayCal(int nbDays) {
		Calendar today = getBeginningOfTodayCal();
		today.add(Calendar.DATE, nbDays);
		return today;
	}

	public static int getHourOfTheDay(long timeInMs) {
		Calendar time = getNewCalendar(timeInMs);
		return time.get(Calendar.HOUR_OF_DAY);
	}

	public static Calendar getBeginningOfYesterdayCal() {
		return getBeginningOfDayRelativeToTodayCal(-1);
	}

	public static Calendar getBeginningOfTomorrowCal() {
		return getBeginningOfDayRelativeToTodayCal(+1);
	}

	public static Calendar getNewCalendarInstance(long timeInMs) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timeInMs);
		return cal;
	}

	public static Calendar getNewCalendar(long timestamp) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		return calendar;
	}

	public static ThreadSafeDateFormatter removeMinutes(ThreadSafeDateFormatter input) {
		String pattern = input.toPattern();
		if (pattern.contains("m")) {
			pattern = pattern.replace("m", "");
		}
		return new ThreadSafeDateFormatter(pattern);
	}

	public static boolean is24HourFormat(Context context) {
		return android.text.format.DateFormat.is24HourFormat(context);
	}

	public static final long FREQUENT_SERVICE_TIMESPAN_IN_MS_DEFAULT = TimeUnit.MINUTES.toMillis(5);
	public static final long FREQUENT_SERVICE_MIN_DURATION_IN_MS_DEFAULT = TimeUnit.MINUTES.toMillis(30);
	public static final long FREQUENT_SERVICE_MIN_SERVICE = 2;

	public static boolean isFrequentService(ArrayList<Schedule.Timestamp> timestamps, long providerFSMinDuractionInMs, long providerFSTimespanInMs) {
		if (CollectionUtils.getSize(timestamps) < FREQUENT_SERVICE_MIN_SERVICE) {
			return false; // NOT FREQUENT (no service at all)
		}
		long fsMinDuractionMs = providerFSMinDuractionInMs > 0 ? providerFSMinDuractionInMs : FREQUENT_SERVICE_MIN_DURATION_IN_MS_DEFAULT;
		long fsTimespanMs = providerFSTimespanInMs > 0 ? providerFSTimespanInMs : FREQUENT_SERVICE_TIMESPAN_IN_MS_DEFAULT;
		long firstTimestamp = timestamps.get(0).t;
		long previousTimestamp = firstTimestamp;
		for (int i = 1; i < timestamps.size() /* && i < maxTests */; i++) {
			long currentTimestamp = timestamps.get(i).t;
			long diffInMs = currentTimestamp - previousTimestamp;
			if (diffInMs > fsTimespanMs) {
				return false; // NOT FREQUENT
			}
			previousTimestamp = currentTimestamp;
			if (previousTimestamp - firstTimestamp >= fsMinDuractionMs) {
				return true; // NOT FREQUENT (for long enough)
			}
		}
		if (previousTimestamp - firstTimestamp < fsMinDuractionMs) {
			return false; // NOT FREQUENT (for long enough)
		}
		return true; // FREQUENT
	}

	private static final ThreadSafeDateFormatter STANDALONE_DAY_OF_THE_WEEK_LONG = new ThreadSafeDateFormatter("cccc");

	private static final ThreadSafeDateFormatter STANDALONE_MONTH_LONG = new ThreadSafeDateFormatter("LLLL");


	public static final long MAX_DURATION_DISPLAYED_IN_MS = TimeUnit.HOURS.toMillis(6);

	public static final int URGENT_SCHEDULE_IN_MIN = 10;
	public static final long URGENT_SCHEDULE_IN_MS = TimeUnit.MINUTES.toMillis(URGENT_SCHEDULE_IN_MIN);
	public static final long MAX_DURATION_SHOW_NUMBER_IN_MS = TimeUnit.MINUTES.toMillis(100) - 1; // 99 minutes 59 seconds 999 milliseconds

	public static Pair<CharSequence, CharSequence> getShortTimeSpan(Context context, long diffInMs, long targetedTimestamp, long precisionInMs) {
		if (diffInMs > MAX_DURATION_DISPLAYED_IN_MS) {
			Pair<CharSequence, CharSequence> timeS = getShortTimeSpanString(context, diffInMs, targetedTimestamp);
			return getShortTimeSpanStringStyle(context, timeS);
		} else {
			return getShortTimeSpanNumber(context, diffInMs, precisionInMs);
		}
	}

	private static final int MILLIS_IN_SEC = 1000;
	private static final int SEC_IN_MIN = 60;
	private static final int MIN_IN_HOUR = 60;
	private static final int HOUR_IN_DAY = 24;

	private static Pair<CharSequence, CharSequence> getShortTimeSpanNumber(Context context, long diffInMs, long precisionInMs) {
		int diffInSec = (int) Math.floor(diffInMs / MILLIS_IN_SEC);
		if (diffInMs - (diffInSec * MILLIS_IN_SEC) > (MILLIS_IN_SEC / 2)) {
			diffInSec++;
		}
		int diffInMin = (int) Math.floor(diffInSec / SEC_IN_MIN);
		if (diffInSec - (diffInMin * SEC_IN_MIN) > (SEC_IN_MIN / 2)) {
			diffInMin++;
		}
		int diffInHour = (int) Math.floor(diffInMin / MIN_IN_HOUR);
		if (diffInMin - (diffInHour * MIN_IN_HOUR) > (MIN_IN_HOUR / 2)) {
			diffInHour++;
		}
		int diffInDay = (int) Math.floor(diffInHour / HOUR_IN_DAY);
		if (diffInHour - (diffInDay * HOUR_IN_DAY) > (HOUR_IN_DAY / 2)) {
			diffInDay++;
		}
		int startTimeUnitLine1 = -1;
		int endTimeUnitLine1 = -1;
		int startUrgentTimeLine1 = -1;
		int endUrgentTimeLine1 = -1;
		int startTimeUnitLine2 = -1;
		int endTimeUnitLine2 = -1;
		int startUrgentTimeLine2 = -1;
		int endUrgentTimeLine2 = -1;
		SpannableStringBuilder shortTimeSpanLine1SSB = new SpannableStringBuilder();
		SpannableStringBuilder shortTimeSpanLine2SSB = new SpannableStringBuilder();
		boolean isShortTimeSpanString = false;
		if (diffInDay > 0 && diffInHour > 99) {
			shortTimeSpanLine1SSB.append(getNumberInLetter(context, diffInDay));
			isShortTimeSpanString = true;
			shortTimeSpanLine2SSB.append(context.getResources().getQuantityText(R.plurals.days_capitalized, diffInDay));
		} else if (diffInHour > 0 && diffInMin > 99) {
			shortTimeSpanLine1SSB.append(getNumberInLetter(context, diffInHour));
			isShortTimeSpanString = true;
			shortTimeSpanLine2SSB.append(context.getResources().getQuantityText(R.plurals.hours_capitalized, diffInHour));
		} else if (diffInMs <= precisionInMs && diffInMs >= -precisionInMs) {
			startUrgentTimeLine1 = shortTimeSpanLine1SSB.length();
			shortTimeSpanLine1SSB.append(String.valueOf(diffInMin));
			endUrgentTimeLine1 = shortTimeSpanLine1SSB.length();
			startUrgentTimeLine2 = shortTimeSpanLine2SSB.length();
			startTimeUnitLine2 = shortTimeSpanLine2SSB.length();
			shortTimeSpanLine2SSB.append(context.getResources().getQuantityString(R.plurals.minutes_capitalized, Math.abs(diffInMin)));
			endTimeUnitLine2 = shortTimeSpanLine2SSB.length();
			endUrgentTimeLine2 = shortTimeSpanLine2SSB.length();
		} else {
			if (diffInMin < URGENT_SCHEDULE_IN_MIN) {
				startUrgentTimeLine1 = shortTimeSpanLine1SSB.length();
			}
			shortTimeSpanLine1SSB.append(String.valueOf(diffInMin));
			if (diffInMin < URGENT_SCHEDULE_IN_MIN) {
				endUrgentTimeLine1 = shortTimeSpanLine1SSB.length();
				startUrgentTimeLine2 = shortTimeSpanLine2SSB.length();
			}
			startTimeUnitLine2 = shortTimeSpanLine2SSB.length();
			shortTimeSpanLine2SSB.append(context.getResources().getQuantityString(R.plurals.minutes_capitalized, diffInMin));
			endTimeUnitLine2 = shortTimeSpanLine2SSB.length();
			if (diffInMin < URGENT_SCHEDULE_IN_MIN) {
				endUrgentTimeLine2 = shortTimeSpanLine2SSB.length();
			}
		}
		if (startUrgentTimeLine1 < endUrgentTimeLine1) {
			SpanUtils.set(shortTimeSpanLine1SSB, SpanUtils.getLargeTextAppearance(context), startUrgentTimeLine1, endUrgentTimeLine1);
		}
		if (startUrgentTimeLine2 < endUrgentTimeLine2) {
			SpanUtils.set(shortTimeSpanLine2SSB, SpanUtils.getLargeTextAppearance(context), startUrgentTimeLine2, endUrgentTimeLine2);
		}
		if (startTimeUnitLine1 < endTimeUnitLine1) {
			SpanUtils.set(shortTimeSpanLine1SSB, SpanUtils.FIFTY_PERCENT_SIZE_SPAN, startTimeUnitLine1, endTimeUnitLine1);
			SpanUtils.set(shortTimeSpanLine1SSB, POIStatus.STATUS_TEXT_FONT, startTimeUnitLine1, endTimeUnitLine1);
		}
		if (startTimeUnitLine2 < endTimeUnitLine2) {
			SpanUtils.set(shortTimeSpanLine2SSB, SpanUtils.FIFTY_PERCENT_SIZE_SPAN, startTimeUnitLine2, endTimeUnitLine2);
			SpanUtils.set(shortTimeSpanLine2SSB, POIStatus.STATUS_TEXT_FONT, startTimeUnitLine2, endTimeUnitLine2);
		}

		if (isShortTimeSpanString) {
			return new Pair<CharSequence, CharSequence>( //
					getShortTimeSpanStringStyle(context, shortTimeSpanLine1SSB), //
					getShortTimeSpanStringStyle(context, shortTimeSpanLine2SSB));
		}
		return new Pair<CharSequence, CharSequence>(shortTimeSpanLine1SSB, shortTimeSpanLine2SSB);
	}

	public static CharSequence getNumberInLetter(Context context, int number) {
		switch (number) {
		case 0:
			return context.getString(R.string.zero_capitalized);
		case 1:
			return context.getString(R.string.one_capitalized);
		case 2:
			return context.getString(R.string.two_capitalized);
		case 3:
			return context.getString(R.string.three_capitalized);
		case 4:
			return context.getString(R.string.four_capitalized);
		case 5:
			return context.getString(R.string.five_capitalized);
		case 6:
			return context.getString(R.string.six_capitalized);
		case 7:
			return context.getString(R.string.seven_capitalized);
		case 8:
			return context.getString(R.string.eight_capitalized);
		case 9:
			return context.getString(R.string.nine_capitalized);
		case 10:
			return context.getString(R.string.ten_capitalized);
		case 11:
			return context.getString(R.string.eleven_capitalized);
		case 12:
			return context.getString(R.string.twelve_capitalized);
		case 13:
			return context.getString(R.string.thirteen_capitalized);
		case 14:
			return context.getString(R.string.fourteen_capitalized);
		case 15:
			return context.getString(R.string.fifteen_capitalized);
		case 16:
			return context.getString(R.string.sixteen_capitalized);
		case 17:
			return context.getString(R.string.seventeen_capitalized);
		case 18:
			return context.getString(R.string.eighteen_capitalized);
		case 19:
			return context.getString(R.string.nineteen_capitalized);
		default:
			return String.valueOf(number); // 2 characters number almost equal world
		}
	}

	private static Pair<CharSequence, CharSequence> getShortTimeSpanString(Context context, long diffInMs, long targetedTimestamp) {
		long now = targetedTimestamp - diffInMs;
		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(now);
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		//
		Calendar todayMorningStarts = (Calendar) today.clone();
		todayMorningStarts.set(Calendar.HOUR_OF_DAY, 6);
		Calendar todayAfterNoonStarts = (Calendar) today.clone();
		todayAfterNoonStarts.set(Calendar.HOUR_OF_DAY, 12);
		// showing text instead of [too-far-to-be-useful] duration
		if (targetedTimestamp >= todayMorningStarts.getTimeInMillis() && targetedTimestamp < todayAfterNoonStarts.getTimeInMillis()) {
			// MORNING
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.this_morning_part_1), context.getString(R.string.this_morning_part_2));
		}
		Calendar todayEveningStarts = (Calendar) today.clone();
		todayEveningStarts.set(Calendar.HOUR_OF_DAY, 18);
		if (targetedTimestamp >= todayAfterNoonStarts.getTimeInMillis() && targetedTimestamp < todayEveningStarts.getTimeInMillis()) {
			// AFTERNOON
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.this_afternoon_part_1), context.getString(R.string.this_afternoon_part_2));
		}
		Calendar tonightStarts = (Calendar) today.clone();
		tonightStarts.set(Calendar.HOUR_OF_DAY, 22);
		if (targetedTimestamp >= todayEveningStarts.getTimeInMillis() && targetedTimestamp < tonightStarts.getTimeInMillis()) {
			// EVENING
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.this_evening_part_1), context.getString(R.string.this_evening_part_2));
		}
		Calendar tomorrow = (Calendar) today.clone();
		tomorrow.add(Calendar.DATE, +1);
		Calendar tomorrowStarts = (Calendar) tomorrow.clone();
		tomorrowStarts.set(Calendar.HOUR_OF_DAY, 5);
		if (targetedTimestamp >= tonightStarts.getTimeInMillis() && targetedTimestamp < tomorrowStarts.getTimeInMillis()) {
			// NIGHT
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.tonight_part_1), context.getString(R.string.tonight_part_2));
		}
		Calendar afterTomorrow = (Calendar) today.clone();
		afterTomorrow.add(Calendar.DATE, +2);
		if (targetedTimestamp >= tomorrowStarts.getTimeInMillis() && targetedTimestamp < afterTomorrow.getTimeInMillis()) {
			// TOMORROW
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.tomorrow_part_1), context.getString(R.string.tomorrow_part_2));
		}
		Calendar nextWeekStarts = (Calendar) today.clone();
		nextWeekStarts.add(Calendar.DATE, +7);
		if (targetedTimestamp >= afterTomorrow.getTimeInMillis() && targetedTimestamp < nextWeekStarts.getTimeInMillis()) {
			// THIS WEEK (Monday-Sunday)
			String weekDay = STANDALONE_DAY_OF_THE_WEEK_LONG.formatThreadSafe(targetedTimestamp);
			return new Pair<CharSequence, CharSequence>(weekDay, null);
		}
		Calendar nextWeekEnds = (Calendar) today.clone();
		nextWeekEnds.add(Calendar.DATE, +14);
		if (targetedTimestamp >= nextWeekStarts.getTimeInMillis() && targetedTimestamp < nextWeekEnds.getTimeInMillis()) {
			// NEXT WEEK
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.next_week_part_1), context.getString(R.string.next_week_part_2));
		}
		Calendar thisMonthStarts = (Calendar) today.clone();
		thisMonthStarts.set(Calendar.DAY_OF_MONTH, 1);
		Calendar nextMonthStarts = (Calendar) thisMonthStarts.clone();
		nextMonthStarts.add(Calendar.MONTH, +1);
		if (targetedTimestamp >= thisMonthStarts.getTimeInMillis() && targetedTimestamp < nextMonthStarts.getTimeInMillis()) {
			// THIS MONTH
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.this_month_part_1), context.getString(R.string.this_month_part_2));
		}
		Calendar nextNextMonthStarts = (Calendar) nextMonthStarts.clone();
		nextNextMonthStarts.add(Calendar.MONTH, +1);
		if (targetedTimestamp >= nextMonthStarts.getTimeInMillis() && targetedTimestamp < nextNextMonthStarts.getTimeInMillis()) {
			// NEXT MONTH
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.next_month_part_1), context.getString(R.string.next_month_part_2));
		}
		Calendar next12MonthsStart = (Calendar) today.clone();
		next12MonthsStart.add(Calendar.MONTH, +1);
		Calendar next12MonthsEnd = (Calendar) today.clone();
		next12MonthsEnd.add(Calendar.MONTH, +6);
		if (targetedTimestamp >= next12MonthsStart.getTimeInMillis() && targetedTimestamp < next12MonthsEnd.getTimeInMillis()) {
			// LESS THAN 12 MONTHS (January-December)
			String monthOfTheYear = STANDALONE_MONTH_LONG.formatThreadSafe(targetedTimestamp);
			return new Pair<CharSequence, CharSequence>(monthOfTheYear, null);
		}
		Calendar thisYearStarts = (Calendar) thisMonthStarts.clone();
		thisYearStarts.set(Calendar.MONTH, Calendar.JANUARY);
		Calendar nextYearStarts = (Calendar) thisYearStarts.clone();
		nextYearStarts.add(Calendar.YEAR, +1);
		Calendar nextNextYearStarts = (Calendar) nextYearStarts.clone();
		nextNextYearStarts.add(Calendar.YEAR, +1);
		if (targetedTimestamp >= nextYearStarts.getTimeInMillis() && targetedTimestamp < nextNextYearStarts.getTimeInMillis()) {
			// NEXT YEAR
			return new Pair<CharSequence, CharSequence>(context.getString(R.string.next_year_part_1), context.getString(R.string.next_year_part_2));
		}
		// DEFAULT
		CharSequence defaultDate = DateUtils.formatSameDayTime(targetedTimestamp, now, ThreadSafeDateFormatter.MEDIUM, ThreadSafeDateFormatter.SHORT);
		return new Pair<CharSequence, CharSequence>(defaultDate, null);
	}

	private static CharSequence getShortTimeSpanStringStyle(Context context, CharSequence timeSpan) {
		if (TextUtils.isEmpty(timeSpan)) {
			return timeSpan;
		}
		SpannableStringBuilder fsSSB = new SpannableStringBuilder(timeSpan);
		SpanUtils.set(fsSSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fsSSB, POIStatus.STATUS_TEXT_FONT);
		return fsSSB;
	}

	private static Pair<CharSequence, CharSequence> getShortTimeSpanStringStyle(Context context, Pair<CharSequence, CharSequence> timeSpans) {
		if (timeSpans == null) {
			return null;
		}
		return new Pair<CharSequence, CharSequence>(getShortTimeSpanStringStyle(context, timeSpans.first), getShortTimeSpanStringStyle(context,
				timeSpans.second));
	}
	public static boolean isSameDay(Long timeInMillis1, Long timeInMillis2) {
		if (timeInMillis1 == null || timeInMillis2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		Calendar cal1 = Calendar.getInstance();
		cal1.setTimeInMillis(timeInMillis1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(timeInMillis2);
		return isSameDay(cal1, cal2);
	}

	public static boolean isSameDay(Calendar cal1, Calendar cal2) {
		if (cal1 == null || cal2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) //
				&& cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) //
		&& cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}

	public static boolean isSameDay(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return isSameDay(cal1, cal2);
	}

	public static class TimeChangedReceiver extends BroadcastReceiver {

		private WeakReference<TimeChangedListener> listenerWR;

		public TimeChangedReceiver(TimeChangedListener listener) {
			this.listenerWR = new WeakReference<TimeChangedListener>(listener);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
				TimeChangedListener listener = this.listenerWR == null ? null : this.listenerWR.get();
				if (listener != null) {
					listener.onTimeChanged();
				}
			}
		}

		public static interface TimeChangedListener {
			public void onTimeChanged();
		}
	}
}
