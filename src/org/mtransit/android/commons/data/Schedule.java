package org.mtransit.android.commons.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TimeUtils;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Pair;

public class Schedule extends POIStatus implements MTLog.Loggable {

	private static final String TAG = Schedule.class.getSimpleName();

	private static final TimestampComparator TIMESTAMPS_COMPARATOR = new TimestampComparator();

	private List<Timestamp> timestamps = new ArrayList<Timestamp>();

	private long providerPrecisionInMs = 0;

	private List<Pair<CharSequence, CharSequence>> nextTimesStrings = null;

	private long nextTimesStringsTimestamp = -1;

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
				.append(timestamps) //
				.append(']').toString();
	}

	@Override
	public String getLogTag() {
		return TAG;
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

	public void setTimestampsAndSort(List<Timestamp> timestamps) {
		this.timestamps = timestamps;
		sortTimestamps();
	}

	public void sortTimestamps() {
		CollectionUtils.sort(this.timestamps, TIMESTAMPS_COMPARATOR);
		resetUsefulUntilInMs();
	}

	public List<Timestamp> getTimestamps() {
		return timestamps;
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

	public List<Timestamp> getNextTimestamps(long after, int count) {
		List<Timestamp> nextTimestamps = new ArrayList<Timestamp>();
		boolean isAfter = false;
		int nbAfter = 0;
		for (Timestamp timestamp : this.timestamps) {
			if (timestamp.t >= after) {
				isAfter = true;
			}
			if (isAfter) {
				nextTimestamps.add(timestamp);
				nbAfter++;
				if (nbAfter >= count) {
					break; // enough time stamps found
				}
			}
		}
		return nextTimestamps;
	}

	public List<Pair<CharSequence, CharSequence>> getNextTimesStrings(Context context, long after, int count) {
		if (this.nextTimesStrings == null || this.nextTimesStringsTimestamp != after) {
			generateNextTimesStrings(context, after, count);
		}
		return this.nextTimesStrings;
	}

	private void generateNextTimesStrings(Context context, long after, int count) {
		if (this.decentOnly) { // DECENT ONLY
			if (this.nextTimesStrings == null || this.nextTimesStrings.size() == 0) {
				generateNextTimesStringsDecentOnly(context);
			} // ESLE decent only already set
			this.nextTimesStringsTimestamp = after;
			return;
		}
		List<Timestamp> nextTimestamps = getNextTimestamps(after - this.providerPrecisionInMs, count);
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

	private void generateNextTimesStringsTimes(Context context, long recentEnoughToBeNow, long diffInMs, List<Timestamp> nextTimestamps) {
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
				SpannableStringBuilder spaceSSB = new SpannableStringBuilder(" ");
				SpanUtils.set(spaceSSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
				SpanUtils.set(spaceSSB, SpanUtils.getSmallTextAppearance(context));
				line1CS = TextUtils.concat(nextTimeCS.first, spaceSSB, nextTimeCS.second);
			}
			if (nextNextTimeCS.second == null || nextNextTimeCS.second.length() == 0) {
				line2CS = nextNextTimeCS.first;
			} else {
				SpannableStringBuilder spaceSSB = new SpannableStringBuilder(" ");
				SpanUtils.set(spaceSSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
				SpanUtils.set(spaceSSB, SpanUtils.getSmallTextAppearance(context));
				line2CS = TextUtils.concat(nextNextTimeCS.first, spaceSSB, nextNextTimeCS.second);
			}
		} else { // NEXT SCHEDULE ONLY (large numbers)
			SpannableStringBuilder ssb = new SpannableStringBuilder(nextTimeCS.first);
			if (diffInMs < TimeUtils.MAX_DURATION_SHOW_NUMBER_IN_MS) {
				SpanUtils.set(ssb, SpanUtils.getLargeTextAppearance(context));
			}
			line1CS = ssb;
			line2CS = nextTimeCS.second;
		}
		if (diffInMs < TimeUtils.URGENT_SCHEDULE_IN_MS) { // URGENT => BOLD
			SpannableStringBuilder ssb = new SpannableStringBuilder(line1CS);
			SpanUtils.set(ssb, SpanUtils.BOLD_STYLE_SPAN);
			line1CS = ssb;
		}
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(line1CS, line2CS));
	}

	private void generateNextTimesStringsNoService(Context context) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(context.getString(R.string.no_service_part_1));
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(context.getString(R.string.no_service_part_2));
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(fs1SSB, fs2SSB));
	}

	private void generateNextTimesStringsFrequentService(Context context) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(context.getString(R.string.frequent_service_part_1));
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(context.getString(R.string.frequent_service_part_2));
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
		this.nextTimesStrings = new ArrayList<Pair<CharSequence, CharSequence>>();
		this.nextTimesStrings.add(new Pair<CharSequence, CharSequence>(fs1SSB, fs2SSB));
	}

	private void generateNextTimesStringsDecentOnly(Context context) {
		SpannableStringBuilder fs1SSB = new SpannableStringBuilder(context.getString(R.string.descent_only_part_1));
		SpanUtils.set(fs1SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs1SSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
		SpannableStringBuilder fs2SSB = new SpannableStringBuilder(context.getString(R.string.descent_only_part_2));
		SpanUtils.set(fs2SSB, SpanUtils.getSmallTextAppearance(context));
		SpanUtils.set(fs2SSB, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN);
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

}
