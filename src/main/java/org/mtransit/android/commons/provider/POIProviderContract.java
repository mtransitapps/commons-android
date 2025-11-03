package org.mtransit.android.commons.provider;

import android.app.SearchManager;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

public interface POIProviderContract extends ProviderContract {

	String POI_PATH = "poi";

	String POI_FILTER_EXTRA_AVOID_LOADING = "avoidLoading";

	String POI_FILTER_EXTRA_SORT_ORDER = "sortOrder";

	long getPOIMaxValidityInMs();

	long getPOIValidityInMs();

	@Nullable
	Cursor getPOI(@Nullable Filter poiFilter);

	@Nullable
	Cursor getPOIFromDB(@Nullable Filter poiFilter);

	@NonNull
	ArrayMap<String, String> getPOIProjectionMap();

	@NonNull
	String[] getPOIProjection();

	@NonNull
	String getPOITable();

	@Nullable
	Cursor getSearchSuggest(@Nullable String query);

	@Nullable
	String getSearchSuggestTable();

	@Nullable
	ArrayMap<String, String> getSearchSuggestProjectionMap();

	@SuppressWarnings("unused")
	String[] PROJECTION_POI_ALL_COLUMNS = null; // null = return all columns

	String[] PROJECTION_POI = FeatureFlags.F_ACCESSIBILITY_PRODUCER ?
			new String[]{
					Columns.T_POI_K_UUID_META,
					Columns.T_POI_K_DST_ID_META,
					Columns.T_POI_K_ID,
					Columns.T_POI_K_NAME,
					Columns.T_POI_K_LAT,
					Columns.T_POI_K_LNG,
					Columns.T_POI_K_ACCESSIBLE,
					Columns.T_POI_K_TYPE,
					Columns.T_POI_K_STATUS_TYPE,
					Columns.T_POI_K_ACTIONS_TYPE,
			}
			: new String[]{
			Columns.T_POI_K_UUID_META,
			Columns.T_POI_K_DST_ID_META,
			Columns.T_POI_K_ID,
			Columns.T_POI_K_NAME,
			Columns.T_POI_K_LAT,
			Columns.T_POI_K_LNG,
			Columns.T_POI_K_TYPE,
			Columns.T_POI_K_STATUS_TYPE,
			Columns.T_POI_K_ACTIONS_TYPE,
	};

	String[] PROJECTION_POI_SEARCH_SUGGEST = new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1};

	class Columns {
		public static final String T_POI_K_ID = BaseColumns._ID;
		@SuppressWarnings("WeakerAccess")
		public static final String T_POI_K_UUID_META = "uuid";
		public static final String T_POI_K_DST_ID_META = "dst";
		public static final String T_POI_K_NAME = "name";
		public static final String T_POI_K_LAT = "lat";
		public static final String T_POI_K_LNG = "lng";
		public static final String T_POI_K_ACCESSIBLE = "a11y";
		public static final String T_POI_K_TYPE = "type";
		public static final String T_POI_K_STATUS_TYPE = "statustype";
		public static final String T_POI_K_ACTIONS_TYPE = "actionstype";
		//
		public static final String T_POI_K_SCORE_META_OPT = "score"; // optional

		@SuppressWarnings("unused")
		@NonNull
		public static String getFkColumnName(@NonNull String key) {
			return "fk" + "_" + key;
		}
	}

	@SuppressWarnings({"WeakerAccess", "unused"})
	class Filter implements MTLog.Loggable {

		private static final String LOG_TAG = POIProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@Nullable
		private Double lat = null;
		@Nullable
		private Double lng = null;
		@Nullable
		private Double aroundDiff = null;

		@Nullable
		private Double minLat = null;
		@Nullable
		private Double maxLat = null;
		@Nullable
		private Double minLng = null;
		@Nullable
		private Double maxLng = null;
		@Nullable
		private Double optLoadedMinLat = null;
		@Nullable
		private Double optLoadedMaxLat = null;
		@Nullable
		private Double optLoadedMinLng = null;
		@Nullable
		private Double optLoadedMaxLng = null;

		@Nullable
		private Collection<String> uuids = null;

		@NonNull
		private final SimpleArrayMap<String, Object> extras = new SimpleArrayMap<>();

		@Nullable
		private String sqlSelection = null;

		@Nullable
		private String[] searchKeywords = null;

		@SuppressWarnings("DeprecatedIsStillUsed")
		@Deprecated // filtered in the main app
		@Nullable
		private Boolean excludeBookingRequired = null;

		private Filter() {
		}

		@NonNull
		public static Filter getNewEmptyFilter() {
			return getNewSqlSelectionFilter(StringUtils.EMPTY);
		}

		@NonNull
		public static Filter getNewSqlSelectionFilter(@NonNull String sqlSelection) {
			return new Filter().setSqlSelection(sqlSelection);
		}

		@NonNull
		private Filter setSqlSelection(@NonNull String sqlSelection) {
			//noinspection ConstantConditions
			if (sqlSelection == null) {
				throw new UnsupportedOperationException("Need an SQL selection!");
			}
			this.sqlSelection = sqlSelection;
			return this;
		}

		@NonNull
		public static Filter getNewSearchFilter(@NonNull String searchKeyword) {
			return getNewSearchFilter(new String[]{searchKeyword});
		}

		@NonNull
		public static Filter getNewSearchFilter(@NonNull String[] searchKeywords) {
			return new Filter().setSearchKeywords(searchKeywords);
		}

		@NonNull
		private Filter setSearchKeywords(@NonNull String[] searchKeywords) {
			if (ArrayUtils.getSize(searchKeywords) == 0) {
				throw new UnsupportedOperationException("Need at least 1 search keyword!");
			}
			this.searchKeywords = searchKeywords;
			return this;
		}

		@NonNull
		public static Filter getNewUUIDFilter(@NonNull String uuid) {
			return getNewUUIDsFilter(Collections.singletonList(uuid));
		}

		@NonNull
		public static Filter getNewUUIDsFilter(@NonNull Collection<String> uuids) {
			return new Filter().setUUIDs(uuids);
		}

		@NonNull
		private Filter setUUIDs(@NonNull Collection<String> uuids) {
			if (CollectionUtils.getSize(uuids) == 0) {
				throw new UnsupportedOperationException("Need at least 1 uuid!");
			}
			this.uuids = uuids;
			return this;
		}

		@NonNull
		public static Filter getNewAroundFilter(double lat, double lng, double aroundDiff) {
			return new Filter().setAround(lat, lng, aroundDiff);
		}

		@NonNull
		private Filter setAround(double lat, double lng, double aroundDiff) {
			this.lat = lat;
			this.lng = lng;
			this.aroundDiff = aroundDiff;
			return this;
		}

		@NonNull
		public static Filter getNewAreaFilter(double minLat, double maxLat, double minLng, double maxLng,
											  @Nullable Double optLoadedMinLat, @Nullable Double optLoadedMaxLat, @Nullable Double optLoadedMinLng, @Nullable Double optLoadedMaxLng) {
			return new Filter().setArea(
					minLat, maxLat, minLng, maxLng,
					optLoadedMinLat, optLoadedMaxLat, optLoadedMinLng, optLoadedMaxLng
			);
		}

		@NonNull
		private Filter setArea(double minLat, double maxLat, double minLng, double maxLng,
							   @Nullable Double optLoadedMinLat, @Nullable Double optLoadedMaxLat, @Nullable Double optLoadedMinLng, @Nullable Double optLoadedMaxLng) {
			this.minLat = minLat;
			this.maxLat = maxLat;
			this.minLng = minLng;
			this.maxLng = maxLng;
			this.optLoadedMinLat = optLoadedMinLat;
			this.optLoadedMaxLat = optLoadedMaxLat;
			this.optLoadedMinLng = optLoadedMinLng;
			this.optLoadedMaxLng = optLoadedMaxLng;
			return this;
		}

		@SuppressWarnings("DeprecatedIsStillUsed")
		@Deprecated // filtered in the main app
		@Nullable
		public Filter setExcludeBookingRequired(@Nullable Boolean excludeBookingRequired) {
			this.excludeBookingRequired = excludeBookingRequired;
			return this;
		}

		@NonNull
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(Filter.class.getSimpleName()).append('[');
			if (isAreaFilter(this)) {
				sb.append("lat:").append(this.lat).append(',');
				sb.append("lng:").append(this.lng).append(',');
				sb.append("aroundDiff:").append(this.aroundDiff).append(',');
			} else if (isAreasFilter(this)) {
				sb.append("minLat:").append(this.minLat).append(',');
				sb.append("maxLat:").append(this.maxLat).append(',');
				sb.append("minLng:").append(this.minLng).append(',');
				sb.append("maxLng:").append(this.maxLng).append(',');
				sb.append("optLoadedMinLat:").append(this.optLoadedMinLat).append(',');
				sb.append("optLoadedMaxLat:").append(this.optLoadedMaxLat).append(',');
				sb.append("optLoadedMinLng").append(this.optLoadedMinLng).append(',');
				sb.append("optLoadedMaxLng:").append(this.optLoadedMaxLng).append(',');
			} else if (isUUIDFilter(this)) {
				sb.append("uuids:").append(this.uuids).append(',');
			} else if (isSearchKeywords(this)) {
				sb.append("searchKeywords:").append((this.searchKeywords == null ? null : Arrays.asList(this.searchKeywords))).append(',');
			} else if (isSQLSelection(this)) {
				sb.append("sqlSelection:").append(this.sqlSelection).append(',');
			}
			//noinspection deprecation // filtered in the main app
			sb.append("exclBookingReq:").append(this.excludeBookingRequired).append(',');
			sb.append("extras:").append(this.extras).append(',');
			sb.append(']');
			return sb.toString();
		}

		public static boolean isUUIDFilter(@Nullable Filter poiFilter) {
			return poiFilter != null && CollectionUtils.getSize(poiFilter.uuids) > 0;
		}

		public static boolean isAreaFilter(@Nullable Filter poiFilter) {
			return poiFilter != null && poiFilter.lat != null && poiFilter.lng != null && poiFilter.aroundDiff != null;
		}

		public static boolean isAreasFilter(@Nullable Filter poiFilter) {
			return poiFilter != null && poiFilter.minLat != null && poiFilter.maxLat != null && poiFilter.minLng != null && poiFilter.maxLng != null;
		}

		public static boolean isSearchKeywords(@Nullable Filter poiFilter) {
			return poiFilter != null && ArrayUtils.getSize(poiFilter.searchKeywords) > 0;
		}

		public static boolean isSQLSelection(@Nullable Filter poiFilter) {
			return poiFilter != null && poiFilter.sqlSelection != null;
		}

		public void addExtra(@NonNull String key, @NonNull Object value) {
			// TODO CRASH SimpleArrayMap ClassCastException: String cannot be cast to Object[]
			this.extras.put(key, value);
		}

		@Nullable
		public Double getLat() {
			return lat;
		}

		@Nullable
		public Double getLng() {
			return lng;
		}

		@Nullable
		public Double getAroundDiff() {
			return aroundDiff;
		}

		@Nullable
		public String getSqlSelection(@NonNull String uuidTableColumn, @NonNull String latTableColumn,
									  @NonNull String lngTableColumn, @NonNull String[] searchableLikeColumns,
									  @NonNull String[] searchableEqualColumns) {
			if (isAreaFilter(this) && this.lat != null && this.lng != null && this.aroundDiff != null) {
				return LocationUtils.genAroundWhere(this.lat, this.lng, latTableColumn, lngTableColumn, this.aroundDiff);
			} else if (isAreasFilter(this)) {
				StringBuilder sb = new StringBuilder();
				if (this.minLat != null && this.maxLat != null && this.minLng != null && this.maxLng != null) {
					sb.append(SqlUtils.P1);
					sb.append(SqlUtils.getBetween(latTableColumn, this.minLat, this.maxLat));
					sb.append(SqlUtils.AND);
					sb.append(SqlUtils.getBetween(lngTableColumn, this.minLng, this.maxLng));
					sb.append(SqlUtils.P2);
				}
				if (this.optLoadedMinLat != null && this.optLoadedMaxLat != null && this.optLoadedMinLng != null && this.optLoadedMaxLng != null) {
					if (sb.length() > 0) {
						sb.append(SqlUtils.AND);
					}
					sb.append(SqlUtils.NOT);
					sb.append(SqlUtils.P1);
					sb.append(SqlUtils.getBetween(latTableColumn, this.optLoadedMinLat, this.optLoadedMaxLat));
					sb.append(SqlUtils.AND);
					sb.append(SqlUtils.getBetween(lngTableColumn, this.optLoadedMinLng, this.optLoadedMaxLng));
					sb.append(SqlUtils.P2);
				}
				return sb.toString();
			} else if (isUUIDFilter(this)) {
				return SqlUtils.getWhereInString(uuidTableColumn, this.uuids);
			} else if (isSearchKeywords(this) && searchKeywords != null) {
				return getSearchSelection(this.searchKeywords, searchableLikeColumns, searchableEqualColumns);
			} else if (isSQLSelection(this)) {
				return this.sqlSelection;
			} else {
				throw new UnsupportedOperationException("SQL selection impossible!");
			}
		}

		@Nullable
		public String[] getSearchKeywords() {
			return searchKeywords;
		}

		@NonNull
		public static String getSearchSelection(@NonNull String[] searchKeywords, @Nullable String[] searchableLikeColumns, @Nullable String[] searchableEqualColumns) {
			if (ArrayUtils.getSize(searchKeywords) == 0
					|| TextUtils.isEmpty(searchKeywords[0])) {
				throw new UnsupportedOperationException(
						String.format("SQL search selection needs at least 1 keyword (%s)!", ArrayUtils.getSize(searchKeywords)));
			}
			if (ArrayUtils.getSize(searchableLikeColumns) == 0
					&& ArrayUtils.getSize(searchableEqualColumns) == 0) {
				throw new UnsupportedOperationException(String.format("SQL search selection needs at least 1 searchable columns (%s|%s)!",
						ArrayUtils.getSize(searchableLikeColumns), ArrayUtils.getSize(searchableEqualColumns)));
			}
			StringBuilder selectionSb = new StringBuilder();
			for (String searchKeyword : searchKeywords) {
				if (TextUtils.isEmpty(searchKeyword)) {
					continue;
				}
				String[] keywords = searchKeyword.toLowerCase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON);
				for (String keyword : keywords) {
					if (TextUtils.isEmpty(keyword)) {
						continue;
					}
					if (selectionSb.length() > 0) {
						selectionSb.append(SqlUtils.AND);
					}
					selectionSb.append(SqlUtils.P1);
					int c = 0;
					c = getSearchSelectionLikeColumns(searchableLikeColumns, selectionSb, keyword, c);
					//noinspection UnusedAssignment
					c = getSearchSelectionEqualColumns(searchableEqualColumns, selectionSb, keyword, c);
					selectionSb.append(SqlUtils.P2);
				}
			}
			return selectionSb.toString();
		}

		private static int getSearchSelectionEqualColumns(String[] searchableEqualColumns, @NonNull StringBuilder selectionSb, @NonNull String keyword, int c) {
			if (searchableEqualColumns != null) {
				for (String searchableColumn : searchableEqualColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(SqlUtils.OR);
					}
					selectionSb.append(SqlUtils.getWhereEqualsString(searchableColumn, keyword));
					c++;
				}
			}
			return c;
		}

		private static int getSearchSelectionLikeColumns(@Nullable String[] searchableLikeColumns, @NonNull StringBuilder selectionSb, @NonNull String keyword, int c) {
			if (searchableLikeColumns != null) {
				for (String searchableColumn : searchableLikeColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(SqlUtils.OR);
					}
					selectionSb.append(SqlUtils.getLikeContains(searchableColumn, keyword));
					c++;
				}
			}
			return c;
		}

		@NonNull
		public static String getSearchSelectionScore(@NonNull String[] searchKeywords, @Nullable String[] searchableLikeColumns, @Nullable String[] searchableEqualColumns) {
			if (ArrayUtils.getSize(searchKeywords) == 0
					|| TextUtils.isEmpty(searchKeywords[0])) {
				throw new UnsupportedOperationException(String.format("SQL search selection score needs at least 1 keyword (%s)!",
						ArrayUtils.getSize(searchKeywords)));
			}
			if (ArrayUtils.getSize(searchableLikeColumns) == 0
					&& ArrayUtils.getSize(searchableEqualColumns) == 0) {
				throw new UnsupportedOperationException(String.format("SQL search selection score needs at least 1 searchable columns (%s|%s)!",
						ArrayUtils.getSize(searchableLikeColumns), ArrayUtils.getSize(searchableEqualColumns)));
			}
			StringBuilder selectionSb = new StringBuilder();
			int c = 0;
			for (String searchKeyword : searchKeywords) {
				if (TextUtils.isEmpty(searchKeyword)) {
					continue;
				}
				String[] keywords = searchKeyword.toLowerCase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON);
				for (String keyword : keywords) {
					if (TextUtils.isEmpty(searchKeyword)) {
						continue;
					}
					c = getSearchSelectionScoreLikeColumns(searchableLikeColumns, selectionSb, keyword, c);
					c = getSearchSelectionScoreEqualColumns(searchableEqualColumns, selectionSb, keyword, c);
				}
			}
			return selectionSb.toString();
		}

		private static final String PLUS = " + ";

		private static int getSearchSelectionScoreEqualColumns(String[] searchableEqualColumns, StringBuilder selectionSb, String keyword, int c) {
			if (searchableEqualColumns != null) {
				for (String searchableColumn : searchableEqualColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(PLUS);
					}
					selectionSb.append(SqlUtils.P1).append(SqlUtils.getWhereEqualsString(searchableColumn, keyword)).append(SqlUtils.P2).append("*2");
					c++;
				}
			}
			return c;
		}

		private static int getSearchSelectionScoreLikeColumns(String[] searchableLikeColumns, StringBuilder selectionSb, String keyword, int c) {
			if (searchableLikeColumns != null) {
				for (String searchableColumn : searchableLikeColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(PLUS);
					}
					selectionSb.append(SqlUtils.P1).append(SqlUtils.getLikeContains(searchableColumn, keyword)).append(SqlUtils.P2);
					c++;
				}
			}
			return c;
		}

		@SuppressWarnings("DeprecatedIsStillUsed")
		@Deprecated // filtered in the main app
		@Nullable
		public Boolean getExcludeBookingRequired() {
			return this.excludeBookingRequired;
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

		@SuppressWarnings("ConstantConditions")
		private static Filter fromJSON(JSONObject json) {
			try {
				Filter poiFilter = new Filter();
				Double lat;
				Double lng;
				Double aroundDiff;
				Double minLat;
				Double maxLat;
				Double minLng;
				Double maxLng;
				Double optLoadedMinLat;
				Double optLoadedMaxLat;
				Double optLoadedMinLng;
				Double optLoadedMaxLng;
				try {
					lat = json.getDouble(JSON_LAT);
					lng = json.getDouble(JSON_LNG);
					aroundDiff = json.getDouble(JSON_AROUND_DIFF);
				} catch (JSONException jsone) {
					lat = null;
					lng = null;
					aroundDiff = null;
				}
				try {
					minLat = json.getDouble(JSON_MIN_LAT);
					maxLat = json.getDouble(JSON_MAX_LAT);
					minLng = json.getDouble(JSON_MIN_LNG);
					maxLng = json.getDouble(JSON_MAX_LNG);
					optLoadedMinLat = json.has(JSON_OPT_LOADED_MIN_LAT) ? json.getDouble(JSON_OPT_LOADED_MIN_LAT) : null;
					optLoadedMaxLat = json.has(JSON_OPT_LOADED_MAX_LAT) ? json.getDouble(JSON_OPT_LOADED_MAX_LAT) : null;
					optLoadedMinLng = json.has(JSON_OPT_LOADED_MIN_LNG) ? json.getDouble(JSON_OPT_LOADED_MIN_LNG) : null;
					optLoadedMaxLng = json.has(JSON_OPT_LOADED_MAX_LNG) ? json.getDouble(JSON_OPT_LOADED_MAX_LNG) : null;
				} catch (JSONException jsone) {
					minLat = null;
					maxLat = null;
					minLng = null;
					maxLng = null;
					optLoadedMinLat = null;
					optLoadedMaxLat = null;
					optLoadedMinLng = null;
					optLoadedMaxLng = null;
				}
				JSONArray jUUIDs = json.optJSONArray(JSON_UUIDS);
				JSONArray jSearchKeywords = json.optJSONArray(JSON_SEARCH_KEYWORDS);
				String sqlSelection = json.optString(JSON_SQL_SELECTION);
				if (lat != null && lng != null && aroundDiff != null) {
					poiFilter.setAround(lat, lng, aroundDiff);
				} else if (minLat != null && maxLat != null && minLng != null && maxLat != null) {
					poiFilter.setArea(minLat, maxLat, minLng, maxLng, optLoadedMinLat, optLoadedMaxLat, optLoadedMinLng, optLoadedMaxLng);
				} else if (jUUIDs != null && jUUIDs.length() > 0) {
					HashSet<String> uuids = new HashSet<>();
					for (int i = 0; i < jUUIDs.length(); i++) {
						uuids.add(jUUIDs.getString(i));
					}
					poiFilter.setUUIDs(uuids);
				} else if (jSearchKeywords != null && jSearchKeywords.length() > 0) {
					ArrayList<String> searchKeywords = new ArrayList<>();
					for (int i = 0; i < jSearchKeywords.length(); i++) {
						searchKeywords.add(jSearchKeywords.getString(i));
					}
					poiFilter.setSearchKeywords(searchKeywords.toArray(new String[0]));
				} else if (sqlSelection != null) {
					poiFilter.setSqlSelection(sqlSelection);
				} else {
					MTLog.w(LOG_TAG, "Empty POI filter JSON object '%s'", json);
					return null;
				}
				JSONArray jExtras = json.getJSONArray(JSON_EXTRAS);
				for (int i = 0; i < jExtras.length(); i++) {
					JSONObject jExtra = jExtras.getJSONObject(i);
					String key = jExtra.getString(JSON_EXTRAS_KEY);
					Object value = jExtra.get(JSON_EXTRAS_VALUE);
					poiFilter.addExtra(key, value);
				}
				if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
					//noinspection deprecation // filtered in the main app
					poiFilter.setExcludeBookingRequired(JSONUtils.optBoolean(json, JSON_EXCLUDE_BOOKING_REQUIRED));
				}
				return poiFilter;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		private static final String JSON_LAT = "lat";
		private static final String JSON_LNG = "lng";
		private static final String JSON_AROUND_DIFF = "aroundDiff";
		private static final String JSON_MIN_LAT = "minLat";
		private static final String JSON_MAX_LAT = "maxLat";
		private static final String JSON_MIN_LNG = "minLng";
		private static final String JSON_MAX_LNG = "maxLng";
		private static final String JSON_OPT_LOADED_MIN_LAT = "optLoadedMinLat";
		private static final String JSON_OPT_LOADED_MAX_LAT = "optLoadedMaxLat";
		private static final String JSON_OPT_LOADED_MIN_LNG = "optLoadedMinLng";
		private static final String JSON_OPT_LOADED_MAX_LNG = "optLoadedMaxLng";
		private static final String JSON_UUIDS = "uuids";
		private static final String JSON_SEARCH_KEYWORDS = "searchKeywords";
		private static final String JSON_SQL_SELECTION = "sqlSelection";
		private static final String JSON_EXTRAS = "extras";
		private static final String JSON_EXTRAS_KEY = "key";
		private static final String JSON_EXTRAS_VALUE = "value";
		@SuppressWarnings("DeprecatedIsStillUsed")
		@Deprecated // filtered in the main app
		private static final String JSON_EXCLUDE_BOOKING_REQUIRED = "exc_booking_req";

		@Nullable
		public static JSONObject toJSON(@Nullable Filter poiFilter) {
			try {
				JSONObject json = new JSONObject();
				if (isAreaFilter(poiFilter)) {
					json.put(JSON_LAT, poiFilter.lat);
					json.put(JSON_LNG, poiFilter.lng);
					json.put(JSON_AROUND_DIFF, poiFilter.aroundDiff);
				} else if (isAreasFilter(poiFilter)) {
					json.put(JSON_MIN_LAT, poiFilter.minLat);
					json.put(JSON_MAX_LAT, poiFilter.maxLat);
					json.put(JSON_MIN_LNG, poiFilter.minLng);
					json.put(JSON_MAX_LNG, poiFilter.maxLng);
					if (poiFilter.optLoadedMinLat != null) {
						json.put(JSON_OPT_LOADED_MIN_LAT, poiFilter.optLoadedMinLat);
					}
					if (poiFilter.optLoadedMaxLat != null) {
						json.put(JSON_OPT_LOADED_MAX_LAT, poiFilter.optLoadedMaxLat);
					}
					if (poiFilter.optLoadedMinLng != null) {
						json.put(JSON_OPT_LOADED_MIN_LNG, poiFilter.optLoadedMinLng);
					}
					if (poiFilter.optLoadedMaxLng != null) {
						json.put(JSON_OPT_LOADED_MAX_LNG, poiFilter.optLoadedMaxLng);
					}
				} else if (isUUIDFilter(poiFilter) && poiFilter.uuids != null) {
					JSONArray jUUIDs = new JSONArray();
					for (String uuid : poiFilter.uuids) {
						jUUIDs.put(uuid);
					}
					json.put(JSON_UUIDS, jUUIDs);
				} else if (isSearchKeywords(poiFilter) && poiFilter.searchKeywords != null) {
					JSONArray jSearchKeywords = new JSONArray();
					for (String searchKeyword : poiFilter.searchKeywords) {
						jSearchKeywords.put(searchKeyword);
					}
					json.put(JSON_SEARCH_KEYWORDS, jSearchKeywords);
				} else if (isSQLSelection(poiFilter)) {
					json.put(JSON_SQL_SELECTION, poiFilter.sqlSelection);
				} else {
					MTLog.w(LOG_TAG, "Empty POI filter '%s' converted to JSON!", poiFilter);
				}
				JSONArray jExtras = new JSONArray();
				if (poiFilter != null) {
					for (int i = 0; i < poiFilter.extras.size(); i++) {
						JSONObject jExtra = new JSONObject();
						jExtra.put(JSON_EXTRAS_KEY, poiFilter.extras.keyAt(i));
						jExtra.put(JSON_EXTRAS_VALUE, poiFilter.extras.valueAt(i));
						jExtras.put(jExtra);
					}
				}
				json.put(JSON_EXTRAS, jExtras);
				if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
					if (poiFilter != null) {
						//noinspection deprecation // filtered in the main app
						json.put(JSON_EXCLUDE_BOOKING_REQUIRED, poiFilter.excludeBookingRequired);
					}
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", poiFilter);
				return null;
			}
		}

		public boolean getExtraBoolean(@NonNull String key, boolean defaultValue) {
			final Object value = this.extras.get(key);
			if (value == null) {
				return defaultValue;
			}
			return (Boolean) value;
		}

		@Nullable
		public String getExtraString(@NonNull String key, @Nullable String defaultValue) {
			final Object value = this.extras.get(key);
			if (value == null) {
				return defaultValue;
			}
			return (String) value;
		}

		@Nullable
		public Double getExtraDouble(@NonNull String key, @Nullable Double defaultValue) {
			final Object value = this.extras.get(key);
			if (value == null) {
				return defaultValue;
			}
			return (Double) value;
		}
	}
}
