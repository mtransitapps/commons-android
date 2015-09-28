package org.mtransit.android.commons.provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.POIStatus;

import android.net.Uri;
import android.provider.BaseColumns;

public interface StatusProviderContract extends ProviderContract {

	String STATUS_PATH = "status";

	long getStatusMaxValidityInMs();

	long getStatusValidityInMs(boolean inFocus);

	long getMinDurationBetweenRefreshInMs(boolean inFocus);

	POIStatus getNewStatus(Filter statusFilter);

	void cacheStatus(POIStatus newStatusToCache);

	POIStatus getCachedStatus(Filter statusFilter);

	boolean purgeUselessCachedStatuses();

	boolean deleteCachedStatus(int cachedStatusId);

	Uri getAuthorityUri();

	int getStatusType();

	String getStatusDbTableName();

	String[] PROJECTION_STATUS = new String[] { Columns.T_STATUS_K_ID, Columns.T_STATUS_K_TYPE, Columns.T_STATUS_K_TARGET_UUID, Columns.T_STATUS_K_LAST_UPDATE,
			Columns.T_STATUS_K_MAX_VALIDITY_IN_MS, Columns.T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS, Columns.T_STATUS_K_EXTRAS };

	class Columns {
		public static final String T_STATUS_K_ID = BaseColumns._ID;
		public static final String T_STATUS_K_TYPE = "type";
		public static final String T_STATUS_K_TARGET_UUID = "target";
		public static final String T_STATUS_K_EXTRAS = "extras";
		public static final String T_STATUS_K_LAST_UPDATE = "last_update";
		public static final String T_STATUS_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS = "read_from_source_at";
		// public static final String T_STATUS_K_NO_DATA = "no_data";
	}

	abstract class Filter implements MTLog.Loggable {

		private static final String TAG = StatusProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

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

		public Filter(int type, String targetUUID) {
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
			return json.getInt(JSON_TYPE);
		}

		public static String getTargetUUIDFromJSON(JSONObject json) throws JSONException {
			return json.getString(JSON_TARGET);
		}

		public static Long getCacheValidityInMsFromJSON(JSONObject json) throws JSONException {
			return json.has(JSON_CACHE_VALIDITY_IN_MS) ? json.getLong(JSON_CACHE_VALIDITY_IN_MS) : null;
		}

		public static void toJSON(Filter statusFilter, JSONObject json) throws JSONException {
			json.put(JSON_TYPE, statusFilter.getType());
			json.put(JSON_TARGET, statusFilter.getTargetUUID());
			if (statusFilter.getCacheOnlyOrNull() != null) {
				json.put(JSON_CACHE_ONLY, statusFilter.getCacheOnlyOrNull());
			}
			if (statusFilter.getInFocusOrNull() != null) {
				json.put(JSON_IN_FOCUS, statusFilter.getInFocusOrNull());
			}
			if (statusFilter.getCacheValidityInMsOrNull() != null) {
				json.put(JSON_CACHE_VALIDITY_IN_MS, statusFilter.getCacheValidityInMsOrNull());
			}
		}

		private static final String JSON_TYPE = "type";
		private static final String JSON_TARGET = "target";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";

		public static void fromJSON(Filter statusFilter, JSONObject json) throws JSONException {
			statusFilter.type = json.getInt(JSON_TYPE);
			statusFilter.targetUUID = json.getString(JSON_TARGET);
			if (json.has(JSON_CACHE_ONLY)) {
				statusFilter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
			}
			if (json.has(JSON_IN_FOCUS)) {
				statusFilter.inFocus = json.getBoolean(JSON_IN_FOCUS);
			}
			if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
				statusFilter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
			}
		}

		public abstract Filter fromJSONStringStatic(String jsonString);

		public abstract String toJSONStringStatic(Filter statusFilter);
	}
}
