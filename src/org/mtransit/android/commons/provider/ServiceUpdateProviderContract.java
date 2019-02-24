package org.mtransit.android.commons.provider;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.ServiceUpdate;

import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface ServiceUpdateProviderContract extends ProviderContract {

	String SERVICE_UPDATE_PATH = "service";

	@NonNull
	Uri getAuthorityUri();

	long getServiceUpdateMaxValidityInMs();

	long getServiceUpdateValidityInMs(boolean inFocus);

	long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus);

	void cacheServiceUpdates(@NonNull ArrayList<ServiceUpdate> newServiceUpdates);

	@Nullable
	ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull Filter serviceUpdateFilter);

	@Nullable
	ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull Filter serviceUpdateFilter);

	boolean deleteCachedServiceUpdate(Integer serviceUpdateId);

	boolean deleteCachedServiceUpdate(String targetUUID, String sourceId);

	boolean purgeUselessCachedServiceUpdates();

	String getServiceUpdateDbTableName();

	String getServiceUpdateLanguage();

	String[] PROJECTION_SERVICE_UPDATE = new String[] { Columns.T_SERVICE_UPDATE_K_ID, Columns.T_SERVICE_UPDATE_K_TARGET_UUID,
			Columns.T_SERVICE_UPDATE_K_LAST_UPDATE, Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, Columns.T_SERVICE_UPDATE_K_SEVERITY,
			Columns.T_SERVICE_UPDATE_K_TEXT, Columns.T_SERVICE_UPDATE_K_TEXT_HTML, Columns.T_SERVICE_UPDATE_K_LANGUAGE,
			Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL, Columns.T_SERVICE_UPDATE_K_SOURCE_ID };

	class Columns {
		public static final String T_SERVICE_UPDATE_K_ID = BaseColumns._ID;
		public static final String T_SERVICE_UPDATE_K_TARGET_UUID = "target";
		public static final String T_SERVICE_UPDATE_K_LAST_UPDATE = "last_update";
		public static final String T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_SERVICE_UPDATE_K_SEVERITY = "severity";
		public static final String T_SERVICE_UPDATE_K_TEXT = "text";
		public static final String T_SERVICE_UPDATE_K_TEXT_HTML = "text_html";
		public static final String T_SERVICE_UPDATE_K_LANGUAGE = "lang";
		public static final String T_SERVICE_UPDATE_K_SOURCE_LABEL = "source_label";
		public static final String T_SERVICE_UPDATE_K_SOURCE_ID = "source_id";
	}

	class Filter implements MTLog.Loggable {

		private static final String TAG = ServiceUpdateProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final boolean CACHE_ONLY_DEFAULT = false;

		private static final boolean IN_FOCUS_DEFAULT = false;

		private POI poi;
		@Nullable
		private Boolean cacheOnly = null;
		@Nullable
		private Long cacheValidityInMs = null;
		@Nullable
		private Boolean inFocus = null;

		public Filter(POI poi) {
			this.poi = poi;
		}

		@NonNull
		@Override
		public String toString() {
			return new StringBuilder(Filter.class.getSimpleName())//
					.append("cacheOnly:").append(this.cacheOnly) //
					.append(',') //
					.append("inFocus:").append(this.inFocus) //
					.append(',') //
					.append("cacheValidityInMs:").append(this.cacheValidityInMs) //
					.append(',') //
					.append("poi:").append(this.poi) //
					.toString();
		}

		public POI getPoi() {
			return poi;
		}

		public void setCacheOnly(@Nullable Boolean cacheOnly) {
			this.cacheOnly = cacheOnly;
		}

		public boolean isCacheOnlyOrDefault() {
			return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
		}

		public Boolean getCacheOnlyOrNull() {
			return this.cacheOnly;
		}

		public void setInFocus(@Nullable Boolean inFocus) {
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
			return this.cacheValidityInMs != null && this.cacheValidityInMs > 0;
		}

		public void setCacheValidityInMs(@Nullable Long cacheValidityInMs) {
			this.cacheValidityInMs = cacheValidityInMs;
		}

		@Nullable
		public static Filter fromJSONString(@Nullable String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_POI = "poi";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";

		@Nullable
		public static Filter fromJSON(@NonNull JSONObject json) {
			try {
				POI poi = DefaultPOI.fromJSONStatic(json.getJSONObject(JSON_POI));
				Filter serviceUpdateFilter = new Filter(poi);
				if (json.has(JSON_CACHE_ONLY)) {
					serviceUpdateFilter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
				}
				if (json.has(JSON_IN_FOCUS)) {
					serviceUpdateFilter.inFocus = json.getBoolean(JSON_IN_FOCUS);
				}
				if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
					serviceUpdateFilter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
				}
				return serviceUpdateFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Nullable
		public String toJSONString() {
			return toJSONString(this);
		}

		@Nullable
		public static String toJSONString(Filter serviceUpdateFilter) {
			JSONObject json = toJSON(serviceUpdateFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		public static JSONObject toJSON(Filter serviceUpdateFilter) {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_POI, serviceUpdateFilter.poi.toJSON());
				if (serviceUpdateFilter.getCacheOnlyOrNull() != null) {
					json.put(JSON_CACHE_ONLY, serviceUpdateFilter.getCacheOnlyOrNull());
				}
				if (serviceUpdateFilter.getInFocusOrNull() != null) {
					json.put(JSON_IN_FOCUS, serviceUpdateFilter.getInFocusOrNull());
				}
				if (serviceUpdateFilter.getCacheValidityInMsOrNull() != null) {
					json.put(JSON_CACHE_VALIDITY_IN_MS, serviceUpdateFilter.getCacheValidityInMsOrNull());
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", serviceUpdateFilter);
				return null;
			}
		}
	}
}
