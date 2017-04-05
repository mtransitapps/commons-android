package org.mtransit.android.commons;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.util.Pair;

public class TimeUtils implements MTLog.Loggable {

	private static final String TAG = TimeUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final long RECENT_IN_MILLIS = DateUtils.HOUR_IN_MILLIS;

	public static IntentFilter TIME_CHANGED_INTENT_FILTER;
	static {
		TIME_CHANGED_INTENT_FILTER = new IntentFilter();
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
	}

	private static final String FORMAT_HOUR_12_PATTERN = "h a";
	private static final String FORMAT_TIME_12_PATTERN = "h:mm a";
	private static final String FORMAT_TIME_12_W_TZ_PATTERN = "h:mm a z";
	private static final String FORMAT_TIME_12_PRECISE_PATTERN = "h:mm:ss a";
	private static final String FORMAT_TIME_12_PRECISE_W_TZ_PATTERN = "h:mm:ss a z";

	private static final String FORMAT_HOUR_24_PATTERN = "HH";
	private static final String FORMAT_TIME_24_PATTERN = "HH:mm";
	private static final String FORMAT_TIME_24_W_TZ_PATTERN = "HH:mm z";
	private static final String FORMAT_TIME_24_PRECISE_PATTERN = "HH:mm:ss";
	private static final String FORMAT_TIME_24_PRECISE_W_TZ_PATTERN = "HH:mm:ss z";

	private static ThreadSafeDateFormatter formatTime;

	private static ThreadSafeDateFormatter getFormatTime(Context context) {
		if (formatTime == null) {
			formatTime = getNewFormatTime(context);
		}
		return formatTime;
	}

	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimeTZ = new WeakHashMap<String, ThreadSafeDateFormatter>();

	private static ThreadSafeDateFormatter getFormatTimeTZ(Context context, TimeZone timeZone) {
		if (!formatTimeTZ.containsKey(timeZone.getID())) {
			ThreadSafeDateFormatter formatTime = getNewFormatTime(context);
			formatTime.setTimeZone(timeZone);
			formatTimeTZ.put(timeZone.getID(), formatTime);
		}
		return formatTimeTZ.get(timeZone.getID());
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

	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimePreciseTZ = new WeakHashMap<String, ThreadSafeDateFormatter>();

	private static ThreadSafeDateFormatter getFormatTimePreciseTZ(Context context, TimeZone timeZone) {
		if (!formatTimePreciseTZ.containsKey(timeZone.getID())) {
			ThreadSafeDateFormatter formatTimePrecise = getNewFormatTimePrecise(context);
			formatTimePrecise.setTimeZone(timeZone);
			formatTimePreciseTZ.put(timeZone.getID(), formatTimePrecise);
		}
		return formatTimePreciseTZ.get(timeZone.getID());
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

	private static final long TO_THE_TENS_SECONDS = TimeUnit.SECONDS.toMillis(10);

	public static long timeToTheTensSecondsMillis(long time) {
		time -= time % TO_THE_TENS_SECONDS;
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
		return timeInMs % DateUtils.MINUTE_IN_MILLIS > 0;
	}

	public static CharSequence formatRelativeTime(Context context, long timeInThePastInMs) {
		return formatRelativeTime(context, timeInThePastInMs, currentTimeMillis());
	}

	public static CharSequence formatRelativeTime(Context context, long timeInThePastInMs, long nowInMs) {
		return DateUtils.getRelativeTimeSpanString(timeInThePastInMs, nowInMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
	}

	public static String formatTime(Context context, long timeInMs) {
		return getFormatTime(context, timeInMs).formatThreadSafe(timeInMs);
	}

	public static String formatTime(Context context, long timeInMs, String timeZone) {
		return formatTime(context, timeInMs, TimeZone.getTimeZone(timeZone));
	}

	public static String formatTime(Context context, long timeInMs, TimeZone timeZone) {
		return getFormatTimeTZ(context, timeInMs, timeZone).formatThreadSafe(timeInMs);
	}

	private static ThreadSafeDateFormatter getFormatTime(Context context, long timeInMs) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePrecise(context);
		}
		return getFormatTime(context);
	}

	private static ThreadSafeDateFormatter getFormatTimeTZ(Context context, long timeInMs, TimeZone timeZone) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePreciseTZ(context, timeZone);
		}
		return getFormatTimeTZ(context, timeZone);
	}

	public static String formatTimeWithTZ(Context context, long timeInMs, TimeZone timeZone) {
		return getFormatTimeWithTZ(context, timeInMs, timeZone).formatThreadSafe(timeInMs);
	}

	private static ThreadSafeDateFormatter getFormatTimeWithTZ(Context context, long timeInMs, TimeZone timeZone) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePreciseWithTZ(context, timeZone);
		}
		return getFormatTimeWithTZ(context, timeZone);
	}

	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimePreciseWithTZ = new WeakHashMap<String, ThreadSafeDateFormatter>();

	private static ThreadSafeDateFormatter getFormatTimePreciseWithTZ(Context context, TimeZone timeZone) {
		if (!formatTimePreciseWithTZ.containsKey(timeZone.getID())) {
			ThreadSafeDateFormatter formatTimePrecise = getNewFormatTimePreciseWithTZ(context);
			formatTimePrecise.setTimeZone(timeZone);
			formatTimePreciseWithTZ.put(timeZone.getID(), formatTimePrecise);
		}
		return formatTimePreciseWithTZ.get(timeZone.getID());
	}

	private static ThreadSafeDateFormatter getNewFormatTimePreciseWithTZ(Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_PRECISE_W_TZ_PATTERN);
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_PRECISE_W_TZ_PATTERN);
		}
	}

	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimeWithTZ = new WeakHashMap<String, ThreadSafeDateFormatter>();

	private static ThreadSafeDateFormatter getFormatTimeWithTZ(Context context, TimeZone timeZone) {
		if (!formatTimeWithTZ.containsKey(timeZone.getID())) {
			ThreadSafeDateFormatter formatTime = getNewFormatTimeWithTZ(context);
			formatTime.setTimeZone(timeZone);
			formatTimeWithTZ.put(timeZone.getID(), formatTime);
		}
		return formatTimeWithTZ.get(timeZone.getID());
	}

	private static ThreadSafeDateFormatter getNewFormatTimeWithTZ(Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_W_TZ_PATTERN);
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_W_TZ_PATTERN);
		}
	}

	public static String formatTime(Context context, Date date) {
		return getFormatTime(context, date.getTime()).formatThreadSafe(date);
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

	private static final String AM = "am";
	private static final String PM = "pm";

	private static final Pattern TIME_W_SECONDS = Pattern.compile("([0-9]{1,2}\\:[0-9]{2}:[0-9]{2})", Pattern.CASE_INSENSITIVE);

	public static void cleanTimes(SpannableStringBuilder ssb) {
		cleanTimes(ssb.toString(), ssb);
	}

	public static void cleanTimes(String input, SpannableStringBuilder output) {
		String word = input.toLowerCase(Locale.ENGLISH);
		for (int index = word.indexOf(AM); index >= 0; index = word.indexOf(AM, index + 1)) { // TODO i18n
			if (index <= 0) {
				break;
			}
			output = SpanUtils.set(output, index - 1, index, SpanUtils.getNew10PercentSizeSpan()); // remove space hack
			output = SpanUtils.set(output, index, index + 2, SpanUtils.getNew25PercentSizeSpan());
		}
		for (int index = word.indexOf(PM); index >= 0; index = word.indexOf(PM, index + 1)) { // TODO i18n
			if (index <= 0) {
				break;
			}
			output = SpanUtils.set(output, index - 1, index, SpanUtils.getNew10PercentSizeSpan()); // remove space hack
			output = SpanUtils.set(output, index, index + 2, SpanUtils.getNew25PercentSizeSpan());
		}
		Matcher rMatcher = TIME_W_SECONDS.matcher(word);
		while (rMatcher.find()) {
			int end = rMatcher.end();
			output = SpanUtils.set(output, end - 3, end, SpanUtils.getNew50PercentSizeSpan());
		}
	}

	private static final String M = "m";

	public static ThreadSafeDateFormatter removeMinutes(ThreadSafeDateFormatter input) {
		String pattern = input == null ? null : input.toPattern();
		if (pattern == null) {
			return null;
		}
		if (pattern.contains(M)) {
			pattern = pattern.replace(M, StringUtils.EMPTY);
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
		long currentTimestamp;
		long diffInMs;
		for (int i = 1; i < timestamps.size(); i++) {
			currentTimestamp = timestamps.get(i).t;
			diffInMs = currentTimestamp - previousTimestamp;
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

	private static final int MAX_MINUTES_SHOWED = 99;
	private static final int MAX_HOURS_SHOWED = 99;
	public static final long MAX_DURATION_SHOW_NUMBER_IN_MS = TimeUnit.MINUTES.toMillis(MAX_MINUTES_SHOWED);

	public static Pair<CharSequence, CharSequence> getShortTimeSpan(Context context, long diffInMs, long targetedTimestamp, long precisionInMs) {
		if (diffInMs < MAX_DURATION_DISPLAYED_IN_MS) {
			return getShortTimeSpanNumber(context, diffInMs, precisionInMs);
		} else {
			Pair<CharSequence, CharSequence> shortTimeSpanString = getShortTimeSpanString(context, diffInMs, targetedTimestamp);
			if (shortTimeSpanString == null) {
				return null;
			}
			return new Pair<CharSequence, CharSequence>( //
					getShortTimeSpanStringStyle(context, shortTimeSpanString.first), getShortTimeSpanStringStyle(context, shortTimeSpanString.second));
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
		int urgentTime1Start = -1;
		int urgentTime1End = -1;
		int timeUnit2Start = -1;
		int timeUnit2End = -1;
		int urgentTime2Start = -1;
		int urgentTime2End = -1;
		SpannableStringBuilder shortTimeSpan1SSB = new SpannableStringBuilder();
		SpannableStringBuilder shortTimeSpan2SSB = new SpannableStringBuilder();
		boolean isShortTimeSpanString = false;
		if (diffInDay > 0 && diffInHour > MAX_HOURS_SHOWED) {
			shortTimeSpan1SSB.append(getNumberInLetter(context, diffInDay));
			isShortTimeSpanString = true;
			shortTimeSpan2SSB.append(context.getResources().getQuantityText(R.plurals.days_capitalized, diffInDay));
		} else if (diffInHour > 0 && diffInMin > MAX_MINUTES_SHOWED) {
			shortTimeSpan1SSB.append(getNumberInLetter(context, diffInHour));
			isShortTimeSpanString = true;
			shortTimeSpan2SSB.append(context.getResources().getQuantityText(R.plurals.hours_capitalized, diffInHour));
		} else if (diffInMs <= precisionInMs && diffInMs >= -precisionInMs) {
			urgentTime1Start = shortTimeSpan1SSB.length();
			shortTimeSpan1SSB.append(String.valueOf(diffInMin));
			urgentTime1End = shortTimeSpan1SSB.length();
			urgentTime2Start = shortTimeSpan2SSB.length();
			timeUnit2Start = shortTimeSpan2SSB.length();
			shortTimeSpan2SSB.append(context.getResources().getQuantityString(R.plurals.minutes_capitalized, Math.abs(diffInMin)));
			timeUnit2End = shortTimeSpan2SSB.length();
			urgentTime2End = shortTimeSpan2SSB.length();
		} else {
			boolean isUrgent = diffInMin < URGENT_SCHEDULE_IN_MIN;
			if (isUrgent) {
				urgentTime1Start = shortTimeSpan1SSB.length();
			}
			shortTimeSpan1SSB.append(String.valueOf(diffInMin));
			if (isUrgent) {
				urgentTime1End = shortTimeSpan1SSB.length();
			}
			if (isUrgent) {
				urgentTime2Start = shortTimeSpan2SSB.length();
			}
			timeUnit2Start = shortTimeSpan2SSB.length();
			shortTimeSpan2SSB.append(context.getResources().getQuantityString(R.plurals.minutes_capitalized, diffInMin));
			timeUnit2End = shortTimeSpan2SSB.length();
			if (isUrgent) {
				urgentTime2End = shortTimeSpan2SSB.length();
			}
		}
		if (urgentTime1Start < urgentTime1End) {
			shortTimeSpan1SSB = SpanUtils.set(shortTimeSpan1SSB, urgentTime1Start, urgentTime1End, getUrgentTime1TextAppearance(context));
		}
		if (urgentTime2Start < urgentTime2End) {
			shortTimeSpan2SSB = SpanUtils.set(shortTimeSpan2SSB, urgentTime2Start, urgentTime2End, getUrgentTime2TextAppearance(context));
		}
		if (timeUnit2Start < timeUnit2End) {
			shortTimeSpan2SSB = SpanUtils.set(shortTimeSpan2SSB, timeUnit2Start, timeUnit2End, //
					TIME_UNIT_SIZE, TIME_UNIT_FONT);
		}
		if (isShortTimeSpanString) {
			return new Pair<CharSequence, CharSequence>( //
					getShortTimeSpanStringStyle(context, shortTimeSpan1SSB), getShortTimeSpanStringStyle(context, shortTimeSpan2SSB));
		}
		return new Pair<CharSequence, CharSequence>(shortTimeSpan1SSB, shortTimeSpan2SSB);
	}

	private static RelativeSizeSpan TIME_UNIT_SIZE = SpanUtils.getNew50PercentSizeSpan();

	private static TypefaceSpan TIME_UNIT_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static TextAppearanceSpan urgentTime1TextAppearance = null;

	private static TextAppearanceSpan getUrgentTime1TextAppearance(Context context) {
		if (urgentTime1TextAppearance == null) {
			urgentTime1TextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return urgentTime1TextAppearance;
	}

	private static TextAppearanceSpan urgentTime2TextAppearance = null;

	private static TextAppearanceSpan getUrgentTime2TextAppearance(Context context) {
		if (urgentTime2TextAppearance == null) {
			urgentTime2TextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return urgentTime2TextAppearance;
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
		Calendar todayMorningStarts = (Calendar) today.clone();
		todayMorningStarts.set(Calendar.HOUR_OF_DAY, 6);
		Calendar todayAfterNoonStarts = (Calendar) today.clone();
		todayAfterNoonStarts.set(Calendar.HOUR_OF_DAY, 12);
		if (targetedTimestamp >= todayMorningStarts.getTimeInMillis() && targetedTimestamp < todayAfterNoonStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.this_morning_part_1), context.getString(R.string.this_morning_part_2)); // MORNING
		}
		Calendar todayEveningStarts = (Calendar) today.clone();
		todayEveningStarts.set(Calendar.HOUR_OF_DAY, 18);
		if (targetedTimestamp >= todayAfterNoonStarts.getTimeInMillis() && targetedTimestamp < todayEveningStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.this_afternoon_part_1), context.getString(R.string.this_afternoon_part_2)); // AFTERNOON
		}
		Calendar tonightStarts = (Calendar) today.clone();
		tonightStarts.set(Calendar.HOUR_OF_DAY, 22);
		if (targetedTimestamp >= todayEveningStarts.getTimeInMillis() && targetedTimestamp < tonightStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.this_evening_part_1), context.getString(R.string.this_evening_part_2)); // EVENING
		}
		Calendar tomorrow = (Calendar) today.clone();
		tomorrow.add(Calendar.DATE, +1);
		Calendar tomorrowStarts = (Calendar) tomorrow.clone();
		tomorrowStarts.set(Calendar.HOUR_OF_DAY, 5);
		if (targetedTimestamp >= tonightStarts.getTimeInMillis() && targetedTimestamp < tomorrowStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.tonight_part_1), context.getString(R.string.tonight_part_2)); // NIGHT
		}
		Calendar afterTomorrow = (Calendar) today.clone();
		afterTomorrow.add(Calendar.DATE, +2);
		if (targetedTimestamp >= tomorrowStarts.getTimeInMillis() && targetedTimestamp < afterTomorrow.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.tomorrow_part_1), context.getString(R.string.tomorrow_part_2)); // TOMORROW
		}
		Calendar nextWeekStarts = (Calendar) today.clone();
		nextWeekStarts.add(Calendar.DATE, +7);
		if (targetedTimestamp >= afterTomorrow.getTimeInMillis() && targetedTimestamp < nextWeekStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>(STANDALONE_DAY_OF_THE_WEEK_LONG.formatThreadSafe(targetedTimestamp), null); // THIS WEEK (Monday-Sunday)
		}
		Calendar nextWeekEnds = (Calendar) today.clone();
		nextWeekEnds.add(Calendar.DATE, +14);
		if (targetedTimestamp >= nextWeekStarts.getTimeInMillis() && targetedTimestamp < nextWeekEnds.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.next_week_part_1), context.getString(R.string.next_week_part_2)); // NEXT WEEK
		}
		Calendar thisMonthStarts = (Calendar) today.clone();
		thisMonthStarts.set(Calendar.DAY_OF_MONTH, 1);
		Calendar nextMonthStarts = (Calendar) thisMonthStarts.clone();
		nextMonthStarts.add(Calendar.MONTH, +1);
		if (targetedTimestamp >= thisMonthStarts.getTimeInMillis() && targetedTimestamp < nextMonthStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.this_month_part_1), context.getString(R.string.this_month_part_2)); // THIS MONTH
		}
		Calendar nextNextMonthStarts = (Calendar) nextMonthStarts.clone();
		nextNextMonthStarts.add(Calendar.MONTH, +1);
		if (targetedTimestamp >= nextMonthStarts.getTimeInMillis() && targetedTimestamp < nextNextMonthStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.next_month_part_1), context.getString(R.string.next_month_part_2)); // NEXT MONTH
		}
		Calendar next12MonthsStart = (Calendar) today.clone();
		next12MonthsStart.add(Calendar.MONTH, +1);
		Calendar next12MonthsEnd = (Calendar) today.clone();
		next12MonthsEnd.add(Calendar.MONTH, +6);
		if (targetedTimestamp >= next12MonthsStart.getTimeInMillis() && targetedTimestamp < next12MonthsEnd.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					STANDALONE_MONTH_LONG.formatThreadSafe(targetedTimestamp), null); // LESS THAN 12 MONTHS (January-December)
		}
		Calendar thisYearStarts = (Calendar) thisMonthStarts.clone();
		thisYearStarts.set(Calendar.MONTH, Calendar.JANUARY);
		Calendar nextYearStarts = (Calendar) thisYearStarts.clone();
		nextYearStarts.add(Calendar.YEAR, +1);
		Calendar nextNextYearStarts = (Calendar) nextYearStarts.clone();
		nextNextYearStarts.add(Calendar.YEAR, +1);
		if (targetedTimestamp >= nextYearStarts.getTimeInMillis() && targetedTimestamp < nextNextYearStarts.getTimeInMillis()) {
			return new Pair<CharSequence, CharSequence>( //
					context.getString(R.string.next_year_part_1), context.getString(R.string.next_year_part_2)); // NEXT YEAR
		}
		return new Pair<CharSequence, CharSequence>( //
				DateUtils.formatSameDayTime(targetedTimestamp, now, ThreadSafeDateFormatter.MEDIUM, ThreadSafeDateFormatter.SHORT), null); // DEFAULT
	}

	private static CharSequence getShortTimeSpanStringStyle(Context context, CharSequence timeSpan) {
		if (TextUtils.isEmpty(timeSpan)) {
			return timeSpan;
		}
		return SpanUtils.setAll(timeSpan, //
				SpanUtils.getNewSmallTextAppearance(context), // can be concatenated
				SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont())); // can be concatenated
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

		public interface TimeChangedListener {
			void onTimeChanged();
		}
	}
}
