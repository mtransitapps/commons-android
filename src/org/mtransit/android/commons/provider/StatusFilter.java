package org.mtransit.android.commons.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

public abstract class StatusFilter implements MTLog.Loggable {

	private static final String TAG = StatusFilter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final boolean CACHE_ONLY_DEFAULT = false;

	private static final boolean IN_FOCUS_DEFAULT = false;

	private String targetUUID = null;
	private int type = -1;
	private Boolean cacheOnly = null;
	private Long cacheValidityInMs = null;
	private Boolean inFocus = null;

	public StatusFilter(int type, String targetUUID) {
		this.type = type;
		this.targetUUID = targetUUID;
	}

	public String getTargetUUID() {
		return this.targetUUID;
	}

	public int getType() {
		return this.type;
	}

	public void setCacheOnly(Boolean cacheOnly) {
		this.cacheOnly = cacheOnly;
	}

	public boolean isCacheOnlyOrDefault() {
		return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
	}

	public Boolean getCacheOnlyOrNull() {
		return this.cacheOnly;
	}

	public void setInFocus(Boolean inFocus) {
		this.inFocus = inFocus;
	}

	public boolean isInFocusOrDefault() {
		return this.inFocus == null ? IN_FOCUS_DEFAULT : this.inFocus;
	}

	public Boolean getInFocusOrNull() {
		return this.inFocus;
	}

	public Long getCacheValidityInMsOrNull() {
		return this.cacheValidityInMs;
	}

	public boolean hasCacheValidityInMs() {
		return cacheValidityInMs != null && cacheValidityInMs > 0;
	}

	public void setCacheValidityInMs(Long cacheValidityInMs) {
		this.cacheValidityInMs = cacheValidityInMs;
	}

	public static int getTypeFromJSONString(String jsonString) {
		try {
			return jsonString == null ? -1 : getTypeFromJSON(new JSONObject(jsonString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
			return -1;
		}
	}

	public static int getTypeFromJSON(JSONObject json) throws JSONException {
		return json.getInt("type");
	}

	public static String getTargetUUIDFromJSON(JSONObject json) throws JSONException {
		return json.getString("target");
	}


	public static Long getCacheValidityInMsFromJSON(JSONObject json) throws JSONException {
		return json.has("cacheValidityInMs") ? json.getLong("cacheValidityInMs") : null;
	}

	public static void toJSON(StatusFilter statusFilter, JSONObject json) throws JSONException {
		json.put("type", statusFilter.getType());
		json.put("target", statusFilter.getTargetUUID());
		if (statusFilter.getCacheOnlyOrNull() != null) {
			json.put("cacheOnly", statusFilter.getCacheOnlyOrNull());
		}
		if (statusFilter.getInFocusOrNull() != null) {
			json.put("inFocus", statusFilter.getInFocusOrNull());
		}
		if (statusFilter.getCacheValidityInMsOrNull() != null) {
			json.put("cacheValidityInMs", statusFilter.getCacheValidityInMsOrNull());
		}
	}

	public static void fromJSON(StatusFilter statusFilter, JSONObject json) throws JSONException {
		statusFilter.type = json.getInt("type");
		statusFilter.targetUUID = json.getString("target");
		if (json.has("cacheOnly")) {
			statusFilter.cacheOnly = json.getBoolean("cacheOnly");
		}
		if (json.has("inFocus")) {
			statusFilter.inFocus = json.getBoolean("inFocus");
		}
		if (json.has("cacheValidityInMs")) {
			statusFilter.cacheValidityInMs = json.getLong("cacheValidityInMs");
		}
	}

	public abstract StatusFilter fromJSONStringStatic(String jsonString);

	public abstract String toJSONStringStatic(StatusFilter statusFilter);

}
