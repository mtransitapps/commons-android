package org.mtransit.android.commons.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ScheduleTimestamps;

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

	@SuppressWarnings("WeakerAccess")
	class Filter implements MTLog.Loggable {

		private static final String LOG_TAG = ScheduleTimestampsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final RouteTripStop rts;
		private final long startsAtInMs;
		private final long endsAtInMs;

		public Filter(@NonNull RouteTripStop rts, long startsAtInMs, long endsAtInMs) {
			this.rts = rts;
			this.startsAtInMs = startsAtInMs;
			this.endsAtInMs = endsAtInMs;
		}

		@NonNull
		public RouteTripStop getRouteTripStop() {
			return this.rts;
		}

		public long getStartsAtInMs() {
			return startsAtInMs;
		}

		public long getEndsAtInMs() {
			return endsAtInMs;
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
				final RouteTripStop rts = RouteTripStop.fromJSONStatic(json.getJSONObject(JSON_ROUTE_TRIP_STOP));
				if (rts == null) {
					return null; // WTF?
				}
				long startsAtInMs = json.getLong(JSON_STARTS_AT_IN_MS);
				long endsAtInMs = json.getLong(JSON_ENDS_AT_IN_MS);
				return new Filter(rts, startsAtInMs, endsAtInMs);
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Nullable
		@SuppressWarnings("unused")
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
				json.put(JSON_STARTS_AT_IN_MS, scheduleTimestampsFilter.startsAtInMs);
				json.put(JSON_ENDS_AT_IN_MS, scheduleTimestampsFilter.endsAtInMs);
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", scheduleTimestampsFilter);
				return null;
			}
		}
	}
}
