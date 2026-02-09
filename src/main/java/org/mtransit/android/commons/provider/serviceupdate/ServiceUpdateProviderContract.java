package org.mtransit.android.commons.provider.serviceupdate;

import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SecureStringUtils;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirection;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Targetable;
import org.mtransit.android.commons.provider.common.ProviderContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ServiceUpdateProviderContract extends ProviderContract {

	String SERVICE_UPDATE_PATH = "service";

	@NonNull
	String getAuthority();

	@NonNull
	Uri getAuthorityUri();

	long getServiceUpdateMaxValidityInMs();

	long getServiceUpdateValidityInMs(boolean inFocus);

	long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus);

	void cacheServiceUpdates(@NonNull List<ServiceUpdate> newServiceUpdates);

	@Nullable
	List<ServiceUpdate> getCachedServiceUpdates(@NonNull Filter serviceUpdateFilter);

	@Nullable
	List<ServiceUpdate> getNewServiceUpdates(@NonNull Filter serviceUpdateFilter);

	@SuppressWarnings("UnusedReturnValue")
	boolean deleteCachedServiceUpdate(@NonNull Integer serviceUpdateId);

	@SuppressWarnings("UnusedReturnValue")
	boolean deleteCachedServiceUpdate(@NonNull String targetUUID, @NonNull String sourceId);

	@SuppressWarnings("UnusedReturnValue")
	boolean purgeUselessCachedServiceUpdates();

	@NonNull
	String getServiceUpdateDbTableName();

	@NonNull
	String getServiceUpdateLanguage();

	String[] PROJECTION_SERVICE_UPDATE = new String[]{
			Columns.T_SERVICE_UPDATE_K_ID,
			Columns.T_SERVICE_UPDATE_K_TARGET_UUID,
			Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID,
			Columns.T_SERVICE_UPDATE_K_LAST_UPDATE,
			Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS,
			Columns.T_SERVICE_UPDATE_K_SEVERITY,
			Columns.T_SERVICE_UPDATE_K_TEXT,
			Columns.T_SERVICE_UPDATE_K_TEXT_HTML,
			Columns.T_SERVICE_UPDATE_K_LANGUAGE,
			Columns.T_SERVICE_UPDATE_K_ORIGINAL_ID,
			Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL,
			Columns.T_SERVICE_UPDATE_K_SOURCE_ID
	};

	class Columns {
		public static final String T_SERVICE_UPDATE_K_ID = BaseColumns._ID;
		public static final String T_SERVICE_UPDATE_K_TARGET_UUID = "target";
		public static final String T_SERVICE_UPDATE_K_TARGET_TRIP_ID = "trip_id";
		public static final String T_SERVICE_UPDATE_K_LAST_UPDATE = "last_update";
		public static final String T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_SERVICE_UPDATE_K_SEVERITY = "severity";
		public static final String T_SERVICE_UPDATE_K_TEXT = "text";
		public static final String T_SERVICE_UPDATE_K_TEXT_HTML = "text_html";
		public static final String T_SERVICE_UPDATE_K_LANGUAGE = "lang";
		public static final String T_SERVICE_UPDATE_K_SOURCE_LABEL = "source_label";
		public static final String T_SERVICE_UPDATE_K_ORIGINAL_ID = "original_id";
		public static final String T_SERVICE_UPDATE_K_SOURCE_ID = "source_id";
	}

	@SuppressWarnings("WeakerAccess")
	class Filter implements MTLog.Loggable {

		private static final String TAG = ServiceUpdateProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final boolean CACHE_ONLY_DEFAULT = false;

		private static final boolean IN_FOCUS_DEFAULT = false;

		@Nullable
		private final POI poi; // RouteDirectionStop or DefaultPOI
		@Nullable
		private final String authority;
		@Nullable
		private final Route route;
		@Nullable
		private final RouteDirection routeDirection;
		@Nullable
		private final Collection<String> tripIds; // original // GTFS // cleaned

		@Nullable
		private Boolean cacheOnly = null;
		@Nullable
		private Long cacheValidityInMs = null;
		@Nullable
		private Boolean inFocus = null;
		@Nullable
		private Map<String, String> providedEncryptKeysMap = null;

		public Filter(@NonNull POI poi, @Nullable Collection<String> tripIds) {
			this.poi = poi;
			this.authority = poi.getAuthority();
			this.route = null;
			this.routeDirection = null;
			this.tripIds = tripIds;
		}

		public Filter(@NonNull String authority, @NonNull Route route, @Nullable Collection<String> tripIds) {
			this.authority = authority;
			this.route = route;
			this.routeDirection = null;
			this.poi = null;
			this.tripIds = tripIds;
		}

		public Filter(@NonNull String authority, @NonNull RouteDirection routeDirection, @Nullable Collection<String> tripIds) {
			this.authority = authority;
			this.routeDirection = routeDirection;
			this.route = null;
			this.poi = null;
			this.tripIds = tripIds;
		}

		@NonNull
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(Filter.class.getSimpleName())
					.append("cacheOnly:").append(this.cacheOnly).append(',')
					.append("inFocus:").append(this.inFocus).append(',')
					.append("cacheValidityInMs:").append(this.cacheValidityInMs).append(',');
			if (this.poi != null) {
				sb.append("poi:").append(this.poi).append(',');
			}
			if (this.authority != null) {
				sb.append("authority:").append(this.authority).append(',');
			}
			if (this.route != null) {
				sb.append("route:").append(this.route).append(',');
			}
			if (this.routeDirection != null) {
				sb.append("routeDirection:").append(this.routeDirection).append(',');
			}
			if (this.tripIds != null) {
				sb.append("tripIds:").append(this.tripIds.size()).append(',');
			}
			return sb.toString();
		}

		@Nullable
		public Targetable getTarget() {
			if (this.poi != null) {
				return this.poi;
			}
			if (this.route != null) {
				return this.route;
			}
			//noinspection RedundantIfStatement
			if (this.routeDirection != null) {
				return this.routeDirection;
			}
			return null;
		}

		@Nullable
		public String getTargetUUID() {
			final Targetable target = getTarget();
			return target == null ? null : target.getUUID();
		}

		@Nullable
		public String getTargetAuthority() {
			if (this.poi != null) {
				return this.poi.getAuthority();
			}
			if (this.route != null) {
				return this.route.getAuthority();
			}
			if (this.routeDirection != null) {
				return this.routeDirection.getAuthority();
			}
			return null;
		}

		@Nullable
		public Long getRouteId() {
			if (this.poi != null && this.poi instanceof RouteDirectionStop) {
				return ((RouteDirectionStop) this.poi).getRoute().getId();
			}
			if (this.route != null) {
				return this.route.getId();
			}
			if (this.routeDirection != null) {
				return this.routeDirection.getRoute().getId();
			}
			return null;
		}

		@Nullable
		public Long getDirectionId() {
			if (this.poi != null && this.poi instanceof RouteDirectionStop) {
				return ((RouteDirectionStop) this.poi).getDirection().getId();
			}
			if (this.routeDirection != null) {
				return this.routeDirection.getDirection().getId();
			}
			return null;
		}

		@Nullable
		public POI getPoi() {
			return poi;
		}

		@Nullable
		public Route getRoute() {
			return route;
		}

		@Nullable
		public RouteDirection getRouteDirection() {
			return routeDirection;
		}

		@SuppressWarnings("unused")
		public void setCacheOnly(@Nullable Boolean cacheOnly) {
			this.cacheOnly = cacheOnly;
		}

		public boolean isCacheOnlyOrDefault() {
			return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
		}

		@Nullable
		public Boolean getCacheOnlyOrNull() {
			return this.cacheOnly;
		}

		public void setInFocus(@Nullable Boolean inFocus) {
			this.inFocus = inFocus;
		}

		public boolean isInFocusOrDefault() {
			return this.inFocus == null ? IN_FOCUS_DEFAULT : this.inFocus;
		}

		@Nullable
		public Boolean getInFocusOrNull() {
			return this.inFocus;
		}

		@Nullable
		public Long getCacheValidityInMsOrNull() {
			return this.cacheValidityInMs;
		}

		@SuppressWarnings("unused")
		public boolean hasCacheValidityInMs() {
			return this.cacheValidityInMs != null && this.cacheValidityInMs > 0;
		}

		@SuppressWarnings("unused")
		public void setCacheValidityInMs(@Nullable Long cacheValidityInMs) {
			this.cacheValidityInMs = cacheValidityInMs;
		}

		@NonNull
		public Filter setProvidedEncryptKeysMap(@Nullable Map<String, String> providedEncryptKeysMap) {
			this.providedEncryptKeysMap = providedEncryptKeysMap;
			return this;
		}

		@SuppressWarnings("unused")
		public boolean hasProvidedEncryptKeysMap() {
			return this.providedEncryptKeysMap != null && !this.providedEncryptKeysMap.isEmpty();
		}

		@Nullable
		public Map<String, String> getProvidedEncryptKeysMap() {
			return this.providedEncryptKeysMap;
		}

		@Nullable
		public String getProvidedEncryptKey(@NonNull String key) {
			if (this.providedEncryptKeysMap == null) {
				return null;
			}
			final String value = this.providedEncryptKeysMap.get(key);
			if (value == null || value.trim().isEmpty()) {
				return null;
			}
			return value;
		}

		@NonNull
		public Filter appendProvidedKeys(@Nullable Map<String, String> keysMap) {
			final Map<String, String> providedEncryptKeysMap = new HashMap<>();
			if (keysMap != null) {
				for (Map.Entry<String, String> entry : keysMap.entrySet()) {
					providedEncryptKeysMap.put(entry.getKey(), SecureStringUtils.enc(entry.getValue()));
				}
			}
			return setProvidedEncryptKeysMap(providedEncryptKeysMap);
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
		private static final String JSON_ROUTE = "route";
		private static final String JSON_ROUTE_DIRECTION = "routeDirection";
		private static final String JSON_AUTHORITY = "authority";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";
		private static final String JSON_PROVIDED_ENCRYPT_KEYS_MAP = "providedEncryptKeysMap";
		private static final String JSON_TRIP_IDS = "tripIds";

		@Nullable
		public static Filter fromJSON(@NonNull JSONObject json) {
			try {
				final POI poi = json.has(JSON_POI) ? DefaultPOI.fromJSONStatic(json.getJSONObject(JSON_POI)) : null;
				final String authority = JSONUtils.optString(json, JSON_AUTHORITY);
				final Route route = json.has(JSON_ROUTE) && authority != null ?
						Route.fromJSON(json.getJSONObject(JSON_ROUTE), authority) : null;
				final RouteDirection routeDirection = json.has(JSON_ROUTE_DIRECTION) && authority != null ?
						RouteDirection.fromJSON(json.getJSONObject(JSON_ROUTE_DIRECTION), authority) : null;
				List<String> tripIds = null;
				final JSONArray jTripIds = json.optJSONArray(JSON_TRIP_IDS);
				if (jTripIds != null) {
					tripIds = new ArrayList<>();
					for (int i = 0; i < jTripIds.length(); i++) {
						tripIds.add(jTripIds.getString(i));
					}
				}
				final Filter serviceUpdateFilter;
				if (poi != null) {
					serviceUpdateFilter = new Filter(poi, tripIds);
				} else if (route != null) {
					serviceUpdateFilter = new Filter(authority, route, tripIds);
				} else if (routeDirection != null) {
					serviceUpdateFilter = new Filter(authority, routeDirection, tripIds);
				} else {
					return null; // WTF?
				}
				serviceUpdateFilter.cacheOnly = JSONUtils.optBoolean(json, JSON_CACHE_ONLY);
				serviceUpdateFilter.inFocus = JSONUtils.optBoolean(json, JSON_IN_FOCUS);
				if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
					serviceUpdateFilter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
				}
				if (json.has(JSON_PROVIDED_ENCRYPT_KEYS_MAP)) {
					serviceUpdateFilter.providedEncryptKeysMap = JSONUtils.toMapOfStrings(json.getJSONObject(JSON_PROVIDED_ENCRYPT_KEYS_MAP));
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
		public static String toJSONString(@NonNull Filter serviceUpdateFilter) {
			JSONObject json = toJSON(serviceUpdateFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		public static JSONObject toJSON(@NonNull Filter serviceUpdateFilter) {
			try {
				JSONObject json = new JSONObject();
				if (serviceUpdateFilter.poi != null) {
					json.put(JSON_POI, serviceUpdateFilter.poi.toJSON());
				}
				if (serviceUpdateFilter.route != null) {
					json.put(JSON_ROUTE, Route.toJSON(serviceUpdateFilter.route));
				}
				if (serviceUpdateFilter.routeDirection != null) {
					json.put(JSON_ROUTE_DIRECTION, RouteDirection.toJSON(serviceUpdateFilter.routeDirection));
				}
				if (serviceUpdateFilter.authority != null) {
					json.put(JSON_AUTHORITY, serviceUpdateFilter.authority);
				}
				if (serviceUpdateFilter.tripIds != null) {
					json.put(JSON_TRIP_IDS, new JSONArray(serviceUpdateFilter.tripIds));
				}
				if (serviceUpdateFilter.getCacheOnlyOrNull() != null) {
					json.put(JSON_CACHE_ONLY, serviceUpdateFilter.getCacheOnlyOrNull());
				}
				if (serviceUpdateFilter.getInFocusOrNull() != null) {
					json.put(JSON_IN_FOCUS, serviceUpdateFilter.getInFocusOrNull());
				}
				if (serviceUpdateFilter.getCacheValidityInMsOrNull() != null) {
					json.put(JSON_CACHE_VALIDITY_IN_MS, serviceUpdateFilter.getCacheValidityInMsOrNull());
				}
				if (serviceUpdateFilter.getProvidedEncryptKeysMap() != null) {
					json.put(JSON_PROVIDED_ENCRYPT_KEYS_MAP, JSONUtils.toJSONObject(serviceUpdateFilter.getProvidedEncryptKeysMap()));
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while making JSON object '%s'", serviceUpdateFilter);
				return null;
			}
		}
	}
}
