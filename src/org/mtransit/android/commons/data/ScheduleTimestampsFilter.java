package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

public class ScheduleTimestampsFilter implements MTLog.Loggable {

	private static final String TAG = ScheduleTimestampsFilter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private RouteTripStop rts;
	private long startsAtInMs;
	private long endsAtInMs;

	public ScheduleTimestampsFilter(RouteTripStop rts, long startsAtInMs, long endsAtInMs) {
		this.rts = rts;
		this.startsAtInMs = startsAtInMs;
		this.endsAtInMs = endsAtInMs;
	}

	public RouteTripStop getRouteTripStop() {
		return this.rts;
	}

	public long getStartsAtInMs() {
		return startsAtInMs;
	}

	public long getEndsAtInMs() {
		return endsAtInMs;
	}

	public static ScheduleTimestampsFilter fromJSONString(String jsonString) {
		try {
			return fromJSON(new JSONObject(jsonString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
			return null;
		}
	}

	public static ScheduleTimestampsFilter fromJSON(JSONObject json) {
		try {
			final RouteTripStop routeTripStop = RouteTripStop.fromJSONStatic(json.optJSONObject("routeTripStop"));
			final long startsAtInMs = json.getLong("startsAtInMs");
			final long endsAtInMs = json.getLong("endsAtInMs");
			final ScheduleTimestampsFilter scheduleTimestampsFilter = new ScheduleTimestampsFilter(/* targetUUID, */routeTripStop, startsAtInMs, endsAtInMs);
			return scheduleTimestampsFilter;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
			return null;
		}
	}

	public String toJSONStringStatic(ScheduleTimestampsFilter scheduleTimestampsFilter) {
		return toJSONString(scheduleTimestampsFilter);
	}

	public String toJSONString() {
		return toJSONString(this);
	}

	public static String toJSONString(ScheduleTimestampsFilter scheduleTimestampsFilter) {
		try {
			return toJSON(scheduleTimestampsFilter).toString();
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", scheduleTimestampsFilter);
			return null;
		}
	}

	public static JSONObject toJSON(ScheduleTimestampsFilter scheduleTimestampsFilter) throws JSONException {
		try {
			JSONObject json = new JSONObject();
			json.put("routeTripStop", scheduleTimestampsFilter.rts.toJSON());
			json.put("startsAtInMs", scheduleTimestampsFilter.startsAtInMs);
			json.put("endsAtInMs", scheduleTimestampsFilter.endsAtInMs);
			return json;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", scheduleTimestampsFilter);
			return null;
		}
	}

}
