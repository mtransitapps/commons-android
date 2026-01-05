package org.mtransit.android.commons.provider;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SecureStringUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.provider.config.news.NewsProviderConfig;
import org.mtransit.commons.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface NewsProviderContract extends ProviderContract {

	String NEWS_PATH = "news";
	String CONFIG_PATH = "config"; // TODO move to ProviderContract once implemented everywhere

	boolean REMOVE_IMAGE_FROM_TEXT = false; // TODO later

	@NonNull
	String getAuthority();

	@NonNull
	Uri getAuthorityUri();

	@Nullable
	Cursor getNewsFromDB(@NonNull Filter newsFilter);

	@NonNull
	String getNewsDbTableName();

	@NonNull
	String[] getNewsProjection();

	@NonNull
	ArrayMap<String, String> getNewsProjectionMap();

	void cacheNews(@NonNull ArrayList<News> newNews);

	@Nullable
	ArrayList<News> getCachedNews(@NonNull Filter newsFilter);

	@Nullable
	ArrayList<News> getNewNews(@NonNull Filter newsFilter);

	@SuppressWarnings("UnusedReturnValue")
	boolean purgeUselessCachedNews();

	@SuppressWarnings("UnusedReturnValue")
	boolean deleteCachedNews(@Nullable Integer id);

	long getNewsMaxValidityInMs();

	long getNewsValidityInMs(boolean inFocusOrDefault);

	long getMinDurationBetweenNewsRefreshInMs(boolean inFocusOrDefault);

	@NonNull
	Collection<String> getNewsLanguages();

	@NonNull
	NewsProviderConfig getNewsConfig();

	interface Columns {
		String T_NEWS_K_ID = BaseColumns._ID;
		String T_NEWS_K_AUTHORITY_META = "authority";
		String T_NEWS_K_UUID = "uuid";
		String T_NEWS_K_SEVERITY = "severity";
		String T_NEWS_K_NOTEWORTHY = "noteworthy";
		String T_NEWS_K_LAST_UPDATE = "last_update";
		String T_NEWS_K_CREATED_AT = "created_at";
		String T_NEWS_K_MAX_VALIDITY_IN_MS = "max_validity";
		String T_NEWS_K_TARGET_UUID = "target";
		String T_NEWS_K_COLOR = "color";
		String T_NEWS_K_AUTHOR_NAME = "author_name";
		String T_NEWS_K_AUTHOR_USERNAME = "author_username";
		String T_NEWS_K_AUTHOR_PICTURE_URL = "author_picture_url";
		String T_NEWS_K_AUTHOR_PROFILE_URL = "author_profile_url";
		String T_NEWS_K_TEXT = "text";
		String T_NEWS_K_TEXT_HTML = "text_html";
		String T_NEWS_K_WEB_URL = "web_url";
		String T_NEWS_K_LANGUAGE = "lang";
		String T_NEWS_K_SOURCE_ID = "source_id";
		String T_NEWS_K_SOURCE_LABEL = "source_label";
		String T_NEWS_K_IMAGE_URLS_COUNT = "image_urls_count";
		String T_NEWS_K_IMAGE_URL_INDEX = "image_urls_";
	}

	String[] PROJECTION_NEWS = new String[]{ //
			Columns.T_NEWS_K_ID, //
			Columns.T_NEWS_K_AUTHORITY_META, //
			Columns.T_NEWS_K_UUID, //
			Columns.T_NEWS_K_SEVERITY, //
			Columns.T_NEWS_K_NOTEWORTHY, //
			Columns.T_NEWS_K_LAST_UPDATE, //
			Columns.T_NEWS_K_MAX_VALIDITY_IN_MS, //
			Columns.T_NEWS_K_CREATED_AT, //
			Columns.T_NEWS_K_TARGET_UUID, //
			Columns.T_NEWS_K_COLOR, //
			Columns.T_NEWS_K_AUTHOR_NAME, //
			Columns.T_NEWS_K_AUTHOR_USERNAME, //
			Columns.T_NEWS_K_AUTHOR_PICTURE_URL, //
			Columns.T_NEWS_K_AUTHOR_PROFILE_URL, //
			Columns.T_NEWS_K_TEXT, //
			Columns.T_NEWS_K_TEXT_HTML, //
			Columns.T_NEWS_K_WEB_URL, //
			Columns.T_NEWS_K_LANGUAGE, //
			Columns.T_NEWS_K_SOURCE_ID, //
			Columns.T_NEWS_K_SOURCE_LABEL, //
			Columns.T_NEWS_K_IMAGE_URLS_COUNT, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 0, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 1, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 2, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 3, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 4, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 5, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 6, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 7, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 8, //
			Columns.T_NEWS_K_IMAGE_URL_INDEX + 9, //
	};

	interface FeedConfigColumns {
		String T_NEWS_FEED_CONFIG_K_TYPE = "type";
		String T_NEWS_FEED_CONFIG_K_TARGET = "target";
		String T_NEWS_FEED_CONFIG_K_LANG = "lang";
		String T_NEWS_FEED_CONFIG_K_COLOR = "color";
		String T_NEWS_FEED_CONFIG_K_SEVERITY = "severity";
		String T_NEWS_FEED_CONFIG_K_NOTEWORTHY = "noteworthy";

		String T_NEWS_FEED_CONFIG_K_EXTRA = "extra";

	}

	String[] PROJECTION_NEWS_FEED_CONFIG = new String[]{
			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_TYPE,
			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_TARGET,
			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_LANG,
			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_COLOR,
			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_SEVERITY,
			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_NOTEWORTHY,

			FeedConfigColumns.T_NEWS_FEED_CONFIG_K_EXTRA,
	};

	@SuppressWarnings("WeakerAccess")
	class Filter implements MTLog.Loggable {

		private static final String LOG_TAG = NewsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final boolean CACHE_ONLY_DEFAULT = false;

		private static final boolean IN_FOCUS_DEFAULT = false;

		@Nullable
		private List<String> uuids;
		@Nullable
		private List<String> targets;
		@Nullable
		private Boolean cacheOnly = null;
		@Nullable
		private Long cacheValidityInMs = null;
		@Nullable
		private Boolean inFocus = null;
		@Nullable
		private Long minCreatedAtInMs = null;
		@Nullable
		private Map<String, String> providedEncryptKeysMap = null;

		private Filter() {
		}

		@NonNull
		public static Filter getNewEmptyFilter() {
			return new Filter();
		}

		@NonNull
		public static Filter getNewUUIDFilter(@NonNull String uuid) {
			return getNewUUIDsFilter(ArrayUtils.asArrayList(uuid));
		}

		@NonNull
		public static Filter getNewUUIDsFilter(@Nullable List<String> uuids) {
			return new Filter().setUUIDs(uuids);
		}

		@NonNull
		private Filter setUUIDs(@Nullable List<String> uuids) {
			if (uuids == null || uuids.isEmpty()) {
				throw new UnsupportedOperationException("Need at least 1 uuid!");
			}
			this.uuids = uuids;
			return this;
		}

		@Nullable
		public List<String> getUUIDs() {
			return uuids;
		}

		@NonNull
		public static Filter getNewTargetFilter(@NonNull POI poi) {
			ArrayList<String> targets = new ArrayList<>();
			targets.add(poi.getAuthority());
			if (poi instanceof RouteDirectionStop) {
				targets.add(POI.POIUtils.getUUID(poi.getAuthority(), ((RouteDirectionStop) poi).getRoute().getId()));
			}
			return getNewTargetsFilter(targets);
		}

		@SuppressWarnings("unused")
		@NonNull
		public static Filter getNewTargetFilter(@NonNull String targets) {
			return getNewUUIDsFilter(ArrayUtils.asArrayList(targets));
		}

		@NonNull
		public static Filter getNewTargetsFilter(@Nullable List<String> targets) {
			return new Filter().setTargets(targets);
		}

		@NonNull
		private Filter setTargets(List<String> targets) {
			if (targets == null || targets.isEmpty()) {
				throw new UnsupportedOperationException("Need at least 1 target!");
			}
			this.targets = targets;
			return this;
		}

		@Nullable
		public List<String> getTargets() {
			return targets;
		}

		@NonNull
		public Filter setMinCreatedAtInMs(long minCreatedAtInMs) {
			this.minCreatedAtInMs = minCreatedAtInMs;
			return this;
		}

		@Nullable
		public Long getMinCreatedAtInMsOrNull() {
			return this.minCreatedAtInMs;
		}

		@NonNull
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(Filter.class.getSimpleName()).append('[');
			if (isUUIDFilter(this)) {
				sb.append("uuids:").append(this.uuids).append(',');
			} else if (isTargetFilter(this)) {
				sb.append("targets:").append(this.targets).append(',');
			}
			sb.append("cacheOnly:").append(this.cacheOnly).append(',');
			sb.append("inFocus:").append(this.inFocus).append(',');
			sb.append("cacheValidityInMs:").append(this.cacheValidityInMs).append(',');
			sb.append("minCreatedAtInMs:").append(this.minCreatedAtInMs);
			sb.append(']');
			return sb.toString();
		}

		public static boolean isUUIDFilter(@Nullable Filter newsFilter) {
			return newsFilter != null && CollectionUtils.getSize(newsFilter.uuids) > 0;
		}

		public static boolean isTargetFilter(@Nullable Filter newsFilter) {
			return newsFilter != null && CollectionUtils.getSize(newsFilter.targets) > 0;
		}

		@NonNull
		public String getSqlSelection(@NonNull String uuidTableColumn, @NonNull String targetColumn, @NonNull String createdAtColumn) {
			StringBuilder sb = new StringBuilder();
			if (isUUIDFilter(this)) {
				sb.append(SqlUtils.getWhereInString(uuidTableColumn, this.uuids));
			} else if (isTargetFilter(this)) {
				sb.append(SqlUtils.getWhereInString(targetColumn, this.targets));
			}
			if (getMinCreatedAtInMsOrNull() != null) {
				if (sb.length() > 0) {
					sb.append(SqlUtils.AND);
				}
				sb.append(SqlUtils.getWhereSuperior(createdAtColumn, getMinCreatedAtInMsOrNull()));
			}
			return sb.toString();
		}

		@SuppressWarnings("unused")
		@NonNull
		public Filter setCacheOnly(@Nullable Boolean cacheOnly) {
			this.cacheOnly = cacheOnly;
			return this;
		}

		public boolean isCacheOnlyOrDefault() {
			return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
		}

		@Nullable
		public Boolean getCacheOnlyOrNull() {
			return this.cacheOnly;
		}

		@NonNull
		public Filter setInFocus(@Nullable Boolean inFocus) {
			this.inFocus = inFocus;
			return this;
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
		@NonNull
		public Filter setCacheValidityInMs(@Nullable Long cacheValidityInMs) {
			this.cacheValidityInMs = cacheValidityInMs;
			return this;
		}

		@NonNull
		public Filter setProvidedEncryptKeysMap(@Nullable Map<String, String> providedEncryptKeysMap) {
			this.providedEncryptKeysMap = providedEncryptKeysMap;
			return this;
		}

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
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_UUIDS = "uuids";
		private static final String JSON_TARGETS = "targets";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";
		private static final String JSON_MIN_CREATED_AT_IN_MS = "minCreatedAtInMs";
		private static final String JSON_PROVIDED_ENCRYPT_KEYS_MAP = "providedEncryptKeysMap";

		@Nullable
		public static Filter fromJSON(@NonNull JSONObject json) {
			try {
				Filter newsFilter = new Filter();
				JSONArray jUUIDs = json.optJSONArray(JSON_UUIDS);
				JSONArray jTargets = json.optJSONArray(JSON_TARGETS);
				if (jUUIDs != null && jUUIDs.length() > 0) {
					ArrayList<String> uuids = new ArrayList<>();
					for (int i = 0; i < jUUIDs.length(); i++) {
						uuids.add(jUUIDs.getString(i));
					}
					newsFilter.setUUIDs(uuids);
				} else if (jTargets != null && jTargets.length() > 0) {
					ArrayList<String> targets = new ArrayList<>();
					for (int i = 0; i < jTargets.length(); i++) {
						targets.add(jTargets.getString(i));
					}
					newsFilter.setTargets(targets);
				}
				if (json.has(JSON_CACHE_ONLY)) {
					newsFilter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
				}
				if (json.has(JSON_IN_FOCUS)) {
					newsFilter.inFocus = json.getBoolean(JSON_IN_FOCUS);
				}
				if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
					newsFilter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
				}
				if (json.has(JSON_MIN_CREATED_AT_IN_MS)) {
					newsFilter.minCreatedAtInMs = json.getLong(JSON_MIN_CREATED_AT_IN_MS);
				}
				if (json.has(JSON_PROVIDED_ENCRYPT_KEYS_MAP)) {
					newsFilter.providedEncryptKeysMap = JSONUtils.toMapOfStrings(json.getJSONObject(JSON_PROVIDED_ENCRYPT_KEYS_MAP));
				}
				return newsFilter;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Nullable
		public String toJSONString() {
			return toJSONString(this);
		}

		@Nullable
		public static String toJSONString(@NonNull Filter newsFilter) {
			JSONObject json = toJSON(newsFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		public static JSONObject toJSON(@NonNull Filter newsFilter) {
			try {
				JSONObject json = new JSONObject();
				if (newsFilter.getMinCreatedAtInMsOrNull() != null) {
					json.put(JSON_MIN_CREATED_AT_IN_MS, newsFilter.getMinCreatedAtInMsOrNull());
				}
				if (newsFilter.getCacheOnlyOrNull() != null) {
					json.put(JSON_CACHE_ONLY, newsFilter.getCacheOnlyOrNull());
				}
				if (newsFilter.getInFocusOrNull() != null) {
					json.put(JSON_IN_FOCUS, newsFilter.getInFocusOrNull());
				}
				if (newsFilter.getCacheValidityInMsOrNull() != null) {
					json.put(JSON_CACHE_VALIDITY_IN_MS, newsFilter.getCacheValidityInMsOrNull());
				}
				if (isUUIDFilter(newsFilter) && newsFilter.uuids != null) {
					JSONArray jUUIDs = new JSONArray();
					for (String uuid : newsFilter.uuids) {
						jUUIDs.put(uuid);
					}
					json.put(JSON_UUIDS, jUUIDs);
				} else if (isTargetFilter(newsFilter) && newsFilter.targets != null) {
					JSONArray jTargets = new JSONArray();
					for (String uuid : newsFilter.targets) {
						jTargets.put(uuid);
					}
					json.put(JSON_TARGETS, jTargets);
				}
				if (newsFilter.getProvidedEncryptKeysMap() != null) {
					json.put(JSON_PROVIDED_ENCRYPT_KEYS_MAP, JSONUtils.toJSONObject(newsFilter.getProvidedEncryptKeysMap()));
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while making JSON object '%s'", newsFilter);
				return null;
			}
		}
	}
}