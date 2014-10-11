package org.mtransit.android.commons.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.POI;

public class AvailabilityPercentStatusFilter extends StatusFilter {

	private static final String TAG = AvailabilityPercentStatusFilter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public AvailabilityPercentStatusFilter(String targetUUID) {
		super(POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, targetUUID);
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
			AvailabilityPercentStatusFilter availabilityPercentStatusFilter = new AvailabilityPercentStatusFilter(targetUUID);
			StatusFilter.fromJSON(availabilityPercentStatusFilter, json);
			return availabilityPercentStatusFilter;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
			return null;
		}
	}

	@Override
	public String toJSONStringStatic(StatusFilter statusFilter) {
		return toJSONString(statusFilter);
	}

	private static String toJSONString(StatusFilter statusFilter) {
		try {
			return toJSON(statusFilter).toString();
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", statusFilter);
			return null;
		}
	}

	private static JSONObject toJSON(StatusFilter statusFilter) throws JSONException {
		try {
			JSONObject json = new JSONObject();
			StatusFilter.toJSON(statusFilter, json);
			// if (statusFilter instanceof AvailabilityPercentStatusFilter) {
			// }
			return json;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
			return null;
		}
	}

}
