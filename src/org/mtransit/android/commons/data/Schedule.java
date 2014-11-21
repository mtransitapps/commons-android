package org.mtransit.android.commons.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.StatusFilter;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Pair;

public class Schedule extends POIStatus implements MTLog.Loggable {

	private static final String TAG = Schedule.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	protected static final TimestampComparator TIMESTAMPS_COMPARATOR = new TimestampComparator();

	private ArrayList<Timestamp> timestamps = new ArrayList<Timestamp>();

	private long providerPrecisionInMs = 0;

	private ArrayList<Pair<CharSequence, CharSequence>> nextTimesStrings = null;

	private long nextTimesStringsTimestamp = -1;

	private CharSequence timesListString = null;

	private long timesListStringTimestamp = -1;

	private long usefulUntilInMs = -1;

	private boolean decentOnly = false;

	public Schedule(POIStatus status, long providerPrecisionInMs, boolean decentOnly) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), providerPrecisionInMs, decentOnly);
	}

	public Schedule(String targetUUID, long lastUpdateInMs, long maxValidityInMs, long providerPrecisionInMs, boolean decentOnly) {
		this(null, targetUUID, lastUpdateInMs, maxValidityInMs, providerPrecisionInMs, decentOnly);
	}

	public Schedule(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long providerPrecisionInMs, boolean decentOnly) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_SCHEDULE, lastUpdateInMs, maxValidityInMs);
		this.decentOnly = decentOnly;
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
		final POIStatus status = POIStatus.fromCursor(cursor);
		final String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	private static Schedule fromExtraJSONString(POIStatus status, String extrasJSONString) {
		try {
			return fromExtraJSON(status, new JSONObject(extrasJSONString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retreiving extras information from cursor.");
			return null;
		}
	}

	private static Schedule fromExtraJSON(POIStatus status, JSONObject extrasJSON) {
		try {
			long providerPrecisionInMs = extrasJSON.getInt("providerPrecisionInMs");
			boolean decentOnly = extrasJSON.optBoolean("decentOnly", false);
			Schedule schedule = new Schedule(status, providerPrecisionInMs, decentOnly);
			JSONArray jTimestamps = extrasJSON.getJSONArray("timestamps");
			for (int i = 0; i < jTimestamps.length(); i++) {
				JSONObject jTimestamp = jTimestamps.getJSONObject(i);
				schedule.addTimestampWithoutSort(Timestamp.parseJSON(jTimestamp));
			}
			schedule.sortTimestamps();
			return schedule;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retreiving extras information from cursor.");
			return null;
		}
	}

	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("providerPrecisionInMs", this.providerPrecisionInMs);
			json.put("decentOnly", this.decentOnly);
			final JSONArray jTimestamps = new JSONArray();
			for (Timestamp timestamp : this.timestamps) {
				jTimestamps.put(timestamp.toJSON());
			}
			json.put("timestamps", jTimestamps);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	private void addTimestampWithoutSort(Timestamp newTimestamp) {
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
		final int timestampsCount = getTimestampsCount();
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
			if (optAfter != null && timestamp.t < optAfter.longValue()) {
				continue; // skip
			}
			lastTimestamp = timestamp;
		}
		return lastTimestamp;
	}

	private ArrayList<Timestamp> getNextTimestamps(long after, Long optBefore, Integer optCount) {
		ArrayList<Timestamp> nextTimestamps = new ArrayList<Timestamp>();
		boolean isAfter = false;
		int nbAfter = 0;
		for (Timestamp timestamp : this.timestamps) {
			if (optBefore != null && timestamp.t > optBefore.longValue()) {
				break;
			}
			if (!isAfter && timestamp.t >= after) {
				isAfter = true;
			}
			if (isAfter) {
				nextTimestamps.add(timestamp);
				nbAfter++;
				if (optCount != null && nbAfter >= optCount.intValue()) {
					break; // enough time stamps found
				}
			}
		}
		return nextTimestamps;
	}

	public CharSequence getTimesListString(Context context, long after, Long optBefore, Integer optCount) {
		if (this.timesListString == null || this.timesListStringTimestamp != after) {
			generateTimesListString(context, after, optBefore, optCount);
		}
		return this.timesListString;
	}

	private static final ThreadSafeDateFormatter FORMAT_TIME = ThreadSafeDateFormatter.getTimeInstance(ThreadSafeDateFormatter.SHORT);

	private void generateTimesListString(Context context, long after, Long optBefore, Integer optCount) {
		ArrayList<Timestamp> nextTimestamps = getNextTimestamps(after - this.providerPrecisionInMs, optBefore, optCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = new SpannableStringBuilder(context.getString(R.string.no_service));
			SpanUtils.set(ssb, SpanUtils.getSmallTextAppearance(context));
			SpanUtils.set(ssb, SpanUtils.getTextColor(ColorUtils.getTextColorTertiary(context)));
			SpanUtils.set(ssb, new RelativeSizeSpan(2.00f));
			this.timesListString = ssb;
			this.timesListStringTimestamp = after;
			return;
		}
		final Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUtils.ONE_HOUR_IN_MS);
		if (lastTimestamp != null && !nextTimestamps.contains(lastTimestamp)) {
			nextTimestamps.add(0, lastTimestamp);
		}
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
			ssb.append(FORMAT_TIME.formatThreadSafe(t.t));
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
			SpanUtils.set(ssb, SpanUtils.getTextColor(ColorUtils.getTextColorTertiary(context)), startPreviousTimes, endPreviousTimes);
		}
		if (startPreviousTime < endPreviousTime) {
			SpanUtils.set(ssb, SpanUtils.getMediumTextAppearance(context), startPreviousTime, endPreviousTime);
			SpanUtils.set(ssb, SpanUtils.getTextColor(ColorUtils.getTextColorTertiary(context)), startPreviousTime, endPreviousTime);
		}
		if (startNextTime < endNextTime) {
			SpanUtils.set(ssb, SpanUtils.getLargeTextAppearance(context), startNextTime, endNextTime);
			SpanUtils.set(ssb, SpanUtils.BOLD_STYLE_SPAN, startNextTime, endNextTime);
		}
		if (startNextNextTime < endNextNextTime) {
			SpanUtils.set(ssb, SpanUtils.getMediumTextAppearance(context), startNextTime, endNextTime);
			SpanUtils.set(ssb, SpanUtils.getTextColor(ColorUtils.getTextColorSecondary(context)), startAfterNextTimes, endAfterNextTimes);
		}
		if (startAfterNextTimes < endAfterNextTimes) {
			SpanUtils.set(ssb, SpanUtils.getSmallTextAppearance(context), startAfterNextTimes, endAfterNextTimes);
			SpanUtils.set(ssb, SpanUtils.getTextColor(ColorUtils.getTextColorSecondary(context)), startAfterNextTimes, endAfterNextTimes);
		}
		final String word = ssb.toString().toLowerCase(Locale.ENGLISH);
		final String amString = "am";
		for (int index = word.indexOf(amString); index >= 0; index = word.indexOf(amString, index + 1)) { // TODO i18n
			if (index <= 0) {
				break;
			}
			SpanUtils.set(ssb, new RelativeSizeSpan(0.1f), index - 1, index); // remove space hack
			SpanUtils.set(ssb, new RelativeSizeSpan(0.25f), index, index + 2);
			index += 2;
		}
		final String pmString = "pm";
		for (int index = word.indexOf(pmString); index >= 0; index = word.indexOf(pmString, index + 1)) { // TODO i18n
			if (index <= 0) {
				break;
			}
			SpanUtils.set(ssb, new RelativeSizeSpan(0.1f), index - 1, index); // remove space hack
			SpanUtils.set(ssb, new RelativeSizeSpan(0.25f), index, index + 2);
		}
		SpanUtils.set(ssb, new RelativeSizeSpan(2.00f));
		this.timesListString = ssb;
		this.timesListStringTimestamp = after;
	}

	public ArrayList<Pair<CharSequence, CharSequence>> getNextTimesStrings(Context context, long after, Long optBefore, Integer optCount) {
		if (this.nextTimesStrings == null || this.nextTimesStringsTimestamp != after) {
			generateNextTimesStrings(context, after, optBefore, optCount);
		}
		return this.nextTimesStrings;
	}

	private void generateNextTimesStrings(Context context, long after, Long optBefore, Integer optCount) {
		if (this.decentOnly) { // DECENT ONLY
			if (this.nextTimesStrings == null || this.nextTimesStrings.size() == 0) {
				generateNextTimesStringsDecentOnly(context);
			} // ESLE decent only already set
			this.nextTimesStringsTimestamp = after;
			return;
		}
		ArrayList<Timestamp> nextTimestamps = getNextTimestamps(after - this.providerPrecisionInMs, optBefore, optCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			generateNextTimesStringsNoService(context);
			this.nextTimesStringsTimestamp = after;
			return;
		}
		long diffInMs = nextTimestamps.get(0).t - after;
		boolean isFrequentService = !this.decentOnly && diffInMs < TimeUtils.FREQUENT_SERVICE_TIMESPAN_IN_MS_DEFAULT
				&& TimeUtils.isFrequentService(nextTimestamps, -1, -1); // needs more than 3 services times!
		if (isFrequentService) { // FREQUENT SERVICE
			generateNextTimesStringsFrequentService(context);
			this.nextTimesStringsTimestamp = after;
			return;
		}
		generateNextTimesStringsTimes(context, after, diffInMs, nextTimestamps);
		this.nextTimesStringsTimestamp = after;
	}

	private void generateNextTimesStringsTimes(Context context, long recentEnoughToBeNow, long diffInMs, ArrayList<Timestamp> nextTimestamps) {
		Pair<CharSequence, CharSequence> nextTimeCS = TimeUtils.getShortTimeSpan(context, diffInMs, nextTimestamps.get(0).t, this.providerPrecisionInMs);
		CharSequence line1CS;
		CharSequence line2CS;
		if (diffInMs < TimeUtils.URGENT_SCHEDULE_IN_MS && CollectionUtils.getSize(nextTimestamps) > 1) { // URGENT & NEXT NEXT SCHEDULE
			long diff2InMs = nextTimestamps.get(1).t - recentEnoughToBeNow;
			Pair<CharSequence, CharSequence> nextNextTimeCS = TimeUtils.getShortTimeSpan(context, diff2InMs, nextTimestamps.get(1).t,
					this.providerPrecisionInMs);
			if (nextTimeCS.second == null || nextTimeCS.second.length() == 0) {
				line1CS = nextTimeCS.first;
			} else {
				SpannableStringBuilder spaceSSB = new SpannableStringBuilder(StringUtils.SPACE_STRING);
				SpanUtils.set(spaceSSB, SpanUtils.getSmallTextAppearance(context));
				SpanUtils.set(spaceSSB, POIStatus.STATUS_TEXT_FONT);
				line1CS = TextUtils.concat(nextTimeCS.first, spaceSSB, nextTimeCS.second);
			}
			SpannableStringBuilder ssb1 = new SpannableStringBuilder(line1CS);
			SpanUtils.set(ssb1, POIStatus.getDefaultStatusTextColorSpan(context));
			line1CS = ssb1;
			if (nextNextTimeCS.second == null || nextNextTimeCS.second.length() == 0) {
				line2CS = nextNextTimeCS.first;
			} else {
				SpannableStringBuilder spaceSSB = new SpannableStringBuilder(StringUtils.SPACE_STRING);
				SpanUtils.set(spaceSSB, SpanUtils.getSmallTextAppearance(context));
				SpanUtils.set(spaceSSB, POIStatus.STATUS_TEXT_FONT);
				line2CS = TextUtils.concat(nextNextTimeCS.first, spaceSSB, nextNextTimeCS.second);
			}
			SpannableStringBuilder ssb2 = new SpannableStringBuilder(line2CS);
			SpanUtils.set(ssb2, POIStatus.getDefaultStatusTextColorSpan(context));
			line2CS = ssb2;
		} else { // NEXT SCHEDULE ONLY (large numbers)
			SpannableStringBuilder ssb1 = new SpannableStringBuilder(nextTimeCS.first);
			SpannableStringBuilder ssb2 = new SpannableStringBuilder(nextTimeCS.second == null ? StringUtils.EMPTY : nextTimeCS.second);
			if (diffInMs < TimeUtils.MAX_DURATION_SHOW_NUMBER_IN_MS) {
				SpanUtils.set(ssb1, SpanUtils.getLargeTextAppearance(context));
			}
			SpanUtils.set(ssb1, POIStatus.getDefaultStatusTextColorSpan(context));
			SpanUtils.set(ssb2, POIStatus.getDefaultStatusTextColorSpan(context));
			line1CS = ssb1;
			line2CS = ssb2;
		}
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(line1CS, line2CS));
	}

	private void generateNextTimesStringsNoService(Context context) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(context.getString(R.string.no_service_part_1));
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs1SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(context.getString(R.string.no_service_part_2));
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs2SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(fs1SSB, fs2SSB));
	}

	private void generateNextTimesStringsFrequentService(Context context) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(context.getString(R.string.frequent_service_part_1));
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs1SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(context.getString(R.string.frequent_service_part_2));
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs2SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(fs1SSB, fs2SSB));
	}

	private void generateNextTimesStringsDecentOnly(Context context) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(context.getString(R.string.descent_only_part_1));
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs1SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(context.getString(R.string.descent_only_part_2));
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(fs2SSB, POIStatus.getDefaultStatusTextColorSpan(context));
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(fs1SSB, fs2SSB));
	}

	private static class TimestampComparator implements Comparator<Timestamp> {
		@Override
		public int compare(Timestamp lhs, Timestamp rhs) {
			return (int) (lhs.t - rhs.t);
		}
	}

	public static class Timestamp {
		public long t;
		private int headsignType = -1;
		private String headsignValue = null;

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
			if (heading == null) {
				heading = getNewHeading(context);
			}
			return heading;
		}

		private String getNewHeading(Context context) {
			return Trip.getNewHeading(context, this.headsignType, this.headsignValue);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder('[');
			sb.append("t:").append(this.t);
			if (this.headsignType >= 0 && this.headsignValue != null) {
				sb.append(',');
				sb.append("ht:").append(this.headsignType);
				sb.append(',');
				sb.append("hv:").append(this.headsignValue);
			}
			sb.append(']');
			return sb.toString();
		}

		public static Timestamp parseJSON(JSONObject jTimestamp) {
			try {
				final long t = jTimestamp.getLong("t");
				Timestamp timestamp = new Timestamp(t);
				final int headsignType = jTimestamp.optInt("ht", -1);
				final String headsignValue = jTimestamp.optString("hv", null);
				if (headsignType >= 0 || headsignValue != null) {
					timestamp.setHeadsign(headsignType, headsignValue);
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
				jTimestamp.put("t", timestamp.t);
				if (timestamp.headsignType >= 0 && timestamp.headsignValue != null) {
					jTimestamp.put("ht", timestamp.headsignType);
					jTimestamp.put("hv", timestamp.headsignValue);
				}
				return jTimestamp;
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", timestamp);
				return null; // no partial result
			}
		}
	}

	public static class ScheduleStatusFilter extends StatusFilter {

		private static final String TAG = ScheduleStatusFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public static final int DATA_REQUEST_WEEK = 7;
		public static final int DATA_REQUEST_MONTH = 31;

		private static final long MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT = TimeUtils.ONE_DAY_IN_MS;
		private static final int MIN_USEFUL_RESULTS_DEFAULT = 10;
		public static final int MAX_DATA_REQUESTS_DEFAULT = DATA_REQUEST_WEEK;
		private static final int LOOK_BEHIND_IN_MS_DEFAULT = 0; // 0 s

		private RouteTripStop routeTripStop = null;
		private Integer lookBehindInMs = null;
		private Long timestamp = null;
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

		public int getLookBehindInMsOrDefault() {
			return lookBehindInMs == null ? LOOK_BEHIND_IN_MS_DEFAULT : lookBehindInMs.intValue();
		}

		public void setLookBehindInMs(Integer lookBehindInMs) {
			this.lookBehindInMs = lookBehindInMs;
		}

		public long getTimestampOrDefault() {
			return timestamp == null ? getNewDefaultTimestamp() : timestamp.longValue();
		}

		public void setTimestamp(Long timestamp) {
			this.timestamp = timestamp;
		}

		public long getMinUsefulDurationCoveredInMsOrDefault() {
			return this.minUsefulDurationCoveredInMs == null ? MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT : this.minUsefulDurationCoveredInMs.longValue();
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

		public Calendar getTimestampCalendarOrDefault() {
			return TimeUtils.getNewCalendar(getTimestampOrDefault());
		}

		private static long getNewDefaultTimestamp() {
			return TimeUtils.currentTimeToTheMinuteMillis();
		}

		@Override
		public StatusFilter fromJSONStringStatic(String jsonString) {
			return fromJSONString(jsonString);
		}

		public static StatusFilter fromJSONString(String jsonString) {
			try {
				return fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		public static StatusFilter fromJSON(JSONObject json) {
			try {
				String targetUUID = StatusFilter.getTargetUUIDFromJSON(json);
				RouteTripStop routeTripStop = RouteTripStop.fromJSONStatic(json.optJSONObject("routeTripStop"));
				ScheduleStatusFilter scheduleStatusFilter = new ScheduleStatusFilter(targetUUID, routeTripStop);
				StatusFilter.fromJSON(scheduleStatusFilter, json);
				scheduleStatusFilter.timestamp = json.has("timestamp") ? json.getLong("timestamp") : null;
				scheduleStatusFilter.lookBehindInMs = json.has("lookBehindInMs") ? json.getInt("lookBehindInMs") : null;
				scheduleStatusFilter.minUsefulDurationCoveredInMs = json.has("minUsefulDurationCoveredInMs") ? json.getLong("minUsefulDurationCoveredInMs")
						: null;
				scheduleStatusFilter.minUsefulResults = json.has("minUsefulResults") ? json.getInt("minUsefulResults") : null;
				scheduleStatusFilter.maxDataRequests = json.has("maxDataRequests") ? json.getInt("maxDataRequests") : null;
				return scheduleStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Override
		public String toJSONStringStatic(StatusFilter statusFilter) {
			return toJSONString(statusFilter);
		}

		public static String toJSONString(StatusFilter statusFilter) {
			try {
				return toJSON(statusFilter).toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", statusFilter);
				return null;
			}
		}

		public static JSONObject toJSON(StatusFilter statusFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				StatusFilter.toJSON(statusFilter, json);
				if (statusFilter instanceof ScheduleStatusFilter) {
					ScheduleStatusFilter scheduleFilter = (ScheduleStatusFilter) statusFilter;
					if (scheduleFilter.routeTripStop != null) {
						json.put("routeTripStop", scheduleFilter.routeTripStop.toJSON());
					}
					if (scheduleFilter.lookBehindInMs != null) {
						json.put("lookBehindInMs", scheduleFilter.lookBehindInMs);
					}
					if (scheduleFilter.timestamp != null) {
						json.put("timestamp", scheduleFilter.timestamp);
					}
					if (scheduleFilter.minUsefulDurationCoveredInMs != null) {
						json.put("minUsefulDurationCoveredInMs", scheduleFilter.minUsefulDurationCoveredInMs);
					}
					if (scheduleFilter.minUsefulResults != null) {
						json.put("minUsefulResults", scheduleFilter.minUsefulResults);
					}
					if (scheduleFilter.maxDataRequests != null) {
						json.put("maxDataRequests", scheduleFilter.maxDataRequests);
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
