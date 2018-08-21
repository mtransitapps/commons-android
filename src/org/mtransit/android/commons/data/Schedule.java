package org.mtransit.android.commons.data;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

public class Schedule extends POIStatus implements MTLog.Loggable {

	private static final String TAG = Schedule.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	protected static final TimestampComparator TIMESTAMPS_COMPARATOR = new TimestampComparator();

	protected static final FrequencyComparator FREQUENCIES_COMPARATOR = new FrequencyComparator();

	private static Integer defaultPastTextColor = null;

	public static int getDefaultPastTextColor(Context context) {
		if (defaultPastTextColor == null) {
			defaultPastTextColor = ColorUtils.getTextColorTertiary(context);
		}
		return defaultPastTextColor;
	}

	private static Typeface defaultPastTypeface;

	public static Typeface getDefaultPastTypeface() {
		if (defaultPastTypeface == null) {
			defaultPastTypeface = Typeface.DEFAULT;
		}
		return defaultPastTypeface;
	}

	private static Integer defaultNowTextColor = null;

	public static int getDefaultNowTextColor(Context context) {
		if (defaultNowTextColor == null) {
			defaultNowTextColor = ColorUtils.getTextColorPrimary(context);
		}
		return defaultNowTextColor;
	}

	private static Typeface defaultNowTypeface;

	public static Typeface getDefaultNowTypeface() {
		if (defaultNowTypeface == null) {
			defaultNowTypeface = Typeface.DEFAULT_BOLD;
		}
		return defaultNowTypeface;
	}

	private static Integer defaultFutureTextColor = null;

	public static int getDefaultFutureTextColor(Context context) {
		if (defaultFutureTextColor == null) {
			defaultFutureTextColor = ColorUtils.getTextColorPrimary(context);
		}
		return defaultFutureTextColor;
	}

	private static Typeface defaultFutureTypeface;

	public static Typeface getDefaultFutureTypeface() {
		if (defaultFutureTypeface == null) {
			defaultFutureTypeface = Typeface.DEFAULT;
		}
		return defaultFutureTypeface;
	}

	private ArrayList<Timestamp> timestamps = new ArrayList<Timestamp>();

	private long providerPrecisionInMs = 0L;

	private ArrayList<Pair<CharSequence, CharSequence>> statusStrings = null;

	private long statusStringsTimestamp = -1L;

	private ArrayList<Pair<CharSequence, CharSequence>> scheduleList = null;

	private long scheduleListTimestamp = -1L;

	private CharSequence scheduleString = null;

	private long scheduleStringTimestamp = -1L;

	private long usefulUntilInMs = -1L;

	private boolean descentOnly = false;

	private ArrayList<Frequency> frequencies = new ArrayList<Frequency>();

	public Schedule(POIStatus status, long providerPrecisionInMs, boolean descentOnly) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), status.getReadFromSourceAtInMs(),
				providerPrecisionInMs, descentOnly, status.isNoData());
	}

	public Schedule(String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs, boolean descentOnly) {
		this(null, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, providerPrecisionInMs, descentOnly);
	}

	public Schedule(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs,
			boolean descentOnly) {
		this(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, providerPrecisionInMs, descentOnly, false);
	}

	public Schedule(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs,
			boolean descentOnly, boolean noData) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_SCHEDULE, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, noData);
		this.descentOnly = descentOnly;
		this.providerPrecisionInMs = providerPrecisionInMs;
		resetUsefulUntilInMs();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Schedule.class.getSimpleName()).append(":[") //
				.append("targetUUID:").append(getTargetUUID()).append(',') //
				.append("timestamps:").append(this.timestamps) //
				.append(']').toString();
	}

	@Nullable
	public static Schedule fromCursor(Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	@Nullable
	private static Schedule fromExtraJSONString(POIStatus status, String extrasJSONString) {
		try {
			JSONObject json = extrasJSONString == null ? null : new JSONObject(extrasJSONString);
			if (json == null) {
				return null;
			}
			return fromExtraJSON(status, json);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Nullable
	private static Schedule fromExtraJSON(POIStatus status, JSONObject extrasJSON) {
		try {
			long providerPrecisionInMs = extrasJSON.getInt(JSON_PROVIDER_PRECISION_IN_MS);
			boolean descentOnly = extrasJSON.optBoolean(JSON_DESCENT_ONLY, false);
			Schedule schedule = new Schedule(status, providerPrecisionInMs, descentOnly);
			JSONArray jTimestamps = extrasJSON.getJSONArray(JSON_TIMESTAMPS);
			for (int i = 0; i < jTimestamps.length(); i++) {
				JSONObject jTimestamp = jTimestamps.getJSONObject(i);
				schedule.addTimestampWithoutSort(Timestamp.parseJSON(jTimestamp));
			}
			schedule.sortTimestamps();
			JSONArray jFrequencies = extrasJSON.getJSONArray(JSON_FREQUENCIES);
			for (int i = 0; i < jFrequencies.length(); i++) {
				JSONObject jFrequency = jFrequencies.getJSONObject(i);
				schedule.addFrequencyWithoutSort(Frequency.parseJSON(jFrequency));
			}
			schedule.sortFrequencies();
			return schedule;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static final String JSON_PROVIDER_PRECISION_IN_MS = "providerPrecisionInMs";
	private static final String JSON_DESCENT_ONLY = "decentOnly";
	private static final String JSON_TIMESTAMPS = "timestamps";
	private static final String JSON_FREQUENCIES = "frequencies";

	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_PROVIDER_PRECISION_IN_MS, this.providerPrecisionInMs);
			json.put(JSON_DESCENT_ONLY, this.descentOnly);
			JSONArray jTimestamps = new JSONArray();
			for (Timestamp timestamp : this.timestamps) {
				jTimestamps.put(timestamp.toJSON());
			}
			json.put(JSON_TIMESTAMPS, jTimestamps);
			JSONArray jFrequencies = new JSONArray();
			for (Frequency frequency : this.frequencies) {
				jFrequencies.put(frequency.toJSON());
			}
			json.put(JSON_FREQUENCIES, jFrequencies);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	public void setDescentOnly(boolean descentOnly) {
		this.descentOnly = descentOnly;
	}

	private void addFrequencyWithoutSort(Frequency newFrequency) {
		this.frequencies.add(newFrequency);
	}

	public void setFrequenciesAndSort(ArrayList<Frequency> frequencies) {
		this.frequencies = frequencies;
		sortFrequencies();
	}

	public void sortFrequencies() {
		CollectionUtils.sort(this.frequencies, FREQUENCIES_COMPARATOR);
		resetUsefulUntilInMs();
	}

	public ArrayList<Frequency> getFrequencies() {
		return this.frequencies;
	}

	public int getFrequenciesCount() {
		if (this.frequencies == null) {
			return 0;
		}
		return this.frequencies.size();
	}

	public void addTimestampWithoutSort(Timestamp newTimestamp) {
		this.timestamps.add(newTimestamp);
	}

	public void setTimestampsAndSort(@NonNull ArrayList<Timestamp> timestamps) {
		this.timestamps = timestamps;
		sortTimestamps();
	}

	public void sortTimestamps() {
		CollectionUtils.sort(this.timestamps, TIMESTAMPS_COMPARATOR);
		resetUsefulUntilInMs();
	}

	public ArrayList<Timestamp> getTimestamps() {
		return this.timestamps;
	}

	public int getTimestampsCount() {
		if (this.timestamps == null) {
			return 0;
		}
		return this.timestamps.size();
	}

	private static final long MIN_UI_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	private long getUIProviderPrecisionInMs() {
		return Math.max(MIN_UI_PRECISION_IN_MS, this.providerPrecisionInMs);
	}

	private void resetUsefulUntilInMs() {
		int timestampsCount = getTimestampsCount();
		if (timestampsCount == 0) {
			this.usefulUntilInMs = 0L; // NOT USEFUL
			return;
		}
		this.usefulUntilInMs = this.timestamps.get(timestampsCount - 1).t + getUIProviderPrecisionInMs();
	}

	public long getUsefulUntilInMs() {
		if (this.usefulUntilInMs < 0L) {
			resetUsefulUntilInMs();
		}
		return usefulUntilInMs;
	}

	@Override
	public boolean isUseful() {
		return super.isUseful() //
				&& getUsefulUntilInMs() > TimeUtils.currentTimeToTheMinuteMillis();
	}

	public Timestamp getNextTimestamp(long after) {
		for (Timestamp timestamp : this.timestamps) {
			if (timestamp.t >= after) {
				return timestamp;
			}
		}
		return null;
	}

	private Timestamp getLastTimestamp(long before, Long optAfter) {
		Timestamp lastTimestamp = null;
		for (Timestamp timestamp : this.timestamps) {
			if (timestamp.t >= before) {
				break;
			}
			if (optAfter != null && timestamp.t < optAfter) {
				continue; // skip
			}
			lastTimestamp = timestamp;
		}
		return lastTimestamp;
	}

	private Frequency getCurrentFrequency(long after) {
		if (this.frequencies != null) {
			for (Frequency frequency : this.frequencies) {
				if (frequency.startTimeInMs <= after && after <= frequency.endTimeInMs) {
					return frequency;
				}
			}
		}
		return null;
	}

	@NonNull
	protected ArrayList<Timestamp> getNextTimestamps(long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		ArrayList<Timestamp> nextTimestamps = new ArrayList<Timestamp>();
		boolean isAfter = false;
		int nbAfter = 0;
		Boolean minCoverageInMsCompleted = optMinCoverageInMs == null ? null : false;
		Boolean minCountCompleted = optMinCount == null ? null : false;
		for (Timestamp timestamp : this.timestamps) {
			if (optMaxCoverageInMs != null && timestamp.t > after + optMaxCoverageInMs) {
				break;
			}
			if (minCoverageInMsCompleted != null && !minCoverageInMsCompleted && timestamp.t > after + optMinCoverageInMs) {
				if (minCountCompleted != null && minCountCompleted) {
					break;
				}
				minCoverageInMsCompleted = true;
			}
			if (!isAfter && timestamp.t >= after) {
				isAfter = true;
			}
			if (isAfter) {
				nextTimestamps.add(timestamp);
				nbAfter++;
				if (optMaxCount != null && nbAfter >= optMaxCount) {
					break; // enough time stamps found
				}
				if (minCountCompleted != null && !minCountCompleted && nbAfter >= optMinCount) {
					if (minCoverageInMsCompleted != null && minCoverageInMsCompleted) {
						break;
					}
					minCountCompleted = true;
				}
			}
		}
		return nextTimestamps;
	}

	public ArrayList<Pair<CharSequence, CharSequence>> getScheduleList(Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount, @Nullable String optDefaultHeadSign) {
		if (this.scheduleList == null || this.scheduleListTimestamp != after) {
			generateScheduleList(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount, optDefaultHeadSign);
		}
		return this.scheduleList;
	}

	private static TextAppearanceSpan noServiceTextAppearance = null;

	private static TextAppearanceSpan getNoServiceTextAppearance(Context context) {
		if (noServiceTextAppearance == null) {
			noServiceTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return noServiceTextAppearance;
	}

	private static ForegroundColorSpan noServiceTextColor = null;

	private static ForegroundColorSpan getNoServiceTextColor(Context context) {
		if (noServiceTextColor == null) {
			noServiceTextColor = SpanUtils.getNewTextColor(ColorUtils.getTextColorTertiary(context));
		}
		return noServiceTextColor;
	}

	private static final RelativeSizeSpan NO_SERVICE_SIZE = SpanUtils.getNew200PercentSizeSpan();

	private void generateScheduleList(Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount, @Nullable String optDefaultHeadSign) {
		ArrayList<Timestamp> nextTimestamps =
				getNextTimestamps(after - getUIProviderPrecisionInMs(), optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = null;
			try {
				Timestamp timestamp = getNextTimestamp(after);
				if (timestamp != null && timestamp.t >= 0L) {
					ssb = new SpannableStringBuilder(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp.t)));
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing next timestamp date time!");
			}
			if (ssb == null) {
				ssb = new SpannableStringBuilder(context.getString(R.string.no_upcoming_departures));
			}
			ssb = SpanUtils.setAll(ssb, //
					getNoServiceTextAppearance(context), getNoServiceTextColor(context), NO_SERVICE_SIZE);
			this.scheduleList = new ArrayList<Pair<CharSequence, CharSequence>>();
			this.scheduleList.add(new Pair<CharSequence, CharSequence>(ssb, null));
			this.scheduleListTimestamp = after;
			return;
		}
		Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.HOURS.toMillis(1L));
		if (lastTimestamp != null && !nextTimestamps.contains(lastTimestamp)) {
			nextTimestamps.add(0, lastTimestamp);
		}
		generateScheduleListTimes(context, after, nextTimestamps, optDefaultHeadSign);
		this.scheduleListTimestamp = after;
	}

	private void generateScheduleListTimes(Context context, long after, ArrayList<Timestamp> nextTimestamps, @Nullable String optDefaultHeadSign) {
		ArrayList<Pair<CharSequence, CharSequence>> list = new ArrayList<Pair<CharSequence, CharSequence>>();
		int startPreviousTimesIndex = -1, endPreviousTimesIndex = -1;
		int startPreviousTimeIndex = -1, endPreviousTimeIndex = -1;
		int startNextTimeIndex = -1, endNextTimeIndex = -1;
		int startNextNextTimeIndex = -1, endNextNextTimeIndex = -1;
		int startAfterNextTimesIndex = -1, endAfterNextTimesIndex = -1;
		int index = 0;
		for (Timestamp t : nextTimestamps) {
			if (endPreviousTimeIndex == -1) {
				if (t.t >= after) {
					if (startPreviousTimeIndex != -1) {
						endPreviousTimeIndex = index;
					}
				} else {
					endPreviousTimesIndex = index;
					startPreviousTimeIndex = endPreviousTimesIndex;
				}
			}
			if (t.t < after) {
				if (endPreviousTimeIndex == -1) {
					if (startPreviousTimesIndex == -1) {
						startPreviousTimesIndex = index;
					}
				}
			}
			if (t.t >= after) {
				if (startNextTimeIndex == -1) {
					startNextTimeIndex = index;
				}
			}
			index++;
			if (t.t >= after) {
				if (endNextTimeIndex == -1) {
					if (startNextTimeIndex != index) {
						endNextTimeIndex = index;
						startNextNextTimeIndex = endNextTimeIndex;
						endNextNextTimeIndex = startNextNextTimeIndex; // if was last, the same means empty
					}
				} else if (endNextNextTimeIndex != -1 && endNextNextTimeIndex == startNextNextTimeIndex) {
					endNextNextTimeIndex = index;
					startAfterNextTimesIndex = endNextNextTimeIndex;
					endAfterNextTimesIndex = startAfterNextTimesIndex; // if was last, the same means empty
				} else if (endAfterNextTimesIndex != -1 && startAfterNextTimesIndex != -1) {
					endAfterNextTimesIndex = index;
				}
			}
		}
		index = 0;
		int nbSpaceBefore = 0;
		int nbSpaceAfter = 0;
		for (Timestamp t : nextTimestamps) {
			index++;
			SpannableStringBuilder headSignSSB = null;
			SpannableStringBuilder timeSSB = new SpannableStringBuilder(TimeUtils.formatTime(context, t.t));
			if (t.hasHeadsign() && !Trip.isSameHeadsign(t.getHeading(context), optDefaultHeadSign)) {
				headSignSSB = new SpannableStringBuilder(t.getHeading(context).toUpperCase(Locale.ENGLISH));
			}
			if (startPreviousTimesIndex < endPreviousTimesIndex //
					&& index > startPreviousTimesIndex && index <= endPreviousTimesIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesFarTextAppearance(context), getScheduleListTimesPastTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
				}
			} else //
			if (startPreviousTimeIndex < endPreviousTimeIndex //
					&& index > startPreviousTimeIndex && index <= endPreviousTimeIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesPastTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
				}
			} else //
			if (startNextTimeIndex < endNextTimeIndex //
					&& index > startNextTimeIndex && index <= endNextTimeIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesClosestTextAppearance(context), getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_STYLE);
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesNowTextColor(context));
				}
			} else //
			if (startNextNextTimeIndex < endNextNextTimeIndex //
					&& index > startNextNextTimeIndex && index <= endNextNextTimeIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesFutureTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesFutureTextColor(context));
				}
			} else //
			if (startAfterNextTimesIndex < endAfterNextTimesIndex //
					&& index > startAfterNextTimesIndex && index <= endAfterNextTimesIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesFarTextAppearance(context), getScheduleListTimesFutureTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesFutureTextColor(context));
				}
			}
			TimeUtils.cleanTimes(timeSSB);
			timeSSB = SpanUtils.setAll(timeSSB, SCHEDULE_LIST_TIMES_SIZE);
			if (headSignSSB != null && headSignSSB.length() > 0) {
				headSignSSB = SpanUtils.setAll(headSignSSB, SCHEDULE_LIST_TIMES_STYLE);
			}
			list.add(new Pair<CharSequence, CharSequence>(timeSSB, headSignSSB));
		}
		this.scheduleList = list;
	}

	private static final RelativeSizeSpan SCHEDULE_LIST_TIMES_SIZE = SpanUtils.getNew200PercentSizeSpan();

	private static final StyleSpan SCHEDULE_LIST_TIMES_STYLE = SpanUtils.getNewBoldStyleSpan();

	private static TextAppearanceSpan scheduleListTimesFarTextAppearance = null;

	private static TextAppearanceSpan getScheduleListTimesFarTextAppearance(Context context) {
		if (scheduleListTimesFarTextAppearance == null) {
			scheduleListTimesFarTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return scheduleListTimesFarTextAppearance;
	}

	private static TextAppearanceSpan scheduleListTimesCloseTextAppearance = null;

	private static TextAppearanceSpan getScheduleListTimesCloseTextAppearance(Context context) {
		if (scheduleListTimesCloseTextAppearance == null) {
			scheduleListTimesCloseTextAppearance = SpanUtils.getNewMediumTextAppearance(context);
		}
		return scheduleListTimesCloseTextAppearance;
	}

	private static TextAppearanceSpan scheduleListTimesClosestTextAppearance = null;

	private static TextAppearanceSpan getScheduleListTimesClosestTextAppearance(Context context) {
		if (scheduleListTimesClosestTextAppearance == null) {
			scheduleListTimesClosestTextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return scheduleListTimesClosestTextAppearance;
	}

	private static ForegroundColorSpan scheduleListTimesPastTextColor = null;

	private static ForegroundColorSpan getScheduleListTimesPastTextColor(Context context) {
		if (scheduleListTimesPastTextColor == null) {
			scheduleListTimesPastTextColor = SpanUtils.getNewTextColor(getDefaultPastTextColor(context));
		}
		return scheduleListTimesPastTextColor;
	}

	private static ForegroundColorSpan scheduleListTimesNowTextColor = null;

	private static ForegroundColorSpan getScheduleListTimesNowTextColor(Context context) {
		if (scheduleListTimesNowTextColor == null) {
			scheduleListTimesNowTextColor = SpanUtils.getNewTextColor(getDefaultNowTextColor(context));
		}
		return scheduleListTimesNowTextColor;
	}

	private static ForegroundColorSpan scheduleListTimesFutureTextColor = null;

	private static ForegroundColorSpan getScheduleListTimesFutureTextColor(Context context) {
		if (scheduleListTimesFutureTextColor == null) {
			scheduleListTimesFutureTextColor = SpanUtils.getNewTextColor(getDefaultFutureTextColor(context));
		}
		return scheduleListTimesFutureTextColor;
	}

	public CharSequence getSchedule(Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		if (this.scheduleString == null || this.scheduleStringTimestamp != after) {
			generateSchedule(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		}
		return this.scheduleString;
	}

	private void generateSchedule(Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		ArrayList<Timestamp> nextTimestamps =
				getNextTimestamps(after - getUIProviderPrecisionInMs(), optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = null;
			try {
				Timestamp timestamp = getNextTimestamp(after);
				if (timestamp != null && timestamp.t >= 0L) {
					ssb = new SpannableStringBuilder(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp.t)));
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing next timestamp date time!");
			}
			if (ssb == null) {
				ssb = new SpannableStringBuilder(context.getString(R.string.no_upcoming_departures));
			}
			ssb = SpanUtils.setAll(ssb, //
					getNoServiceTextAppearance(context), //
					getNoServiceTextColor(context), //
					NO_SERVICE_SIZE);
			this.scheduleString = ssb;
			this.scheduleStringTimestamp = after;
			return;
		}
		Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.HOURS.toMillis(1));
		if (lastTimestamp != null && !nextTimestamps.contains(lastTimestamp)) {
			nextTimestamps.add(0, lastTimestamp);
		}
		generateScheduleStringsTimes(context, after, nextTimestamps);
		this.scheduleStringTimestamp = after;
	}

	private void generateScheduleStringsTimes(Context context, long after, ArrayList<Timestamp> nextTimestamps) {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int startPreviousTimes = -1, endPreviousTimes = -1;
		int startPreviousTime = -1, endPreviousTime = -1;
		int startNextTime = -1, endNextTime = -1;
		int startNextNextTime = -1, endNextNextTime = -1;
		int startAfterNextTimes = -1, endAfterNextTimes = -1;
		for (Timestamp t : nextTimestamps) {
			if (ssb.length() > 0) {
				ssb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR);
			}
			if (endPreviousTime == -1) {
				if (t.t >= after) {
					if (startPreviousTime != -1) {
						endPreviousTime = ssb.length();
					}
				} else {
					endPreviousTimes = ssb.length();
					startPreviousTime = endPreviousTimes;
				}
			}
			if (t.t < after) {
				if (endPreviousTime == -1) {
					if (startPreviousTimes == -1) {
						startPreviousTimes = ssb.length();
					}
				}
			}
			if (t.t >= after) {
				if (startNextTime == -1) {
					startNextTime = ssb.length();
				}
			}
			ssb.append(TimeUtils.formatTime(context, t.t));
			if (t.t >= after) {
				if (endNextTime == -1) {
					if (startNextTime != ssb.length()) {
						endNextTime = ssb.length();
						startNextNextTime = endNextTime;
						endNextNextTime = startNextNextTime; // if was last, the same means empty
					}
				} else if (endNextNextTime != -1 && endNextNextTime == startNextNextTime) {
					endNextNextTime = ssb.length();
					startAfterNextTimes = endNextNextTime;
					endAfterNextTimes = startAfterNextTimes; // if was last, the same means empty
				} else if (endAfterNextTimes != -1 && startAfterNextTimes != -1) {
					endAfterNextTimes = ssb.length();
				}
			}
		}
		if (startPreviousTimes < endPreviousTimes) {
			ssb = SpanUtils.set(ssb, startPreviousTimes, endPreviousTimes, //
					getScheduleListTimesFarTextAppearance(context), getScheduleListTimesPastTextColor(context));
		}
		if (startPreviousTime < endPreviousTime) {
			ssb = SpanUtils.set(ssb, startPreviousTime, endPreviousTime, //
					getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesPastTextColor1(context));
		}
		if (startNextTime < endNextTime) {
			ssb = SpanUtils.set(ssb, startNextTime, endNextTime, //
					getScheduleListTimesClosestTextAppearance(context), getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_STYLE);
		}
		if (startNextNextTime < endNextNextTime) {
			ssb = SpanUtils.set(ssb, startNextNextTime, endNextNextTime, //
					getScheduleListTimesCloseTextAppearance1(context), getScheduleListTimesFutureTextColor(context));
		}
		if (startAfterNextTimes < endAfterNextTimes) {
			ssb = SpanUtils.set(ssb, startAfterNextTimes, endAfterNextTimes, //
					getScheduleListTimesFarTextAppearance1(context), getScheduleListTimesFutureTextColor1(context));
		}
		TimeUtils.cleanTimes(ssb);
		ssb = SpanUtils.setAll(ssb, SpanUtils.getNew200PercentSizeSpan());
		this.scheduleString = ssb;
	}

	private static ForegroundColorSpan scheduleListTimesFutureTextColor1 = null;

	private static ForegroundColorSpan getScheduleListTimesFutureTextColor1(Context context) {
		if (scheduleListTimesFutureTextColor1 == null) {
			scheduleListTimesFutureTextColor1 = SpanUtils.getNewTextColor(getDefaultFutureTextColor(context));
		}
		return scheduleListTimesFutureTextColor1;
	}

	private static TextAppearanceSpan scheduleListTimesCloseTextAppearance1 = null;

	private static TextAppearanceSpan getScheduleListTimesCloseTextAppearance1(Context context) {
		if (scheduleListTimesCloseTextAppearance1 == null) {
			scheduleListTimesCloseTextAppearance1 = SpanUtils.getNewMediumTextAppearance(context);
		}
		return scheduleListTimesCloseTextAppearance1;
	}

	private static TextAppearanceSpan scheduleListTimesFarTextAppearance1 = null;

	private static TextAppearanceSpan getScheduleListTimesFarTextAppearance1(Context context) {
		if (scheduleListTimesFarTextAppearance1 == null) {
			scheduleListTimesFarTextAppearance1 = SpanUtils.getNewSmallTextAppearance(context);
		}
		return scheduleListTimesFarTextAppearance1;
	}

	private static ForegroundColorSpan scheduleListTimesPastTextColor1 = null;

	private static ForegroundColorSpan getScheduleListTimesPastTextColor1(Context context) {
		if (scheduleListTimesPastTextColor1 == null) {
			scheduleListTimesPastTextColor1 = SpanUtils.getNewTextColor(getDefaultPastTextColor(context));
		}
		return scheduleListTimesPastTextColor1;
	}

	public ArrayList<Pair<CharSequence, CharSequence>> getStatus(Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		if (this.statusStrings == null || this.statusStringsTimestamp != after) {
			generateStatus(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		}
		return this.statusStrings;
	}

	public static final long MAX_FREQUENCY_DISPLAYED_IN_SEC = TimeUnit.MINUTES.toSeconds(15L);

	private static final long MAX_LAST_STATUS_DIFF_IN_MS = TimeUnit.MINUTES.toMillis(5L);

	protected void generateStatus(Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		if (isNoData()) { // NO DATA
			return;
		}
		if (this.descentOnly) { // DESCENT ONLY
			if (this.statusStrings == null || this.statusStrings.size() == 0) {
				generateStatusStringsDescentOnly(context);
			} // ELSE descent only already set
			this.statusStringsTimestamp = after;
			return;
		}
		Frequency frequency = getCurrentFrequency(after);
		if (frequency != null && frequency.headwayInSec < MAX_FREQUENCY_DISPLAYED_IN_SEC) { // FREQUENCY
			generateStatusStringsFrequency(context, frequency);
			this.statusStringsTimestamp = after;
			return;
		}
		ArrayList<Long> nextTimestamps = getStatusNextTimestamps(after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			generateStatusStringsNoService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		long diffInMs = nextTimestamps.get(0) - after;
		// TODO diffInMs can be < 0 !! ?
		boolean isFrequentService = //
				!this.descentOnly //
						&& diffInMs < TimeUtils.FREQUENT_SERVICE_TIME_SPAN_IN_MS_DEFAULT //
						&& TimeUtils.isFrequentService(nextTimestamps, -1, -1); // needs more than 3 services times!
		if (isFrequentService) { // FREQUENT SERVICE
			generateStatusStringsFrequentService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		nextTimestamps = filterStatusNextTimestampsTimes(nextTimestamps);
		generateStatusStringsTimes(context, after, diffInMs, nextTimestamps);
		this.statusStringsTimestamp = after;
	}

	protected static ArrayList<Long> filterStatusNextTimestampsTimes(ArrayList<Long> nextTimestampList) {
		ArrayList<Long> nextTimestampsT = new ArrayList<>();
		Long lastTimestamp = null;
		for (Long timestamp : nextTimestampList) {
			if (nextTimestampsT.contains(timestamp)) {
				continue; // skip duplicate time
			}
			if (lastTimestamp != null //
					&& (timestamp - lastTimestamp) < MIN_UI_PRECISION_IN_MS) {
				continue; // skip near duplicate time
			}
			nextTimestampsT.add(timestamp);
			lastTimestamp = timestamp;
		}
		return nextTimestampsT;
	}

	@NonNull
	protected ArrayList<Long> getStatusNextTimestamps(long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
			@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		long usefulPastInMs = Math.max(MAX_LAST_STATUS_DIFF_IN_MS, getUIProviderPrecisionInMs());
		ArrayList<Timestamp> nextTimestampList = getNextTimestamps(after - usefulPastInMs, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		ArrayList<Long> nextTimestampsT = new ArrayList<>();
		for (Timestamp timestamp : nextTimestampList) {
			Long tt = timestamp.t;
			nextTimestampsT.add(tt);
		}
		if (nextTimestampsT.size() > 0) {
			Long theNextTimestamp = null;
			Long theLastTimestamp = null;
			for (Long timestamp : nextTimestampsT) {
				if (timestamp >= after) {
					if (theNextTimestamp == null || timestamp < theNextTimestamp) {
						theNextTimestamp = timestamp;
					}
				} else {
					if (theLastTimestamp == null || theLastTimestamp < timestamp) {
						theLastTimestamp = timestamp;
					}
				}
			}
			Long oldestUsefulTimestamp = null;
			if (theNextTimestamp != null) {
				oldestUsefulTimestamp = after - ((theNextTimestamp - after) / 2L);
			}
			if (theLastTimestamp != null) {
				if (oldestUsefulTimestamp == null //
						|| oldestUsefulTimestamp < theLastTimestamp) {
					oldestUsefulTimestamp = theLastTimestamp;
				}
			}
			if (oldestUsefulTimestamp != null) {
				Iterator<Long> it = nextTimestampsT.iterator();
				while (it.hasNext()) {
					Long timestamp = it.next();
					if (timestamp < oldestUsefulTimestamp) {
						it.remove();
					}
				}
			}
		}
		return nextTimestampsT;
	}

	private static ForegroundColorSpan statusStringsTextColor1 = null;

	private static ForegroundColorSpan getStatusStringsTextColor1(Context context) {
		if (statusStringsTextColor1 == null) {
			statusStringsTextColor1 = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusStringsTextColor1;
	}

	private static ForegroundColorSpan statusStringsTextColor2 = null;

	private static ForegroundColorSpan getStatusStringsTextColor2(Context context) {
		if (statusStringsTextColor2 == null) {
			statusStringsTextColor2 = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusStringsTextColor2;
	}

	private static ForegroundColorSpan statusStringsTextColor3 = null;

	private static ForegroundColorSpan getStatusStringsTextColor3(Context context) {
		if (statusStringsTextColor3 == null) {
			statusStringsTextColor3 = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusStringsTextColor3;
	}

	private void generateStatusStringsTimes(Context context, long recentEnoughToBeNow, long diffInMs, ArrayList<Long> nextTimestamps) {
		Pair<CharSequence, CharSequence> statusCS = TimeUtils.getShortTimeSpan(context, diffInMs, nextTimestamps.get(0), getUIProviderPrecisionInMs());
		CharSequence line1CS;
		CharSequence line2CS;
		if (diffInMs < TimeUtils.URGENT_SCHEDULE_IN_MS && CollectionUtils.getSize(nextTimestamps) > 1) { // URGENT & NEXT NEXT SCHEDULE
			if (statusCS.second == null || statusCS.second.length() == 0) {
				line1CS = SpanUtils.setAll(statusCS.first, getStatusStringsTextColor1(context));
			} else {
				line1CS = TextUtils.concat( //
						SpanUtils.setAll(statusCS.first, getStatusStringsTextColor1(context)), //
						SpanUtils.setAll(getNewStatusSpaceSSB(context), getStatusStringsTextColor2(context)), //
						SpanUtils.setAll(statusCS.second, getStatusStringsTextColor3(context)));
			}
			long diff2InMs = nextTimestamps.get(1) - recentEnoughToBeNow;
			Pair<CharSequence, CharSequence> nextStatusCS = TimeUtils.getShortTimeSpan(context, diff2InMs, nextTimestamps.get(1), getUIProviderPrecisionInMs());
			if (nextStatusCS.second == null || nextStatusCS.second.length() == 0) {
				line2CS = SpanUtils.setAll(nextStatusCS.first, getStatusStringsTextColor1(context));
			} else {
				line2CS = TextUtils.concat( //
						SpanUtils.setAll(nextStatusCS.first, getStatusStringsTextColor1(context)), //
						SpanUtils.setAll(getNewStatusSpaceSSB(context), getStatusStringsTextColor2(context)), //
						SpanUtils.setAll(nextStatusCS.second, getStatusStringsTextColor3(context)));
			}
		} else { // NEXT SCHEDULE ONLY (large numbers)
			if (diffInMs < TimeUtils.MAX_DURATION_SHOW_NUMBER_IN_MS) {
				line1CS = SpanUtils.setAll(statusCS.first, //
						getStatusStringsTimesNumberShownTextAppearance(context), //
						getStatusStringsTextColor1(context));
			} else {
				line1CS = SpanUtils.setAll(statusCS.first, //
						getStatusStringsTextColor1(context));
			}
			if (!TextUtils.isEmpty(statusCS.second)) {
				line2CS = SpanUtils.setAll(statusCS.second, getStatusStringsTextColor1(context));
			} else {
				line2CS = null;
			}
		}
		this.statusStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.statusStrings.add(new Pair<CharSequence, CharSequence>(line1CS, line2CS));
	}

	private static TextAppearanceSpan statusStringsTimesNumberShownTextAppearance = null;

	private static TextAppearanceSpan getStatusStringsTimesNumberShownTextAppearance(Context context) {
		if (statusStringsTimesNumberShownTextAppearance == null) {
			statusStringsTimesNumberShownTextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return statusStringsTimesNumberShownTextAppearance;
	}

	private static final TypefaceSpan STATUS_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static TextAppearanceSpan statusSpaceTextAppearance = null;

	private static TextAppearanceSpan getStatusSpaceTextAppearance(Context context) {
		if (statusSpaceTextAppearance == null) {
			statusSpaceTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return statusSpaceTextAppearance;
	}

	private SpannableStringBuilder getNewStatusSpaceSSB(Context context) {
		return SpanUtils.setAll(new SpannableStringBuilder(StringUtils.SPACE_STRING), //
				getStatusSpaceTextAppearance(context), STATUS_FONT);
	}

	private void generateStatusStringsFrequency(Context context, Frequency frequency) {
		int headwayInMin = frequency == null ? 0 : (frequency.headwayInSec / 60);
		CharSequence headway = TimeUtils.getNumberInLetter(context, headwayInMin);
		generateStatusStrings(context, //
				context.getResources().getQuantityString(R.plurals.every_minutes_and_quantity_part_1, headwayInMin, headway), //
				context.getResources().getQuantityString(R.plurals.every_minutes_and_quantity_part_2, headwayInMin, headway));
	}

	private void generateStatusStringsNoService(Context context) {
		generateStatusStrings(context, context.getString(R.string.no_service_part_1), context.getString(R.string.no_service_part_2));
	}

	private void generateStatusStringsFrequentService(Context context) {
		generateStatusStrings(context, context.getString(R.string.frequent_service_part_1), context.getString(R.string.frequent_service_part_2));
	}

	private void generateStatusStringsDescentOnly(Context context) {
		generateStatusStrings(context, context.getString(R.string.descent_only_part_1), context.getString(R.string.descent_only_part_2));
	}

	private void generateStatusStrings(Context context, CharSequence cs1, CharSequence cs2) {
		this.statusStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.statusStrings.add(new Pair<CharSequence, CharSequence>(//
				SpanUtils.setAll(cs1, //
						getStatusStringTextAppearance(context), //
						STATUS_FONT, //
						getStatusStringsTextColor1(context)), //
				SpanUtils.setAll(cs2, //
						getStatusStringTextAppearance(context), //
						STATUS_FONT, //
						getStatusStringsTextColor2(context))));
	}

	private static TextAppearanceSpan statusStringTextAppearance = null;

	private static TextAppearanceSpan getStatusStringTextAppearance(Context context) {
		if (statusStringTextAppearance == null) {
			statusStringTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return statusStringTextAppearance;
	}

	private static class TimestampComparator implements Comparator<Timestamp> {
		@Override
		public int compare(Timestamp lhs, Timestamp rhs) {
			long lt = lhs == null ? 0L : lhs.t;
			long rt = rhs == null ? 0L : rhs.t;
			return (int) (lt - rt);
		}
	}

	private static class FrequencyComparator implements Comparator<Frequency> {
		@Override
		public int compare(Frequency lhs, Frequency rhs) {
			if (lhs.startTimeInMs == rhs.startTimeInMs) {
				return (int) (lhs.endTimeInMs - rhs.endTimeInMs);
			}
			return (int) (lhs.startTimeInMs - rhs.startTimeInMs);
		}
	}

	public static class Frequency implements MTLog.Loggable {

		private static final String TAG = Frequency.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public long startTimeInMs;
		public long endTimeInMs;
		public int headwayInSec;

		public Frequency(long startTimeInMs, long endTimeInMs, int headwayInSec) {
			this.startTimeInMs = startTimeInMs;
			this.endTimeInMs = endTimeInMs;
			this.headwayInSec = headwayInSec;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(Frequency.class.getSimpleName()).append('[');
			sb.append("startTimeInMs:").append(this.startTimeInMs).append(',');
			sb.append("endTimeInMs:").append(this.endTimeInMs).append(',');
			sb.append("headwayInSec:").append(this.headwayInSec).append(',');
			return sb.append(']').toString();
		}

		@Nullable
		public static Frequency parseJSON(JSONObject jFrequency) {
			try {
				long startTimeInMs = jFrequency.getLong(JSON_START_TIME_IN_MS);
				long endTimeInMs = jFrequency.getLong(JSON_END_TIME_IN_MS);
				int headwayInSec = jFrequency.getInt(JSON_HEADWAY_IN_SEC);
				return new Frequency(startTimeInMs, endTimeInMs, headwayInSec);
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'!", jFrequency);
				return null; // no partial results
			}
		}

		private static final String JSON_START_TIME_IN_MS = "startTimeInMs";
		private static final String JSON_END_TIME_IN_MS = "endTimeInMs";
		private static final String JSON_HEADWAY_IN_SEC = "headwayInSec";

		@Nullable
		public JSONObject toJSON() {
			return toJSON(this);
		}

		@Nullable
		public static JSONObject toJSON(Frequency frequency) {
			try {
				JSONObject jFrequency = new JSONObject();
				jFrequency.put(JSON_START_TIME_IN_MS, frequency.startTimeInMs);
				jFrequency.put(JSON_END_TIME_IN_MS, frequency.endTimeInMs);
				jFrequency.put(JSON_HEADWAY_IN_SEC, frequency.headwayInSec);
				return jFrequency;
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", frequency);
				return null; // no partial result
			}
		}
	}

	public static class Timestamp implements MTLog.Loggable {

		private static final String TAG = Timestamp.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public long t;
		private int headsignType = -1;
		private String headsignValue = null;
		private String localTimeZone = null;

		public Timestamp(long t) {
			this.t = t;
		}

		public void setHeadsign(int headsignType, String headsignValue) {
			this.headsignType = headsignType;
			this.headsignValue = headsignValue;
		}

		public boolean hasHeadsign() {
			return this.headsignType >= 0 && !TextUtils.isEmpty(this.headsignValue);
		}

		private String heading = null;

		@NonNull
		public String getHeading(@NonNull Context context) {
			if (this.heading == null) {
				this.heading = getNewHeading(context);
			}
			return this.heading;
		}

		@Nullable
		public String getHeading() {
			if (this.heading == null) {
				this.heading = getNewHeading();
			}
			return this.heading;
		}

		@NonNull
		private String getNewHeading(@NonNull Context context) {
			return Trip.getNewHeading(context, this.headsignType, this.headsignValue);
		}

		@Nullable
		private String getNewHeading() {
			return Trip.getNewHeading(this.headsignType, this.headsignValue);
		}

		public void setLocalTimeZone(String localTimeZone) {
			this.localTimeZone = localTimeZone;
		}

		public String getLocalTimeZone() {
			return localTimeZone;
		}

		public boolean hasLocalTimeZone() {
			return !TextUtils.isEmpty(this.localTimeZone);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder().append('[');
			sb.append("t:").append(this.t);
			if (this.headsignType >= 0 && this.headsignValue != null) {
				sb.append(',');
				sb.append("ht:").append(this.headsignType);
				sb.append(',');
				sb.append("hv:").append(this.headsignValue);
				sb.append(',');
				sb.append("ltz:").append(this.localTimeZone);
			}
			sb.append(']');
			return sb.toString();
		}

		private static final String JSON_TIMESTAMP = "t";
		private static final String JSON_HEADSIGN_TYPE = "ht";
		private static final String JSON_HEADSIGN_VALUE = "hv";
		private static final String JSON_LOCAL_TIME_ZONE = "localTimeZone";

		public static Timestamp parseJSON(JSONObject jTimestamp) {
			try {
				long t = jTimestamp.getLong(JSON_TIMESTAMP);
				Timestamp timestamp = new Timestamp(t);
				int headsignType = jTimestamp.optInt(JSON_HEADSIGN_TYPE, -1);
				String headsignValue = jTimestamp.optString(JSON_HEADSIGN_VALUE, null);
				if (headsignType >= 0 || headsignValue != null) {
					timestamp.setHeadsign(headsignType, headsignValue);
				}
				String localTimeZone = jTimestamp.optString(JSON_LOCAL_TIME_ZONE);
				if (!TextUtils.isEmpty(localTimeZone)) {
					timestamp.setLocalTimeZone(localTimeZone);
				}
				return timestamp;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'!", jTimestamp);
				return null; // no partial results
			}
		}

		public JSONObject toJSON() {
			return toJSON(this);
		}

		public static JSONObject toJSON(Timestamp timestamp) {
			try {
				JSONObject jTimestamp = new JSONObject();
				jTimestamp.put(JSON_TIMESTAMP, timestamp.t);
				if (timestamp.headsignType >= 0 && timestamp.headsignValue != null) {
					jTimestamp.put(JSON_HEADSIGN_TYPE, timestamp.headsignType);
					jTimestamp.put(JSON_HEADSIGN_VALUE, timestamp.headsignValue);
				}
				if (timestamp.hasLocalTimeZone()) {
					jTimestamp.put(JSON_LOCAL_TIME_ZONE, timestamp.localTimeZone);
				}
				return jTimestamp;
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", timestamp);
				return null; // no partial result
			}
		}
	}

	public static class ScheduleStatusFilter extends StatusProviderContract.Filter {

		private static final String TAG = ScheduleStatusFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public static final int DATA_REQUEST_WEEK = 7;
		public static final int DATA_REQUEST_MONTHS = 62;
		public static final int DATA_REQUEST_YEAR = 365;

		private static final long MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT = TimeUnit.DAYS.toMillis(1L);
		private static final int MIN_USEFUL_RESULTS_DEFAULT = 10;
		public static final int MAX_DATA_REQUESTS_DEFAULT = DATA_REQUEST_WEEK;
		private static final long LOOK_BEHIND_IN_MS_DEFAULT = TimeUnit.MILLISECONDS.toMillis(0L);

		private RouteTripStop routeTripStop;
		private Long lookBehindInMs = null;
		private Long minUsefulDurationCoveredInMs = null;
		private Integer minUsefulResults = null;
		private Integer maxDataRequests = null;

		public ScheduleStatusFilter(String targetUUID, RouteTripStop rts) {
			super(POI.ITEM_STATUS_TYPE_SCHEDULE, targetUUID);
			this.routeTripStop = rts;
		}

		public RouteTripStop getRouteTripStop() {
			return routeTripStop;
		}

		public void setRouteTripStop(RouteTripStop routeTripStop) {
			this.routeTripStop = routeTripStop;
		}

		public long getLookBehindInMsOrDefault() {
			return lookBehindInMs == null ? LOOK_BEHIND_IN_MS_DEFAULT : lookBehindInMs;
		}

		public void setLookBehindInMs(Long lookBehindInMs) {
			this.lookBehindInMs = lookBehindInMs;
		}

		public long getTimestampOrDefault() {
			return getNewDefaultTimestamp();
		}

		public long getMinUsefulDurationCoveredInMsOrDefault() {
			return this.minUsefulDurationCoveredInMs == null ? MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT : this.minUsefulDurationCoveredInMs;
		}

		public void setMinUsefulDurationCoveredInMs(Long minUsefulDurationCoveredInMs) {
			this.minUsefulDurationCoveredInMs = minUsefulDurationCoveredInMs;
		}

		public int getMinUsefulResultsOrDefault() {
			return minUsefulResults == null ? MIN_USEFUL_RESULTS_DEFAULT : minUsefulResults;
		}

		public void setMinUsefulResults(Integer minUsefulResults) {
			this.minUsefulResults = minUsefulResults;
		}

		public int getMaxDataRequestsOrDefault() {
			return maxDataRequests == null ? MAX_DATA_REQUESTS_DEFAULT : maxDataRequests;
		}

		public void setMaxDataRequests(Integer maxDataRequests) {
			this.maxDataRequests = maxDataRequests;
		}

		private static long getNewDefaultTimestamp() {
			return TimeUtils.currentTimeToTheMinuteMillis();
		}

		@Override
		public StatusProviderContract.Filter fromJSONStringStatic(String jsonString) {
			return fromJSONString(jsonString);
		}

		public static StatusProviderContract.Filter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_MIN_USEFUL_DURATION_COVERED_IN_MS = "minUsefulDurationCoveredInMs";
		private static final String JSON_MIN_USEFUL_RESULTS = "minUsefulResults";
		private static final String JSON_MAX_DATA_REQUESTS = "maxDataRequests";
		private static final String JSON_ROUTE_TRIP_STOP = "routeTripStop";
		private static final String JSON_LOOK_BEHIND_IN_MS = "lookBehindInMs";

		public static StatusProviderContract.Filter fromJSON(JSONObject json) {
			try {
				String targetUUID = StatusProviderContract.Filter.getTargetUUIDFromJSON(json);
				RouteTripStop routeTripStop = RouteTripStop.fromJSONStatic(json.optJSONObject(JSON_ROUTE_TRIP_STOP));
				ScheduleStatusFilter scheduleStatusFilter = new ScheduleStatusFilter(targetUUID, routeTripStop);
				StatusProviderContract.Filter.fromJSON(scheduleStatusFilter, json);
				scheduleStatusFilter.lookBehindInMs = json.has(JSON_LOOK_BEHIND_IN_MS) ? json.getLong(JSON_LOOK_BEHIND_IN_MS) : null;
				scheduleStatusFilter.minUsefulDurationCoveredInMs =
						json.has(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS) ? json.getLong(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS) : null;
				scheduleStatusFilter.minUsefulResults = json.has(JSON_MIN_USEFUL_RESULTS) ? json.getInt(JSON_MIN_USEFUL_RESULTS) : null;
				scheduleStatusFilter.maxDataRequests = json.has(JSON_MAX_DATA_REQUESTS) ? json.getInt(JSON_MAX_DATA_REQUESTS) : null;
				return scheduleStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Nullable
		@Override
		public String toJSONStringStatic(StatusProviderContract.Filter statusFilter) {
			return toJSONString(statusFilter);
		}

		@Nullable
		public static String toJSONString(StatusProviderContract.Filter statusFilter) {
			JSONObject json = toJSON(statusFilter);
			return json == null ? null : json.toString();
		}

		public static JSONObject toJSON(StatusProviderContract.Filter statusFilter) {
			try {
				JSONObject json = new JSONObject();
				StatusProviderContract.Filter.toJSON(statusFilter, json);
				if (statusFilter instanceof ScheduleStatusFilter) {
					ScheduleStatusFilter scheduleFilter = (ScheduleStatusFilter) statusFilter;
					if (scheduleFilter.routeTripStop != null) {
						json.put(JSON_ROUTE_TRIP_STOP, scheduleFilter.routeTripStop.toJSON());
					}
					if (scheduleFilter.lookBehindInMs != null) {
						json.put(JSON_LOOK_BEHIND_IN_MS, scheduleFilter.lookBehindInMs);
					}
					if (scheduleFilter.minUsefulDurationCoveredInMs != null) {
						json.put(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS, scheduleFilter.minUsefulDurationCoveredInMs);
					}
					if (scheduleFilter.minUsefulResults != null) {
						json.put(JSON_MIN_USEFUL_RESULTS, scheduleFilter.minUsefulResults);
					}
					if (scheduleFilter.maxDataRequests != null) {
						json.put(JSON_MAX_DATA_REQUESTS, scheduleFilter.maxDataRequests);
					}
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
				return null;
			}
		}
	}
}
