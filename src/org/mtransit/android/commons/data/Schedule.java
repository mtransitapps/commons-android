package org.mtransit.android.commons.data;

import java.util.ArrayList;
import java.util.Comparator;
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
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Pair;

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

	private static ForegroundColorSpan defaultPastTextColorSpan = null;

	public static ForegroundColorSpan getDefaultPastTextColorSpan(Context context) {
		if (defaultPastTextColorSpan == null) {
			defaultPastTextColorSpan = SpanUtils.getTextColor(getDefaultPastTextColor(context));
		}
		return defaultPastTextColorSpan;
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

	private static ForegroundColorSpan defaultNowTextColorSpan = null;

	public static ForegroundColorSpan getDefaultNowTextColorSpan(Context context) {
		if (defaultNowTextColorSpan == null) {
			defaultNowTextColorSpan = SpanUtils.getTextColor(getDefaultNowTextColor(context));
		}
		return defaultNowTextColorSpan;
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

	private static ForegroundColorSpan defaultFutureTextColorSpan = null;

	public static ForegroundColorSpan getDefaultFutureTextColorSpan(Context context) {
		if (defaultFutureTextColorSpan == null) {
			defaultFutureTextColorSpan = SpanUtils.getTextColor(getDefaultFutureTextColor(context));
		}
		return defaultFutureTextColorSpan;
	}

	private static Typeface defaultFutureTypeface;

	public static Typeface getDefaultFutureTypeface() {
		if (defaultFutureTypeface == null) {
			defaultFutureTypeface = Typeface.DEFAULT;
		}
		return defaultFutureTypeface;
	}

	private ArrayList<Timestamp> timestamps = new ArrayList<Timestamp>();

	private long providerPrecisionInMs = 0;

	private ArrayList<Pair<CharSequence, CharSequence>> statusStrings = null;

	private long statusStringsTimestamp = -1;

	private CharSequence scheduleString = null;

	private long scheduleStringTimestamp = -1;

	private long usefulUntilInMs = -1;

	private boolean descentOnly = false;

	private ArrayList<Frequency> frequencies = new ArrayList<Frequency>();

	public Schedule(POIStatus status, long providerPrecisionInMs, boolean descentOnly) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), status.getReadFromSourceAtInMs(),
				providerPrecisionInMs, descentOnly);
	}

	public Schedule(String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs, boolean descentOnly) {
		this(null, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, providerPrecisionInMs, descentOnly);
	}

	public Schedule(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs,
			boolean descentOnly) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_SCHEDULE, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs);
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

	public static Schedule fromCursor(Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

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

	public void setTimestampsAndSort(ArrayList<Timestamp> timestamps) {
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

	private void resetUsefulUntilInMs() {
		int timestampsCount = getTimestampsCount();
		if (timestampsCount == 0) {
			this.usefulUntilInMs = 0; // NOT USEFUL
			return;
		}
		this.usefulUntilInMs = this.timestamps.get(timestampsCount - 1).t + this.providerPrecisionInMs;
	}

	public long getUsefulUntilInMs() {
		if (this.usefulUntilInMs < 0) {
			resetUsefulUntilInMs();
		}
		return usefulUntilInMs;
	}

	@Override
	public boolean isUseful() {
		if (!super.isUseful()) {
			return false;
		}
		return getUsefulUntilInMs() > TimeUtils.currentTimeToTheMinuteMillis();
	}

	public long getNextTimestamp(long after) {
		for (Timestamp timestamp : this.timestamps) {
			if (timestamp.t >= after) {
				return timestamp.t;
			}
		}
		return -1;
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

	private ArrayList<Timestamp> getNextTimestamps(long after, Long optMinCoverageInMs, Long optMaxCoverageInMs, Integer optMinCount, Integer optMaxCount) {
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

	public CharSequence getSchedule(Context context, long after, Long optMinCoverageInMs, Long optMaxCoverageInMs, Integer optMinCount, Integer optMaxCount) {
		if (this.scheduleString == null || this.scheduleStringTimestamp != after) {
			generateSchedule(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		}
		return this.scheduleString;
	}

	private void generateSchedule(Context context, long after, Long optMinCoverageInMs, Long optMaxCoverageInMs, Integer optMinCount, Integer optMaxCount) {
		ArrayList<Timestamp> nextTimestamps = getNextTimestamps(after - this.providerPrecisionInMs, optMinCoverageInMs, optMaxCoverageInMs, optMinCount,
				optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = new SpannableStringBuilder(context.getString(R.string.no_upcoming_departures));
			SpanUtils.set(ssb, SpanUtils.getSmallTextAppearance(context));
			SpanUtils.set(ssb, SpanUtils.getTextColor(ColorUtils.getTextColorTertiary(context)));
			SpanUtils.set(ssb, new RelativeSizeSpan(2.00f));
			this.scheduleString = ssb;
			this.scheduleStringTimestamp = after;
			return;
		}
		Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.HOURS.toMillis(1));
		if (lastTimestamp != null && nextTimestamps != null && !nextTimestamps.contains(lastTimestamp)) {
			nextTimestamps.add(0, lastTimestamp);
		}
		generateScheduleStringsTimes(context, after, nextTimestamps);
		this.scheduleStringTimestamp = after;
	}

	private static final String AM = "am";
	private static final String PM = "pm";

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
			SpanUtils.set(ssb, SpanUtils.getSmallTextAppearance(context), startPreviousTimes, endPreviousTimes);
			SpanUtils.set(ssb, SpanUtils.getTextColor(getDefaultPastTextColor(context)), startPreviousTimes, endPreviousTimes);
		}
		if (startPreviousTime < endPreviousTime) {
			SpanUtils.set(ssb, SpanUtils.getMediumTextAppearance(context), startPreviousTime, endPreviousTime);
			SpanUtils.set(ssb, SpanUtils.getTextColor(getDefaultPastTextColor(context)), startPreviousTime, endPreviousTime);
		}
		if (startNextTime < endNextTime) {
			SpanUtils.set(ssb, SpanUtils.getLargeTextAppearance(context), startNextTime, endNextTime);
			SpanUtils.set(ssb, SpanUtils.BOLD_STYLE_SPAN, startNextTime, endNextTime);
			SpanUtils.set(ssb, getDefaultNowTextColorSpan(context), startNextTime, endNextTime);
		}
		if (startNextNextTime < endNextNextTime) {
			SpanUtils.set(ssb, SpanUtils.getMediumTextAppearance(context), startNextNextTime, endNextNextTime);
			SpanUtils.set(ssb, SpanUtils.getTextColor(getDefaultFutureTextColor(context)), startNextNextTime, endNextNextTime);
		}
		if (startAfterNextTimes < endAfterNextTimes) {
			SpanUtils.set(ssb, SpanUtils.getSmallTextAppearance(context), startAfterNextTimes, endAfterNextTimes);
			SpanUtils.set(ssb, SpanUtils.getTextColor(getDefaultFutureTextColor(context)), startAfterNextTimes, endAfterNextTimes);
		}
		String word = ssb.toString().toLowerCase(Locale.ENGLISH);
		for (int index = word.indexOf(AM); index >= 0; index = word.indexOf(AM, index + 1)) { // TODO i18n
			if (index <= 0) {
				break;
			}
			SpanUtils.set(ssb, new RelativeSizeSpan(0.1f), index - 1, index); // remove space hack
			SpanUtils.set(ssb, new RelativeSizeSpan(0.25f), index, index + 2);
			index += 2;
		}
		for (int index = word.indexOf(PM); index >= 0; index = word.indexOf(PM, index + 1)) { // TODO i18n
			if (index <= 0) {
				break;
			}
			SpanUtils.set(ssb, new RelativeSizeSpan(0.1f), index - 1, index); // remove space hack
			SpanUtils.set(ssb, new RelativeSizeSpan(0.25f), index, index + 2);
		}
		SpanUtils.set(ssb, new RelativeSizeSpan(2.00f));
		this.scheduleString = ssb;
	}

	public ArrayList<Pair<CharSequence, CharSequence>> getStatus(Context context, long after, Long optMinCoverageInMs, Long optMaxCoverageInMs,
			Integer optMinCount, Integer optMaxCount) {
		if (this.statusStrings == null || this.statusStringsTimestamp != after) {
			generateStatus(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		}
		return this.statusStrings;
	}

	private void generateStatus(Context context, long after, Long optMinCoverageInMs, Long optMaxCoverageInMs, Integer optMinCount, Integer optMaxCount) {
		if (this.descentOnly) { // DESCENT ONLY
			if (this.statusStrings == null || this.statusStrings.size() == 0) {
				generateStatusStringsDescentOnly(context);
			} // ESLE descent only already set
			this.statusStringsTimestamp = after;
			return;
		}
		Frequency frequency = getCurrentFrequency(after);
		if (frequency != null) { // FREQUENCY
			generateStatusStringsFrequency(context, frequency);
			this.statusStringsTimestamp = after;
			return;
		}
		ArrayList<Timestamp> nextTimestamps = getNextTimestamps(after - this.providerPrecisionInMs, optMinCoverageInMs, optMaxCoverageInMs, optMinCount,
				optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			generateStatusStringsNoService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		long diffInMs = nextTimestamps.get(0).t - after;
		boolean isFrequentService = !this.descentOnly && diffInMs < TimeUtils.FREQUENT_SERVICE_TIMESPAN_IN_MS_DEFAULT
				&& TimeUtils.isFrequentService(nextTimestamps, -1, -1); // needs more than 3 services times!
		if (isFrequentService) { // FREQUENT SERVICE
			generateStatusStringsFrequentService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		generateStatusStringsTimes(context, after, diffInMs, nextTimestamps);
		this.statusStringsTimestamp = after;
	}

	private void generateStatusStringsTimes(Context context, long recentEnoughToBeNow, long diffInMs, ArrayList<Timestamp> nextTimestamps) {
		Pair<CharSequence, CharSequence> statusCS = TimeUtils.getShortTimeSpan(context, diffInMs, nextTimestamps.get(0).t, this.providerPrecisionInMs);
		CharSequence line1CS;
		CharSequence line2CS;
		if (diffInMs < TimeUtils.URGENT_SCHEDULE_IN_MS && CollectionUtils.getSize(nextTimestamps) > 1) { // URGENT & NEXT NEXT SCHEDULE
			long diff2InMs = nextTimestamps.get(1).t - recentEnoughToBeNow;
			Pair<CharSequence, CharSequence> nextStatusCS = TimeUtils.getShortTimeSpan(context, diff2InMs, nextTimestamps.get(1).t, this.providerPrecisionInMs);
			if (statusCS.second == null || statusCS.second.length() == 0) {
				line1CS = statusCS.first;
			} else {
				SpannableStringBuilder spaceSSB = new SpannableStringBuilder(StringUtils.SPACE_STRING);
				SpanUtils.set(spaceSSB, SpanUtils.getSmallTextAppearance(context));
				SpanUtils.set(spaceSSB, POIStatus.STATUS_TEXT_FONT);
				line1CS = TextUtils.concat(statusCS.first, spaceSSB, statusCS.second);
			}
			SpannableStringBuilder ssb1 = new SpannableStringBuilder(line1CS);
			SpanUtils.set(ssb1, POIStatus.getDefaultStatusTextColorSpan(context));
			line1CS = ssb1;
			if (nextStatusCS.second == null || nextStatusCS.second.length() == 0) {
				line2CS = nextStatusCS.first;
			} else {
				SpannableStringBuilder spaceSSB = new SpannableStringBuilder(StringUtils.SPACE_STRING);
				SpanUtils.set(spaceSSB, SpanUtils.getSmallTextAppearance(context));
				SpanUtils.set(spaceSSB, POIStatus.STATUS_TEXT_FONT);
				line2CS = TextUtils.concat(nextStatusCS.first, spaceSSB, nextStatusCS.second);
			}
			SpannableStringBuilder ssb2 = new SpannableStringBuilder(line2CS);
			SpanUtils.set(ssb2, POIStatus.getDefaultStatusTextColorSpan(context));
			line2CS = ssb2;
		} else { // NEXT SCHEDULE ONLY (large numbers)
			SpannableStringBuilder ssb1 = new SpannableStringBuilder(statusCS.first);
			if (diffInMs < TimeUtils.MAX_DURATION_SHOW_NUMBER_IN_MS) {
				SpanUtils.set(ssb1, SpanUtils.getLargeTextAppearance(context));
			}
			SpanUtils.set(ssb1, POIStatus.getDefaultStatusTextColorSpan(context));
			line1CS = ssb1;
			if (!TextUtils.isEmpty(statusCS.second)) {
				SpannableStringBuilder ssb2 = new SpannableStringBuilder(statusCS.second);
				SpanUtils.set(ssb2, POIStatus.getDefaultStatusTextColorSpan(context));
				line2CS = ssb2;
			} else {
				line2CS = null;
			}
		}
		this.statusStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.statusStrings.add(new Pair<CharSequence, CharSequence>(line1CS, line2CS));
	}

	private void generateStatusStringsNoService(Context context) {
		generateStatusStrings(context, R.string.no_service_part_1, R.string.no_service_part_2);
	}

	private void generateStatusStringsFrequency(Context context, Frequency frequency) {
		int headwayInMin = frequency == null ? 0 : (frequency.headwayInSec / 60);
		CharSequence headway = TimeUtils.getNumberInLetter(context, headwayInMin);
		String string1 = context.getResources().getQuantityString(R.plurals.every_minutes_and_quantity_part_1, headwayInMin, headway);
		String string2 = context.getResources().getQuantityString(R.plurals.every_minutes_and_quantity_part_2, headwayInMin, headway);
		generateStatusStrings(context, string1, string2);
	}

	private void generateStatusStringsFrequentService(Context context) {
		generateStatusStrings(context, R.string.frequent_service_part_1, R.string.frequent_service_part_2);
	}

	private void generateStatusStringsDescentOnly(Context context) {
		generateStatusStrings(context, R.string.descent_only_part_1, R.string.descent_only_part_2);
	}

	private void generateStatusStrings(Context context, int resId1, int resId2) {
		generateStatusStrings(context, context.getString(resId1), context.getString(resId2));
	}

	private void generateStatusStrings(Context context, CharSequence cs1, CharSequence cs2) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(cs1);
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs1SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(cs2);
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs2SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		this.statusStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.statusStrings.add(new Pair<CharSequence, CharSequence>(fs1SSB, fs2SSB));
	}

	private static class TimestampComparator implements Comparator<Timestamp> {
		@Override
		public int compare(Timestamp lhs, Timestamp rhs) {
			return (int) (lhs.t - rhs.t);
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

		public JSONObject toJSON() {
			return toJSON(this);
		}

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

		public String getHeading(Context context) {
			if (this.heading == null) {
				this.heading = getNewHeading(context);
			}
			return this.heading;
		}

		private String getNewHeading(Context context) {
			return Trip.getNewHeading(context, this.headsignType, this.headsignValue);
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
		private static final String JSON_HEADSING_VALUE = "hv";
		private static final String JSON_LOCAL_TIME_ZONE = "localTimeZone";

		public static Timestamp parseJSON(JSONObject jTimestamp) {
			try {
				long t = jTimestamp.getLong(JSON_TIMESTAMP);
				Timestamp timestamp = new Timestamp(t);
				int headsignType = jTimestamp.optInt(JSON_HEADSIGN_TYPE, -1);
				String headsignValue = jTimestamp.optString(JSON_HEADSING_VALUE, null);
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
					jTimestamp.put(JSON_HEADSING_VALUE, timestamp.headsignValue);
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

		private static final long MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT = TimeUnit.DAYS.toMillis(1);
		private static final int MIN_USEFUL_RESULTS_DEFAULT = 10;
		public static final int MAX_DATA_REQUESTS_DEFAULT = DATA_REQUEST_WEEK;
		private static final long LOOK_BEHIND_IN_MS_DEFAULT = TimeUnit.MILLISECONDS.toMillis(0);

		private RouteTripStop routeTripStop = null;
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
				scheduleStatusFilter.minUsefulDurationCoveredInMs = json.has(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS) ? json
						.getLong(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS) : null;
				scheduleStatusFilter.minUsefulResults = json.has(JSON_MIN_USEFUL_RESULTS) ? json.getInt(JSON_MIN_USEFUL_RESULTS) : null;
				scheduleStatusFilter.maxDataRequests = json.has(JSON_MAX_DATA_REQUESTS) ? json.getInt(JSON_MAX_DATA_REQUESTS) : null;
				return scheduleStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Override
		public String toJSONStringStatic(StatusProviderContract.Filter statusFilter) {
			return toJSONString(statusFilter);
		}

		public static String toJSONString(StatusProviderContract.Filter statusFilter) {
			try {
				JSONObject json = toJSON(statusFilter);
				return json == null ? null : json.toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", statusFilter);
				return null;
			}
		}

		public static JSONObject toJSON(StatusProviderContract.Filter statusFilter) throws JSONException {
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
