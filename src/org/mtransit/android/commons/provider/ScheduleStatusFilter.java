package org.mtransit.android.commons.provider;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteTripStop;

public class ScheduleStatusFilter extends StatusFilter {

	private static final String TAG = ScheduleStatusFilter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int MIN_USEFUL_RESULTS_DEFAULT = 10;
	private static final int MAX_DATA_REQUESTS_DEFAULT = 7; // ALL WEEK
	private static final int LOOK_BEHIND_IN_MS_DEFAULT = 0; // 0 s

	private RouteTripStop routeTripStop = null;
	private Integer lookBehindInMs = null;
	private Long timestamp = null;
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
