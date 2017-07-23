package org.mtransit.android.commons.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ScheduleTimestamps;

import android.text.TextUtils;

public interface ScheduleTimestampsProviderContract extends ProviderContract {

	String SCHEDULE_TIMESTAMPS_PATH = "schedule";

	class Columns {
		public static final String T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID = "target";
		public static final String T_SCHEDULE_TIMESTAMPS_K_EXTRAS = "extras";
		public static final String T_SCHEDULE_TIMESTAMPS_K_STARTS_AT = "startsAt";
		public static final String T_SCHEDULE_TIMESTAMPS_K_ENDS_AT = "endsAt";
	}

	ScheduleTimestamps getScheduleTimestamps(Filter scheduleTimestampsFilter);

	class Filter implements MTLog.Loggable {

		private static final String TAG = ScheduleTimestampsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private RouteTripStop rts;
		private long startsAtInMs;
		private long endsAtInMs;

		public Filter(RouteTripStop rts, long startsAtInMs, long endsAtInMs) {
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

		public static Filter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_ROUTE_TRIP_STOP = "routeTripStop";
		private static final String JSON_STARTS_AT_IN_MS = "startsAtInMs";
		private static final String JSON_ENDS_AT_IN_MS = "endsAtInMs";

		public static Filter fromJSON(JSONObject json) {
			try {
				RouteTripStop routeTripStop = RouteTripStop.fromJSONStatic(json.optJSONObject(JSON_ROUTE_TRIP_STOP));
				long startsAtInMs = json.getLong(JSON_STARTS_AT_IN_MS);
				long endsAtInMs = json.getLong(JSON_ENDS_AT_IN_MS);
				return new Filter(routeTripStop, startsAtInMs, endsAtInMs);
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		public String toJSONString() {
			return toJSONString(this);
		}

		public static String toJSONString(Filter scheduleTimestampsFilter) {
			try {
				JSONObject json = toJSON(scheduleTimestampsFilter);
				return json == null ? null : json.toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", scheduleTimestampsFilter);
				return null;
			}
		}

		public static JSONObject toJSON(Filter scheduleTimestampsFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_ROUTE_TRIP_STOP, scheduleTimestampsFilter.rts.toJSON());
				json.put(JSON_STARTS_AT_IN_MS, scheduleTimestampsFilter.startsAtInMs);
				json.put(JSON_ENDS_AT_IN_MS, scheduleTimestampsFilter.endsAtInMs);
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", scheduleTimestampsFilter);
				return null;
			}
		}
	}
}
