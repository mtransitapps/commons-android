package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;

import android.app.SearchManager;
import android.database.Cursor;
import android.provider.BaseColumns;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import android.text.TextUtils;

public interface POIProviderContract extends ProviderContract {

	String POI_PATH = "poi";

	String POI_FILTER_EXTRA_AVOID_LOADING = "avoidLoading";

	String POI_FILTER_EXTRA_SORT_ORDER = "sortOrder";

	long getPOIMaxValidityInMs();

	long getPOIValidityInMs();

	Cursor getPOI(@Nullable Filter poiFilter);

	@Nullable
	Cursor getPOIFromDB(@Nullable Filter poiFilter);

	ArrayMap<String, String> getPOIProjectionMap();

	String[] getPOIProjection();

	String getPOITable();

	Cursor getSearchSuggest(String query);

	String getSearchSuggestTable();

	ArrayMap<String, String> getSearchSuggestProjectionMap();

	String[] PROJECTION_POI_ALL_COLUMNS = null; // null = return all columns

	String[] PROJECTION_POI = new String[] { Columns.T_POI_K_UUID_META, Columns.T_POI_K_DST_ID_META, Columns.T_POI_K_ID, Columns.T_POI_K_NAME,
			Columns.T_POI_K_LAT, Columns.T_POI_K_LNG, Columns.T_POI_K_TYPE, Columns.T_POI_K_STATUS_TYPE, Columns.T_POI_K_ACTIONS_TYPE };

	String[] PROJECTION_POI_SEARCH_SUGGEST = new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1 };

	class Columns {
		public static final String T_POI_K_ID = BaseColumns._ID;
		public static final String T_POI_K_UUID_META = "uuid";
		public static final String T_POI_K_DST_ID_META = "dst";
		public static final String T_POI_K_NAME = "name";
		public static final String T_POI_K_LAT = "lat";
		public static final String T_POI_K_LNG = "lng";
		public static final String T_POI_K_TYPE = "type";
		public static final String T_POI_K_STATUS_TYPE = "statustype";
		public static final String T_POI_K_ACTIONS_TYPE = "actionstype";
		//
		public static final String T_POI_K_SCORE_META_OPT = "score"; // optional

		public static String getFkColumnName(String key) {
			return "fk" + "_" + key;
		}
	}

	class Filter implements MTLog.Loggable {

		private static final String TAG = POIProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private Double lat = null;
		private Double lng = null;
		private Double aroundDiff = null;

		private Double minLat = null;
		private Double maxLat = null;
		private Double minLng = null;
		private Double maxLng = null;
		private Double optLoadedMinLat = null;
		private Double optLoadedMaxLat = null;
		private Double optLoadedMinLng = null;
		private Double optLoadedMaxLng = null;

		private Collection<String> uuids;

		private ArrayMap<String, Object> extras = new ArrayMap<String, Object>();

		private String sqlSelection = null;

		private String[] searchKeywords = null;

		private Filter() {
		}

		public static Filter getNewEmptyFilter() {
			return getNewSqlSelectionFilter(StringUtils.EMPTY);
		}

		public static Filter getNewSqlSelectionFilter(String sqlSelection) {
			return new Filter().setSqlSelection(sqlSelection);
		}

		private Filter setSqlSelection(String sqlSelection) {
			if (sqlSelection == null) {
				throw new UnsupportedOperationException("Need an SQL selection!");
			}
			this.sqlSelection = sqlSelection;
			return this;
		}

		public static Filter getNewSearchFilter(String searchKeyword) {
			return getNewSearchFilter(new String[] { searchKeyword });
		}

		public static Filter getNewSearchFilter(String[] searchKeywords) {
			return new Filter().setSearchKeywords(searchKeywords);
		}

		private Filter setSearchKeywords(String[] searchKeywords) {
			if (ArrayUtils.getSize(searchKeywords) == 0) {
				throw new UnsupportedOperationException("Need at least 1 search keyword!");
			}
			this.searchKeywords = searchKeywords;
			return this;
		}

		public static Filter getNewUUIDFilter(String uuid) {
			return getNewUUIDsFilter(Collections.singletonList(uuid));
		}

		public static Filter getNewUUIDsFilter(Collection<String> uuids) {
			return new Filter().setUUIDs(uuids);
		}

		private Filter setUUIDs(Collection<String> uuids) {
			if (CollectionUtils.getSize(uuids) == 0) {
				throw new UnsupportedOperationException("Need at least 1 uuid!");
			}
			this.uuids = uuids;
			return this;
		}

		public static Filter getNewAroundFilter(double lat, double lng, double aroundDiff) {
			return new Filter().setAround(lat, lng, aroundDiff);
		}

		private Filter setAround(double lat, double lng, double aroundDiff) {
			this.lat = lat;
			this.lng = lng;
			this.aroundDiff = aroundDiff;
			return this;
		}

		public static Filter getNewAreaFilter(double minLat, double maxLat, double minLng, double maxLng, Double optLoadedMinLat, Double optLoadedMaxLat,
				Double optLoadedMinLng, Double optLoadedMaxLng) {
			return new Filter().setArea(minLat, maxLat, minLng, maxLng, optLoadedMinLat, optLoadedMaxLat, optLoadedMinLng, optLoadedMaxLng);
		}

		private Filter setArea(double minLat, double maxLat, double minLng, double maxLng, Double optLoadedMinLat, Double optLoadedMaxLat,
				Double optLoadedMinLng, Double optLoadedMaxLng) {
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
				sb.append("searchKeywords:").append(java.util.Arrays.asList(this.searchKeywords)).append(',');
			} else if (isSQLSelection(this)) {
				sb.append("sqlSelection:").append(this.sqlSelection).append(',');
			}
			sb.append("extras:").append(this.extras);
			sb.append(']');
			return sb.toString();
		}

		public static boolean isUUIDFilter(Filter poiFilter) {
			return poiFilter != null && CollectionUtils.getSize(poiFilter.uuids) > 0;
		}

		public static boolean isAreaFilter(Filter poiFilter) {
			return poiFilter != null && poiFilter.lat != null && poiFilter.lng != null && poiFilter.aroundDiff != null;
		}

		public static boolean isAreasFilter(Filter poiFilter) {
			return poiFilter != null && poiFilter.minLat != null && poiFilter.maxLat != null && poiFilter.minLng != null && poiFilter.maxLng != null;
		}

		public static boolean isSearchKeywords(Filter poiFilter) {
			return poiFilter != null && ArrayUtils.getSize(poiFilter.searchKeywords) > 0;
		}

		public static boolean isSQLSelection(Filter poiFilter) {
			return poiFilter != null && poiFilter.sqlSelection != null;
		}

		public void addExtra(String key, Object value) {
			this.extras.put(key, value);
		}

		public Double getLat() {
			return lat;
		}

		public Double getLng() {
			return lng;
		}

		public Double getAroundDiff() {
			return aroundDiff;
		}

		public String getSqlSelection(String uuidTableColumn, String latTableColumn, String lngTableColumn, String[] searchableLikeColumns,
				String[] searchableEqualColumns) {
			if (isAreaFilter(this)) {
				return LocationUtils.genAroundWhere(this.lat, this.lng, latTableColumn, lngTableColumn, this.aroundDiff);
			} else if (isAreasFilter(this)) {
				StringBuilder sb = new StringBuilder();
				sb.append(SqlUtils.P1);
				sb.append(SqlUtils.getBetween(latTableColumn, this.minLat, this.maxLat));
				sb.append(SqlUtils.AND);
				sb.append(SqlUtils.getBetween(lngTableColumn, this.minLng, this.maxLng));
				sb.append(SqlUtils.P2);
				if (this.optLoadedMinLat != null && this.optLoadedMaxLat != null && this.optLoadedMinLng != null && this.optLoadedMaxLng != null) {
					sb.append(SqlUtils.AND);
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
			} else if (isSearchKeywords(this)) {
				return getSearchSelection(this.searchKeywords, searchableLikeColumns, searchableEqualColumns);
			} else if (isSQLSelection(this)) {
				return this.sqlSelection;
			} else {
				throw new UnsupportedOperationException("SQL selection impossible!");
			}
		}

		public String[] getSearchKeywords() {
			return searchKeywords;
		}

		public static String getSearchSelection(String[] searchKeywords, String[] searchableLikeColumns, String[] searchableEqualColumns) {
			if (ArrayUtils.getSize(searchKeywords) == 0 || TextUtils.isEmpty(searchKeywords[0])) {
				throw new UnsupportedOperationException(
						String.format("SQL search selection needs at least 1 keyword (%s)!", ArrayUtils.getSize(searchKeywords)));
			}
			if (ArrayUtils.getSize(searchableLikeColumns) == 0 && ArrayUtils.getSize(searchableEqualColumns) == 0) {
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
					if (TextUtils.isEmpty(searchKeyword)) {
						continue;
					}
					if (selectionSb.length() > 0) {
						selectionSb.append(SqlUtils.AND);
					}
					selectionSb.append(SqlUtils.P1);
					int c = 0;
					c = getSearchSelectionLikeColumns(searchableLikeColumns, selectionSb, keyword, c);
					c = getSearchSelectionEqualColumns(searchableEqualColumns, selectionSb, keyword, c);
					selectionSb.append(SqlUtils.P2);
				}
			}
			return selectionSb.toString();
		}

		private static int getSearchSelectionEqualColumns(String[] searchableEqualColumns, StringBuilder selectionSb, String keyword, int c) {
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

		private static int getSearchSelectionLikeColumns(String[] searchableLikeColumns, StringBuilder selectionSb, String keyword, int c) {
			if (searchableLikeColumns != null) {
				for (String searchableColumn : searchableLikeColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(SqlUtils.OR);
					}
					selectionSb.append(SqlUtils.getLike(searchableColumn, keyword));
					c++;
				}
			}
			return c;
		}

		public static String getSearchSelectionScore(String[] searchKeywords, String[] searchableLikeColumns, String[] searchableEqualColumns) {
			if (ArrayUtils.getSize(searchKeywords) == 0 || TextUtils.isEmpty(searchKeywords[0])) {
				throw new UnsupportedOperationException(String.format("SQL search selection score needs at least 1 keyword (%s)!",
						ArrayUtils.getSize(searchKeywords)));
			}
			if (ArrayUtils.getSize(searchableLikeColumns) == 0 && ArrayUtils.getSize(searchableEqualColumns) == 0) {
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
					selectionSb.append(SqlUtils.P1).append(SqlUtils.getLike(searchableColumn, keyword)).append(SqlUtils.P2);
					c++;
				}
			}
			return c;
		}

		@Nullable
		public static Filter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

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
					HashSet<String> uuids = new HashSet<String>();
					for (int i = 0; i < jUUIDs.length(); i++) {
						uuids.add(jUUIDs.getString(i));
					}
					poiFilter.setUUIDs(uuids);
				} else if (jSearchKeywords != null && jSearchKeywords.length() > 0) {
					ArrayList<String> searchKeywords = new ArrayList<String>();
					for (int i = 0; i < jSearchKeywords.length(); i++) {
						searchKeywords.add(jSearchKeywords.getString(i));
					}
					poiFilter.setSearchKeywords(searchKeywords.toArray(new String[0]));
				} else if (sqlSelection != null) {
					poiFilter.setSqlSelection(sqlSelection);
				} else {
					MTLog.w(TAG, "Empty POI filter JSON object '%s'", json);
					return null;
				}
				JSONArray jExtras = json.getJSONArray(JSON_EXTRAS);
				for (int i = 0; i < jExtras.length(); i++) {
					JSONObject jExtra = jExtras.getJSONObject(i);
					String key = jExtra.getString(JSON_EXTRAS_KEY);
					Object value = jExtra.get(JSON_EXTRAS_VALUE);
					poiFilter.addExtra(key, value);
				}
				return poiFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
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

		public static JSONObject toJSON(Filter poiFilter) {
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
				} else if (isUUIDFilter(poiFilter)) {
					JSONArray jUUIDs = new JSONArray();
					for (String uuid : poiFilter.uuids) {
						jUUIDs.put(uuid);
					}
					json.put(JSON_UUIDS, jUUIDs);
				} else if (isSearchKeywords(poiFilter)) {
					JSONArray jSearchKeywords = new JSONArray();
					for (String searchKeyword : poiFilter.searchKeywords) {
						jSearchKeywords.put(searchKeyword);
					}
					json.put(JSON_SEARCH_KEYWORDS, jSearchKeywords);
				} else if (isSQLSelection(poiFilter)) {
					json.put(JSON_SQL_SELECTION, poiFilter.sqlSelection);
				} else {
					MTLog.w(TAG, "Empty POI filter '%s' converted to JSON!", poiFilter);
				}
				JSONArray jExtras = new JSONArray();
				if (poiFilter.extras != null) {
					for (ArrayMap.Entry<String, Object> extra : poiFilter.extras.entrySet()) {
						JSONObject jExtra = new JSONObject();
						jExtra.put(JSON_EXTRAS_KEY, extra.getKey());
						jExtra.put(JSON_EXTRAS_VALUE, extra.getValue());
						jExtras.put(jExtra);
					}
				}
				json.put(JSON_EXTRAS, jExtras);
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", poiFilter);
				return null;
			}
		}

		public boolean getExtraBoolean(String key, boolean defaultValue) {
			if (this.extras == null || !this.extras.containsKey(key)) {
				return defaultValue;
			}
			return (Boolean) this.extras.get(key);
		}

		public String getExtraString(String key, String defaultValue) {
			if (this.extras == null || !this.extras.containsKey(key)) {
				return defaultValue;
			}
			return (String) this.extras.get(key);
		}

		public Double getExtraDouble(String key, Double defaultValue) {
			if (this.extras == null || !this.extras.containsKey(key)) {
				return defaultValue;
			}
			return (Double) this.extras.get(key);
		}
	}
}
