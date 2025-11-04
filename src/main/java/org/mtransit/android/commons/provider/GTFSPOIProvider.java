package org.mtransit.android.commons.provider;

import android.app.SearchManager;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.commons.Constants;
import org.mtransit.commons.FeatureFlags;

import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class GTFSPOIProvider implements MTLog.Loggable {

	private static final String TAG = GTFSPOIProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		POIProvider.append(uriMatcher, authority);
	}

	private static int agencyTypeId = -1;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public static int getAGENCY_TYPE_ID(@NonNull Context context) {
		if (agencyTypeId < 0) {
			agencyTypeId = context.getResources().getInteger(R.integer.gtfs_rts_agency_type); // do not change to avoid breaking compat w/ old modules
		}
		return agencyTypeId;
	}

	@Nullable
	public static Cursor queryS(@NonNull GTFSProvider provider, @NonNull Uri uri, @Nullable String selection) {
		return POIProvider.queryS(provider, uri, selection);
	}

	@Nullable
	public static String getSortOrderS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		return POIProvider.getSortOrderS(provider, uri);
	}

	@Nullable
	public static String getTypeS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		return POIProvider.getTypeS(provider, uri);
	}

	@Nullable
	public static Cursor getSearchSuggest(@NonNull GTFSProvider provider, @Nullable String query) {
		return POIProvider.getDefaultSearchSuggest(query, provider); // simple search suggest
	}

	@NonNull
	public static String getSearchSuggestTable(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return GTFSProviderDbHelper.T_STOP; // simple search suggest
	}

	// @formatter:off
	private static final ArrayMap<String, String> SIMPLE_SEARCH_SUGGEST_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, SearchManager.SUGGEST_COLUMN_TEXT_1) //
			.build();
	// @formatter:on

	@NonNull
	public static ArrayMap<String, String> getSearchSuggestProjectionMap(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return SIMPLE_SEARCH_SUGGEST_PROJECTION_MAP; // simple search suggest
	}

	@Nullable
	public static Cursor getPOI(@NonNull GTFSProvider provider, @Nullable POIProviderContract.Filter poiFilter) {
		return provider.getPOIFromDB(poiFilter);
	}

	private static final String[] SEARCHABLE_LIKE_COLUMNS = new String[]{ //
			SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME),//
			SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME),//
	};
	private static final String[] SEARCHABLE_EQUAL_COLUMNS = new String[]{ //
			SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE), //
			SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME),//
	};

	@Nullable
	public static Cursor getPOIFromDB(@NonNull GTFSProvider provider, @Nullable POIProviderContract.Filter poiFilter) {
		try {
			if (poiFilter == null) {
				return null;
			}
			String selection = poiFilter.getSqlSelection(POIProviderContract.Columns.T_POI_K_UUID_META, POIProviderContract.Columns.T_POI_K_LAT,
					POIProviderContract.Columns.T_POI_K_LNG, SEARCHABLE_LIKE_COLUMNS, SEARCHABLE_EQUAL_COLUMNS);
			if (poiFilter.getExtraBoolean(GTFSProviderContract.POI_FILTER_EXTRA_NO_PICKUP, false)) {
				selection = SqlUtils.appendToSelection(selection, SqlUtils.getWhereBooleanNotTrue(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_NO_PICKUP));
			}
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSRDSProvider.ROUTE_DIRECTION_DIRECTION_STOPS_STOP_JOIN);
			ArrayMap<String, String> poiProjectionMap = provider.getPOIProjectionMap();
			boolean searchKeywordsAdded = false;
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter) && poiFilter.getSearchKeywords() != null) {
				SqlUtils.appendProjection(poiProjectionMap,
						POIProviderContract.Filter.getSearchSelectionScore(poiFilter.getSearchKeywords(), SEARCHABLE_LIKE_COLUMNS, SEARCHABLE_EQUAL_COLUMNS),
						POIProviderContract.Columns.T_POI_K_SCORE_META_OPT);
				searchKeywordsAdded = true;
			}
			qb.setProjectionMap(poiProjectionMap);

			String[] poiProjection = provider.getPOIProjection();
			if (searchKeywordsAdded) {
				poiProjection = ArrayUtils.addAllNonNull(poiProjection, new String[]{POIProviderContract.Columns.T_POI_K_SCORE_META_OPT});
			}
			if (poiProjection.length != poiProjectionMap.size()) {
				MTLog.w(TAG, "getPOIFromDB() > different projection sizes (%d VS %d)", poiProjection.length, poiProjectionMap.size());
				if (Constants.DEBUG) {
					MTLog.w(TAG, "getPOIFromDB() > poiProjection: %d", poiProjection.length);
					for (String string : poiProjection) {
						MTLog.w(TAG, "getPOIFromDB() > poiProjection: - %s.", string);
					}
					MTLog.w(TAG, "getPOIFromDB() > poiProjectionMap: %d", poiProjectionMap.size());
					for (Map.Entry<String, String> keyValue : poiProjectionMap.entrySet()) {
						MTLog.w(TAG, "getPOIFromDB() > poiProjectionMap: - %s: %s.", keyValue.getKey(), keyValue.getValue());
					}
				}
			}
			String groupBy = null;
			if (searchKeywordsAdded) {
				groupBy = POIProviderContract.Columns.T_POI_K_UUID_META;
			}
			String sortOrder = poiFilter.getExtraString(POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER, null);
			if (searchKeywordsAdded) {
				sortOrder = SqlUtils.getSortOrderDescending(POIProviderContract.Columns.T_POI_K_SCORE_META_OPT);
			}
			return qb.query(provider.getReadDB(), poiProjection, selection, null, groupBy, null, sortOrder, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while loading POIs '%s'!", poiFilter);
			return null;
		}
	}

	@NonNull
	public static String[] getPOIProjection(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return GTFSProviderContract.PROJECTION_RDS_POI;
	}

	private static ArrayMap<String, String> poiProjectionMap;

	@NonNull
	public static ArrayMap<String, String> getPOIProjectionMap(@NonNull GTFSProvider provider) {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewProjectionMap(GTFSProvider.getAUTHORITY(provider.requireContextCompat()), getAGENCY_TYPE_ID(provider.requireContextCompat()));
		}
		return poiProjectionMap;
	}

	@NonNull
	private static ArrayMap<String, String> getNewProjectionMap(String authority, int dataSourceTypeId) {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		// POIProviderContract.Columns
		sb.appendValue(
				SqlUtils.concatenate(
						SqlUtils.escapeString(POI.POIUtils.UID_SEPARATOR),
						SqlUtils.escapeString(authority),
						SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID),
						SqlUtils.getTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID),
						SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID)
				),
				POIProviderContract.Columns.T_POI_K_UUID_META
		);
		sb.appendValue(dataSourceTypeId, POIProviderContract.Columns.T_POI_K_DST_ID_META);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, POIProviderContract.Columns.T_POI_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, POIProviderContract.Columns.T_POI_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, POIProviderContract.Columns.T_POI_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, POIProviderContract.Columns.T_POI_K_LNG);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, POIProviderContract.Columns.T_POI_K_ACCESSIBLE);
		sb.appendValue(POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP, POIProviderContract.Columns.T_POI_K_TYPE);
		sb.appendValue(POI.ITEM_STATUS_TYPE_SCHEDULE, POIProviderContract.Columns.T_POI_K_STATUS_TYPE);
		sb.appendValue(POI.ITEM_ACTION_TYPE_ROUTE_DIRECTION_STOP, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE);
		// GTFSProviderContract.RouteDirectionStopColumns
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_CODE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_LNG);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ACCESSIBLE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_STOP_SEQUENCE, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_NO_PICKUP, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_NO_PICKUP);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_TYPE, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_TYPE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_VALUE, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_VALUE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ROUTE_ID, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ROUTE_ID);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_SHORT_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_LONG_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_COLOR);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
		if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_TYPE);
		}
		return sb.build();
	}

	@NonNull
	public static String getPOITable(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		//noinspection ConstantConditions TODO throw new RuntimeException("Should never user default table for GTFS POI provider!");
		return null; // USING CUSTOM TABLE
	}

	private static final long POI_MAX_VALIDITY_IN_MS = ProviderContract.MAX_CACHE_VALIDITY_MS;

	public static long getPOIMaxValidityInMs(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return POI_MAX_VALIDITY_IN_MS;
	}

	private static final long POI_VALIDITY_IN_MS = ProviderContract.MAX_CACHE_VALIDITY_MS;

	public static long getPOIValidityInMs(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return POI_VALIDITY_IN_MS;
	}
}
