package org.mtransit.android.commons.provider.news;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.provider.common.ProviderContract;
import org.mtransit.commons.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface NewsProviderContract extends ProviderContract {

	String NEWS_PATH = "news";

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

	@SuppressWarnings("WeakerAccess")
	class Filter extends ProviderContract.Filter implements MTLog.Loggable {

		private static final String LOG_TAG = NewsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@Nullable
		private List<String> articlesUUIDs; // article UUIDs
		@Nullable
		private List<String> targetsUUIDs; // POI UUIDs
		@Nullable
		private Long minCreatedAtInMs = null;

		private Filter() {
		}

		@NonNull
		public static Filter getNewEmptyFilter() {
			return new Filter();
		}

		@NonNull
		public static Filter getNewArticleUUIDFilter(@NonNull String articleUUID) {
			return getNewArticlesUUIDsFilter(Collections.singletonList(articleUUID));
		}

		@NonNull
		public static Filter getNewArticlesUUIDsFilter(@Nullable List<String> articlesUUIDs) {
			return new Filter().setArticlesUUIDs(articlesUUIDs);
		}

		@NonNull
		private Filter setArticlesUUIDs(@Nullable List<String> articlesUUIDs) {
			if (articlesUUIDs == null || articlesUUIDs.isEmpty()) {
				throw new UnsupportedOperationException("Need at least 1 article uuid!");
			}
			this.articlesUUIDs = articlesUUIDs;
			return this;
		}

		@SuppressWarnings("unused")
		@Nullable
		public List<String> getArticlesUUIDs() {
			return articlesUUIDs;
		}

		@NonNull
		public static Filter getNewTargetUUIDsFilter(@NonNull POI poi) {
			return getNewTargetsUUIDsFilter(makeTargetsUUIDs(poi));
		}

		@NonNull
		public static ArrayList<String> makeTargetsUUIDs(@NonNull POI poi) {
			final ArrayList<String> targetsUUIDs = new ArrayList<>();
			targetsUUIDs.add(poi.getAuthority());
			if (poi instanceof RouteDirectionStop) {
				targetsUUIDs.add(POI.POIUtils.makeUUID(poi.getAuthority(), ((RouteDirectionStop) poi).getRoute().getId()));
			}
			return targetsUUIDs;
		}

		@SuppressWarnings("unused")
		@NonNull
		public static Filter getNewTargetUUIDsFilter(@NonNull String targetUUID) {
			return getNewTargetsUUIDsFilter(Collections.singletonList(targetUUID));
		}

		@NonNull
		public static Filter getNewTargetsUUIDsFilter(@Nullable List<String> targetsUUIDs) {
			return new Filter().setTargetsUUIDs(targetsUUIDs);
		}

		@NonNull
		private Filter setTargetsUUIDs(List<String> targetsUUIDs) {
			if (targetsUUIDs == null || targetsUUIDs.isEmpty()) {
				throw new UnsupportedOperationException("Need at least 1 target!");
			}
			this.targetsUUIDs = targetsUUIDs;
			return this;
		}

		@SuppressWarnings("unused")
		@Nullable
		public List<String> getTargetsUUIDs() {
			return targetsUUIDs;
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
				sb.append("articleUUIDs:").append(this.articlesUUIDs).append(',');
			} else if (isTargetFilter(this)) {
				sb.append("targetsUUIDs:").append(this.targetsUUIDs).append(',');
			}
			sb.append(super.toStringParts());
			sb.append("minCreatedAtInMs:").append(this.minCreatedAtInMs);
			sb.append(']');
			return sb.toString();
		}

		@SuppressWarnings("unused")
		@NonNull
		public String toStringTargetsAndUuid() {
			final StringBuilder sb = new StringBuilder(Filter.class.getSimpleName()).append('[');
			if (isUUIDFilter(this)) {
				sb.append("articleUUIDs:").append(this.articlesUUIDs).append(',');
			} else if (isTargetFilter(this)) {
				sb.append("targetsUUIDs:").append(this.targetsUUIDs).append(',');
			}
			sb.append(']');
			return sb.toString();
		}

		public static boolean isUUIDFilter(@Nullable Filter newsFilter) {
			return newsFilter != null && CollectionUtils.getSize(newsFilter.articlesUUIDs) > 0;
		}

		public static boolean isTargetFilter(@Nullable Filter newsFilter) {
			return newsFilter != null && CollectionUtils.getSize(newsFilter.targetsUUIDs) > 0;
		}

		@NonNull
		public String getSqlSelection(@NonNull String uuidTableColumn, @NonNull String targetColumn, @NonNull String createdAtColumn) {
			StringBuilder sb = new StringBuilder();
			if (isUUIDFilter(this)) {
				sb.append(SqlUtils.getWhereInString(uuidTableColumn, this.articlesUUIDs));
			} else if (isTargetFilter(this)) {
				sb.append(SqlUtils.getWhereInString(targetColumn, this.targetsUUIDs));
			}
			if (getMinCreatedAtInMsOrNull() != null) {
				if (sb.length() > 0) {
					sb.append(SqlUtils.AND);
				}
				sb.append(SqlUtils.getWhereSuperior(createdAtColumn, getMinCreatedAtInMsOrNull()));
			}
			return sb.toString();
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

		private static final String JSON_ARTICLE_UUIDS = "uuids"; // article UUIDs
		private static final String JSON_TARGETS_UUIDS = "targets"; // POI UUIDs
		private static final String JSON_MIN_CREATED_AT_IN_MS = "minCreatedAtInMs";

		@Nullable
		public static Filter fromJSON(@NonNull JSONObject json) {
			try {
				final Filter newsFilter = new Filter();
				ProviderContract.Filter.fromJSON(newsFilter, json);
				final JSONArray jArticleUUIDs = json.optJSONArray(JSON_ARTICLE_UUIDS);
				final JSONArray jTargetsUUIDs = json.optJSONArray(JSON_TARGETS_UUIDS);
				if (jArticleUUIDs != null && jArticleUUIDs.length() > 0) {
					final ArrayList<String> articleUUIDs = new ArrayList<>();
					for (int i = 0; i < jArticleUUIDs.length(); i++) {
						articleUUIDs.add(jArticleUUIDs.getString(i));
					}
					newsFilter.setArticlesUUIDs(articleUUIDs);
				} else if (jTargetsUUIDs != null && jTargetsUUIDs.length() > 0) {
					final ArrayList<String> targetsUUIDs = new ArrayList<>();
					for (int i = 0; i < jTargetsUUIDs.length(); i++) {
						targetsUUIDs.add(jTargetsUUIDs.getString(i));
					}
					newsFilter.setTargetsUUIDs(targetsUUIDs);
				}
				if (json.has(JSON_MIN_CREATED_AT_IN_MS)) {
					newsFilter.minCreatedAtInMs = json.getLong(JSON_MIN_CREATED_AT_IN_MS);
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
			final JSONObject json = toJSON(newsFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		public static JSONObject toJSON(@NonNull Filter newsFilter) {
			try {
				final JSONObject json = new JSONObject();
				ProviderContract.Filter.toJSON(newsFilter, json);
				if (newsFilter.getMinCreatedAtInMsOrNull() != null) {
					json.put(JSON_MIN_CREATED_AT_IN_MS, newsFilter.getMinCreatedAtInMsOrNull());
				}
				if (isUUIDFilter(newsFilter) && newsFilter.articlesUUIDs != null) {
					final JSONArray jArticleUUIDs = new JSONArray();
					for (String articleUUID : newsFilter.articlesUUIDs) {
						jArticleUUIDs.put(articleUUID);
					}
					json.put(JSON_ARTICLE_UUIDS, jArticleUUIDs);
				} else if (isTargetFilter(newsFilter) && newsFilter.targetsUUIDs != null) {
					final JSONArray jTargetUUIDs = new JSONArray();
					for (String targetUUID : newsFilter.targetsUUIDs) {
						jTargetUUIDs.put(targetUUID);
					}
					json.put(JSON_TARGETS_UUIDS, jTargetUUIDs);
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while making JSON object '%s'", newsFilter);
				return null;
			}
		}
	}
}
