package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;

import android.text.TextUtils;
public class POIFilter implements MTLog.Loggable {

	private static final String TAG = POIFilter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Double lat = null;
	private Double lng = null;
	private Double aroundDiff = null;

	private Collection<String> uuids;

	private Map<String, Object> extras = new HashMap<String, Object>();

	private String sqlSelection = null;

	private String[] searchKeywords = null;

	public POIFilter(String sqlSelection) {
		if (sqlSelection == null) {
			throw new UnsupportedOperationException("Need an SQL selection!");
		}
		this.sqlSelection = sqlSelection;
	}

	public POIFilter(String[] searchKeywords) {
		if (searchKeywords == null || searchKeywords.length == 0) {
			throw new UnsupportedOperationException("Need at least 1 search keyword!");
		}
		this.searchKeywords = searchKeywords;
	}

	public POIFilter(Collection<String> uuids) {
		if (uuids == null || uuids.size() == 0) {
			throw new UnsupportedOperationException("Need at least 1 uuid!");
		}
		this.uuids = uuids;
	}

	public POIFilter(double lat, double lng, double aroundDiff) {
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(POIFilter.class.getSimpleName()).append('['); //
		if (isAreaFilter(this)) {
			sb.append("lat:").append(lat).append(',');//
			sb.append("lng:").append(lng).append(','); //
			sb.append("aroundDiff:").append(aroundDiff).append(','); //
		} else if (isUUIDFilter(this)) {
			sb.append("uuids:").append(this.uuids).append(','); //
		} else if (isSearchKeywords(this)) {
			sb.append("searchKeywords:").append(this.searchKeywords).append(',');
		} else if (isSQLSelection(this)) {
			sb.append("sqlSelection:").append(this.sqlSelection).append(',');
		}
		sb.append("extras:").append(extras); //
		sb.append(']');
		return sb.toString();
	}

	public static boolean isUUIDFilter(POIFilter poiFilter) {
		return poiFilter.uuids != null && poiFilter.uuids.size() > 0;
	}

	public static boolean isAreaFilter(POIFilter poiFilter) {
		return poiFilter.lat != null && poiFilter.lng != null && poiFilter.aroundDiff != null;
	}

	public static boolean isSearchKeywords(POIFilter poiFilter) {
		return poiFilter.searchKeywords != null && poiFilter.searchKeywords.length > 0;
	}

	public static boolean isSQLSelection(POIFilter poiFilter) {
		return poiFilter.sqlSelection != null;
	}

	public void addExtra(String key, Object value) {
		this.extras.put(key, value);
	}

	public String getSqlSelection(String uuidTableColumn, String latTableColumn, String lngTableColumn, String[] searchableLikeColumns,
			String[] searchableEqualColumns) {
		if (isAreaFilter(this)) {
			return LocationUtils.genAroundWhere(this.lat, this.lng, latTableColumn, lngTableColumn, this.aroundDiff);
		} else if (isUUIDFilter(this)) {
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
			throw new UnsupportedOperationException("SQL search selection needs at least 1 keyword (" + searchKeywords + ")!");
		}
		if (ArrayUtils.getSize(searchableLikeColumns) == 0 && ArrayUtils.getSize(searchableEqualColumns) == 0) {
			throw new UnsupportedOperationException("SQL search selection needs at least 1 searchable columns (" + searchableLikeColumns + "|"
					+ searchableEqualColumns + ")!");
		}
		StringBuilder selectionSb = new StringBuilder();
		for (String searchKeyword : searchKeywords) {
			if (TextUtils.isEmpty(searchKeyword)) {
				continue;
			}
			final String[] keywords = searchKeyword.toLowerCase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON);
			for (String keyword : keywords) {
				if (TextUtils.isEmpty(searchKeyword)) {
					continue;
				}
				if (selectionSb.length() > 0) {
					selectionSb.append(" AND ");
				}
				selectionSb.append("(");
				int c = 0;
				for (String searchableColumn : searchableLikeColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(" OR ");
					}
					selectionSb.append(searchableColumn).append(" LIKE '%").append(keyword).append("%'");
					c++;
				}
				for (String searchableColumn : searchableEqualColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(" OR ");
					}
					selectionSb.append(searchableColumn).append("='").append(keyword).append("'");
					c++;
				}
				selectionSb.append(")");
			}
		}
		return selectionSb.toString();
	}

	public static String getSearchSelectionScore(String[] searchKeywords, String[] searchableLikeColumns, String[] searchableEqualColumns) {
		if (ArrayUtils.getSize(searchKeywords) == 0 || TextUtils.isEmpty(searchKeywords[0])) {
			throw new UnsupportedOperationException("SQL search selection score needs at least 1 keyword (" + searchKeywords + ")!");
		}
		if (ArrayUtils.getSize(searchableLikeColumns) == 0 && ArrayUtils.getSize(searchableEqualColumns) == 0) {
			throw new UnsupportedOperationException("SQL search selection score needs at least 1 searchable columns (" + searchableLikeColumns + "|"
					+ searchableEqualColumns + ")!");
		}
		StringBuilder selectionSb = new StringBuilder();
		int c = 0;
		for (String searchKeyword : searchKeywords) {
			if (TextUtils.isEmpty(searchKeyword)) {
				continue;
			}
			final String[] keywords = searchKeyword.toLowerCase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON);
			for (String keyword : keywords) {
				if (TextUtils.isEmpty(searchKeyword)) {
					continue;
				}
				for (String searchableColumn : searchableLikeColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(" + ");
					}
					selectionSb.append('(').append(searchableColumn).append(" LIKE '%").append(keyword).append("%'").append(')');
					c++;
				}
				for (String searchableColumn : searchableEqualColumns) {
					if (TextUtils.isEmpty(searchableColumn)) {
						continue;
					}
					if (c > 0) {
						selectionSb.append(" + ");
					}
					selectionSb.append('(').append(searchableColumn).append("='").append(keyword).append("'").append(')').append("*2");
					c++;
				}
			}
		}
		return selectionSb.toString();
	}

	public static POIFilter fromJSONString(String jsonString) {
		try {
			return fromJSON(new JSONObject(jsonString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
			return null;
		}
	}

	private static POIFilter fromJSON(JSONObject json) {
		try {
			final POIFilter poiFilter;
			Double lat;
			Double lng;
			Double aroundDiff;
			try {
				lat = json.getDouble("lat");
				lng = json.getDouble("lng");
				aroundDiff = json.getDouble("aroundDiff");
			} catch (JSONException jsone) {
				lat = null;
				lng = null;
				aroundDiff = null;
			}
			JSONArray jUUIDs = json.optJSONArray("uuids");
			JSONArray jSearchKeywords = json.optJSONArray("searchKeywords");
			String sqlSelection = json.optString("sqlSelection");
			if (lat != null && lng != null && aroundDiff != null) {
				poiFilter = new POIFilter(lat, lng, aroundDiff);
			} else if (jUUIDs != null && jUUIDs.length() > 0) {
				HashSet<String> uuids = new HashSet<String>();
				for (int i = 0; i < jUUIDs.length(); i++) {
					uuids.add(jUUIDs.getString(i));
				}
				poiFilter = new POIFilter(uuids);
			} else if (jSearchKeywords != null && jSearchKeywords.length() > 0) {
				List<String> searchKeywords = new ArrayList<String>();
				for (int i = 0; i < jSearchKeywords.length(); i++) {
					searchKeywords.add(jSearchKeywords.getString(i));
				}
				poiFilter = new POIFilter(searchKeywords.toArray(new String[] {}));
			} else if (sqlSelection != null) {
				poiFilter = new POIFilter(sqlSelection);
			} else {
				MTLog.w(TAG, "Empty POI filter JSON object '%s'", json);
				return null;
			}
			JSONArray jExtras = json.getJSONArray("extras");
			for (int i = 0; i < jExtras.length(); i++) {
				JSONObject jExtra = jExtras.getJSONObject(i);
				final String key = jExtra.getString("key");
				final Object value = jExtra.get("value");
				poiFilter.addExtra(key, value);
			}
			return poiFilter;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
			return null;
		}
	}

	public static JSONObject toJSON(POIFilter poiFilter) throws JSONException {
		try {
			JSONObject json = new JSONObject();
			if (isAreaFilter(poiFilter)) {
				json.put("lat", poiFilter.lat);
				json.put("lng", poiFilter.lng);
				json.put("aroundDiff", poiFilter.aroundDiff);
			} else if (isUUIDFilter(poiFilter)) {
				JSONArray jUUIDs = new JSONArray();
				for (String uuid : poiFilter.uuids) {
					jUUIDs.put(uuid);
				}
				json.put("uuids", jUUIDs);
			} else if (isSearchKeywords(poiFilter)) {
				JSONArray jSearchKeywords = new JSONArray();
				for (String searchKeyword : poiFilter.searchKeywords) {
					jSearchKeywords.put(searchKeyword);
				}
				json.put("searchKeywords", jSearchKeywords);
			} else if (isSQLSelection(poiFilter)) {
				json.put("sqlSelection", poiFilter.sqlSelection);
			} else {
				MTLog.w(TAG, "Empty POI filter '%s' converted to JSON!", poiFilter);
			}
			JSONArray jExtras = new JSONArray();
			if (poiFilter.extras != null) {
				for (Map.Entry<String, Object> extra : poiFilter.extras.entrySet()) {
					JSONObject jExtra = new JSONObject();
					jExtra.put("key", extra.getKey());
					jExtra.put("value", extra.getValue());
					jExtras.put(jExtra);
				}
			}
			json.put("extras", jExtras);
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
}
