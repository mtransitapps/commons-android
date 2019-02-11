package org.mtransit.android.commons.provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

	@NonNull
	ScheduleTimestamps getScheduleTimestamps(@NonNull Filter scheduleTimestampsFilter);

	class Filter implements MTLog.Loggable {

		private static final String LOG_TAG = ScheduleTimestampsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private RouteTripStop rts;
		private long startsAtInMs = -1L;
		private long endsAtInMs = -1L;

		public Filter(RouteTripStop rts, long startsAtInMs, long endsAtInMs) {
			this.rts = rts;
			this.startsAtInMs = startsAtInMs;
			this.endsAtInMs = endsAtInMs;
		}

		public RouteTripStop getRouteTripStop() {
			return this.rts;
		}

		public boolean isStartEndFilter() {
			return this.startsAtInMs >= 0L && this.endsAtInMs >= 0L;
		}

		public long getStartsAtInMs() {
			return this.startsAtInMs;
		}

		public long getEndsAtInMs() {
			return this.endsAtInMs;
		}

		@Nullable
		public static Filter fromJSONString(@Nullable String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_ROUTE_TRIP_STOP = "routeTripStop";
		private static final String JSON_STARTS_AT_IN_MS = "startsAtInMs";
		private static final String JSON_ENDS_AT_IN_MS = "endsAtInMs";

		@Nullable
		public static Filter fromJSON(@NonNull JSONObject json) {
			try {
				RouteTripStop routeTripStop = RouteTripStop.fromJSONStatic(json.optJSONObject(JSON_ROUTE_TRIP_STOP));
				if (json.has(JSON_STARTS_AT_IN_MS) && json.has(JSON_ENDS_AT_IN_MS)) {
					long startsAtInMs = json.getLong(JSON_STARTS_AT_IN_MS);
					long endsAtInMs = json.getLong(JSON_ENDS_AT_IN_MS);
					return new Filter(routeTripStop, startsAtInMs, endsAtInMs);
				} else {
					MTLog.w(LOG_TAG, "Unexpected filter while parsing JSON object '%s'", json);
					return null;
				}
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		public String toJSONString() {
			return toJSONString(this);
		}

		@Nullable
		public static String toJSONString(@NonNull Filter scheduleTimestampsFilter) {
			JSONObject json = toJSON(scheduleTimestampsFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		public static JSONObject toJSON(@NonNull Filter scheduleTimestampsFilter) {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_ROUTE_TRIP_STOP, scheduleTimestampsFilter.rts.toJSON());
				if (scheduleTimestampsFilter.isStartEndFilter()) {
					json.put(JSON_STARTS_AT_IN_MS, scheduleTimestampsFilter.startsAtInMs);
					json.put(JSON_ENDS_AT_IN_MS, scheduleTimestampsFilter.endsAtInMs);
					return json;
				} else {
					MTLog.w(LOG_TAG, "Unexpected filter while generating JSON object '%s'", scheduleTimestampsFilter);
					return null;
				}
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while generating JSON object '%s'", scheduleTimestampsFilter);
				return null;
			}
		}
	}
}
