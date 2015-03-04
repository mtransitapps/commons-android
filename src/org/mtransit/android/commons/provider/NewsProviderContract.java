package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.News;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public interface NewsProviderContract extends ProviderContract {

	public static final String NEWS_PATH = "news";

	String getAuthority();

	Uri getAuthorityUri();

	Cursor getNewsFromDB(Filter newsFilter);

	String getNewsDbTableName();

	String[] getNewsProjection();

	HashMap<String, String> getNewsProjectionMap();

	void cacheNews(ArrayList<News> newNews);

	ArrayList<News> getCachedNews(Filter newsFilter);

	ArrayList<News> getNewNews(Filter newsFilter);

	boolean purgeUselessCachedNews();

	boolean deleteCachedNews(Integer id);

	long getNewsMaxValidityInMs();

	long getNewsValidityInMs(boolean inFocusOrDefault);

	long getMinDurationBetweenNewsRefreshInMs(boolean inFocusOrDefault);

	public static class Columns {
		public static final String T_NEWS_K_ID = BaseColumns._ID;
		public static final String T_NEWS_K_AUTHORITY_META = "authority";
		public static final String T_NEWS_K_UUID = "uuid";
		public static final String T_NEWS_K_LAST_UPDATE = "last_update";
		public static final String T_NEWS_K_CREATED_AT = "created_at";
		public static final String T_NEWS_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_NEWS_K_TARGET_UUID = "target";
		public static final String T_NEWS_K_COLOR = "color";
		public static final String T_NEWS_K_AUTHOR_NAME = "author_name";
		public static final String T_NEWS_K_AUTHOR_USERNAME = "author_username";
		public static final String T_NEWS_K_AUTHOR_PICTURE_URL = "author_picture_url";
		public static final String T_NEWS_K_AUTHOR_PROFILE_URL = "author_profile_url";
		public static final String T_NEWS_K_TEXT = "text";
		public static final String T_NEWS_K_TEXT_HTML = "text_html";
		public static final String T_NEWS_K_WEB_URL = "web_url";
		public static final String T_NEWS_K_LANGUAGE = "lang";
		public static final String T_NEWS_K_SOURCE_ID = "source_id";
		public static final String T_NEWS_K_SOURCE_LABEL = "source_label";
	}

	public static final String[] PROJECTION_NEWS = new String[] { //
	Columns.T_NEWS_K_ID, //
			Columns.T_NEWS_K_AUTHORITY_META, //
			Columns.T_NEWS_K_UUID, //
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
			Columns.T_NEWS_K_SOURCE_LABEL //
	};

	public static class Filter implements MTLog.Loggable {

		private static final String TAG = NewsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final boolean CACHE_ONLY_DEFAULT = false;

		private static final boolean IN_FOCUS_DEFAULT = false;

		private Collection<String> uuids;
		private Boolean cacheOnly = null;
		private Long cacheValidityInMs = null;
		private Boolean inFocus = null;

		public Filter() {
		}

		public Filter(Collection<String> uuids) {
			if (uuids == null || uuids.size() == 0) {
				throw new UnsupportedOperationException("Need at least 1 uuid!");
			}
			this.uuids = uuids;
		}

		@Override
		public String toString() {
			return new StringBuilder(Filter.class.getSimpleName()).append('{') //
					.append("uuids:").append(this.uuids) //
					.append(',') //
					.append("cacheOnly:").append(this.cacheOnly) //
					.append(',') //
					.append("inFocus:").append(this.inFocus) //
					.append(',') //
					.append("cacheValidityInMs:").append(this.cacheValidityInMs) //
					.append('}').toString();
		}

		public static boolean isUUIDFilter(Filter newsFilter) {
			return newsFilter != null && newsFilter.uuids != null && newsFilter.uuids.size() > 0;
		}

		public String getSqlSelection(String uuidTableColumn) {
			if (isUUIDFilter(this)) {
				StringBuilder qb = new StringBuilder();
				for (String uid : this.uuids) {
					if (qb.length() == 0) {
						qb.append(uuidTableColumn).append(" IN (");
					} else {
						qb.append(',');
					}
					qb.append('\'').append(uid).append('\'');
				}
				qb.append(')');
				return qb.toString();
			} else {
				return null;
			}
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
			return this.cacheValidityInMs != null && this.cacheValidityInMs > 0;
		}

		public void setCacheValidityInMs(Long cacheValidityInMs) {
			this.cacheValidityInMs = cacheValidityInMs;
		}

		public static Filter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_UUIDS = "uuids";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";

		public static Filter fromJSON(JSONObject json) {
			try {
				Filter newsFilter;
				JSONArray jUUIDs = json.optJSONArray(JSON_UUIDS);
				if (jUUIDs != null && jUUIDs.length() > 0) {
					HashSet<String> uuids = new HashSet<String>();
					for (int i = 0; i < jUUIDs.length(); i++) {
						uuids.add(jUUIDs.getString(i));
					}
					newsFilter = new Filter(uuids);
				} else {
					newsFilter = new Filter();
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
				return newsFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		public String toJSONString() {
			return toJSONString(this);
		}

		public static String toJSONString(Filter newsFilter) {
			try {
				JSONObject json = toJSON(newsFilter);
				return json == null ? null : json.toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", newsFilter);
				return null;
			}
		}

		public static JSONObject toJSON(Filter newsFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				if (newsFilter.getCacheOnlyOrNull() != null) {
					json.put(JSON_CACHE_ONLY, newsFilter.getCacheOnlyOrNull());
				}
				if (newsFilter.getInFocusOrNull() != null) {
					json.put(JSON_IN_FOCUS, newsFilter.getInFocusOrNull());
				}
				if (newsFilter.getCacheValidityInMsOrNull() != null) {
					json.put(JSON_CACHE_VALIDITY_IN_MS, newsFilter.getCacheValidityInMsOrNull());
				}
				if (isUUIDFilter(newsFilter)) {
					JSONArray jUUIDs = new JSONArray();
					for (String uuid : newsFilter.uuids) {
						jUUIDs.put(uuid);
					}
					json.put(JSON_UUIDS, jUUIDs);
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", newsFilter);
				return null;
			}
		}
	}
}
