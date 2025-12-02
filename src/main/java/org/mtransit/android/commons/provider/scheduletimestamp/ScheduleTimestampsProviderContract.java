package org.mtransit.android.commons.provider.scheduletimestamp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.provider.common.ProviderContract;

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
		private final RouteDirectionStop rds;
		private final long startsAtInMs;
		private final long endsAtInMs;

		public Filter(@NonNull RouteDirectionStop rds, long startsAtInMs, long endsAtInMs) {
			this.rds = rds;
			this.startsAtInMs = startsAtInMs;
			this.endsAtInMs = endsAtInMs;
		}

		@NonNull
		public RouteDirectionStop getRouteDirectionStop() {
			return this.rds;
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

		private static final String JSON_ROUTE_DIRECTION_STOP = "routeTripStop"; // do not change to avoid breaking compat w/ old modules
		private static final String JSON_STARTS_AT_IN_MS = "startsAtInMs";
		private static final String JSON_ENDS_AT_IN_MS = "endsAtInMs";

		@Nullable
		public static Filter fromJSON(@NonNull JSONObject json) {
			try {
				final RouteDirectionStop rds = RouteDirectionStop.fromJSONStatic(json.getJSONObject(JSON_ROUTE_DIRECTION_STOP));
				if (rds == null) {
					return null; // WTF?
				}
				long startsAtInMs = json.getLong(JSON_STARTS_AT_IN_MS);
				long endsAtInMs = json.getLong(JSON_ENDS_AT_IN_MS);
				return new Filter(rds, startsAtInMs, endsAtInMs);
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
				json.put(JSON_ROUTE_DIRECTION_STOP, scheduleTimestampsFilter.rds.toJSON());
				json.put(JSON_STARTS_AT_IN_MS, scheduleTimestampsFilter.startsAtInMs);
				json.put(JSON_ENDS_AT_IN_MS, scheduleTimestampsFilter.endsAtInMs);
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", scheduleTimestampsFilter);
				return null;
			}
		}

		@NonNull
		@Override
		public String toString() {
			return Filter.class.getSimpleName() + "{" +
					"rts=" + rds.getUUID() +
					", startsAtInMs=" + startsAtInMs +
					", endsAtInMs=" + endsAtInMs +
					'}';
		}
	}
}
