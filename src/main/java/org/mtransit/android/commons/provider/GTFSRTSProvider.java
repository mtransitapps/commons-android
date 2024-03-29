package org.mtransit.android.commons.provider;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.sql.SQLJoinBuilder;

import java.util.Locale;

public class GTFSRTSProvider implements MTLog.Loggable {

	private static final String TAG = GTFSRTSProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	protected static final int ROUTES = 1;
	protected static final int STOPS = 2;
	protected static final int TRIPS = 3;
	protected static final int ROUTES_TRIPS_STOPS = 4;
	protected static final int ROUTES_TRIPS_STOPS_SEARCH = 5;
	protected static final int ROUTES_TRIPS = 6;
	protected static final int TRIPS_STOPS = 7;

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_PATH, ROUTES);
		uriMatcher.addURI(authority, GTFSProviderContract.TRIP_PATH, TRIPS);
		uriMatcher.addURI(authority, GTFSProviderContract.STOP_PATH, STOPS);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_TRIP_STOP_PATH, ROUTES_TRIPS_STOPS);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_TRIP_STOP_SEARCH_PATH, ROUTES_TRIPS_STOPS_SEARCH);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_TRIP_PATH, ROUTES_TRIPS);
		uriMatcher.addURI(authority, GTFSProviderContract.TRIP_STOP_PATH, TRIPS_STOPS);
	}

	private static final ArrayMap<String, String> ROUTE_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteColumns.T_ROUTE_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteColumns.T_ROUTE_K_SHORT_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteColumns.T_ROUTE_K_LONG_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteColumns.T_ROUTE_K_COLOR);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteColumns.T_ROUTE_K_TYPE);
			}
		}
		ROUTE_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> TRIP_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.TripColumns.T_TRIP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_TYPE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_VALUE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID) //
			.build();

	private static final ArrayMap<String, String> STOP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.StopColumns.T_STOP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.StopColumns.T_STOP_K_CODE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.StopColumns.T_STOP_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.StopColumns.T_STOP_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.StopColumns.T_STOP_K_LNG);
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, GTFSProviderContract.StopColumns.T_STOP_K_ACCESSIBLE);
		}
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ORIGINAL_ID_HASH, GTFSProviderContract.StopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		}
		STOP_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> ROUTE_TRIP_STOP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG);
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ACCESSIBLE);
		}
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		}
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_NO_PICKUP, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_NO_PICKUP);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_TYPE);
			}
		}
		ROUTE_TRIP_STOP_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> ROUTE_TRIP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.RouteTripColumns.T_TRIP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.RouteTripColumns.T_TRIP_K_ROUTE_ID);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_SHORT_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_LONG_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_COLOR);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_TYPE);
			}
		}
		ROUTE_TRIP_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> TRIP_STOP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.TripStopColumns.T_STOP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.TripStopColumns.T_STOP_K_CODE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.TripStopColumns.T_STOP_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.TripStopColumns.T_STOP_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.TripStopColumns.T_STOP_K_LNG);
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, GTFSProviderContract.TripStopColumns.T_STOP_K_ACCESSIBLE);
		}
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ORIGINAL_ID_HASH, GTFSProviderContract.TripStopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		}
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE, GTFSProviderContract.TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_NO_PICKUP, GTFSProviderContract.TripStopColumns.T_TRIP_STOPS_K_NO_PICKUP);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.TripStopColumns.T_TRIP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.TripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.TripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.TripStopColumns.T_TRIP_K_ROUTE_ID);
		TRIP_STOP_PROJECTION_MAP = sb.build();
	}

	public static final String ROUTE_TRIP_TRIP_STOPS_STOP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_STOP) //
			.innerJoin(GTFSProviderDbHelper.T_TRIP_STOPS, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID,//
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_ID) //
			.innerJoin(GTFSProviderDbHelper.T_TRIP, //
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_TRIP_ID,//
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID) //
			.innerJoin(GTFSProviderDbHelper.T_ROUTE, //
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, //
					GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID) //
			.build();

	@SuppressWarnings("unused")
	private static final String TRIP_STOPS_STOP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_TRIP_STOPS) //
			.innerJoin(GTFSProviderDbHelper.T_STOP, //
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_ID, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID) //
			.build();

	private static final String TRIP_TRIP_STOPS_STOP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_STOP) //
			.innerJoin(GTFSProviderDbHelper.T_TRIP_STOPS, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID,//
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_ID) //
			.innerJoin(GTFSProviderDbHelper.T_TRIP, //
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_TRIP_ID,//
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID) //
			.build();

	private static final String ROUTE_TRIP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_TRIP) //
			.innerJoin(GTFSProviderDbHelper.T_ROUTE,//
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, //
					GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID)//
			.build();

	@Nullable
	public static Cursor queryS(@NonNull GTFSProvider provider, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			switch (provider.getURI_MATCHER().match(uri)) {
			case ROUTES:
				qb.setTables(GTFSProviderDbHelper.T_ROUTE);
				qb.setProjectionMap(ROUTE_PROJECTION_MAP);
				break;
			case TRIPS:
				qb.setTables(GTFSProviderDbHelper.T_TRIP);
				qb.setProjectionMap(TRIP_PROJECTION_MAP);
				break;
			case STOPS:
				qb.setTables(GTFSProviderDbHelper.T_STOP);
				qb.setProjectionMap(STOP_PROJECTION_MAP);
				break;
			case ROUTES_TRIPS_STOPS:
				qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
				qb.setProjectionMap(ROUTE_TRIP_STOP_PROJECTION_MAP);
				break;
			case ROUTES_TRIPS_STOPS_SEARCH:
				qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
				qb.setProjectionMap(ROUTE_TRIP_STOP_PROJECTION_MAP);
				appendRouteTripStopSearch(uri, qb);
				break;
			case ROUTES_TRIPS:
				qb.setTables(ROUTE_TRIP_JOIN);
				qb.setProjectionMap(ROUTE_TRIP_PROJECTION_MAP);
				break;
			case TRIPS_STOPS:
				qb.setTables(TRIP_TRIP_STOPS_STOP_JOIN);
				qb.setProjectionMap(TRIP_STOP_PROJECTION_MAP);
				break;
			default:
				return null; // not processed
			}
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = provider.getSortOrder(uri);
			}
			Cursor cursor = qb.query(provider.getReadDB(), projection, selection, selectionArgs, null, null, sortOrder, null);
			if (cursor != null) {
				cursor.setNotificationUri(provider.requireContextCompat().getContentResolver(), uri);
			}
			return cursor;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	private static void appendRouteTripStopSearch(Uri uri, SQLiteQueryBuilder qb) {
		String lastPathSegment = uri.getLastPathSegment() == null ? "" : uri.getLastPathSegment();
		String search = lastPathSegment.toLowerCase(Locale.ENGLISH);
		if (!TextUtils.isEmpty(search)) {
			String[] keywords = search.split(ContentProviderConstants.SEARCH_SPLIT_ON);
			StringBuilder inWhere = new StringBuilder();
			for (String keyword : keywords) {
				if (inWhere.length() > 0) {
					inWhere.append(SqlUtils.AND);
				}
				inWhere.append(SqlUtils.getWhereGroup( //
						SqlUtils.OR, //
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE), keyword),
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME), keyword),
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME), keyword),
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME), keyword)));
			}
			qb.appendWhere(inWhere);
		}
	}

	private static final String ROUTE_SORT_ORDER =
			SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID));
	private static final String TRIP_SORT_ORDER =
			SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID));
	private static final String STOP_SORT_ORDER =
			SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID));
	private static final String ROUTE_TRIP_STOP_SORT_ORDER = SqlUtils.mergeSortOrder(ROUTE_SORT_ORDER, TRIP_SORT_ORDER, STOP_SORT_ORDER);
	private static final String ROUTE_TRIP_SORT_ORDER = SqlUtils.mergeSortOrder(ROUTE_SORT_ORDER, TRIP_SORT_ORDER);
	private static final String TRIP_STOP_SORT_ORDER = SqlUtils.mergeSortOrder(TRIP_SORT_ORDER, STOP_SORT_ORDER);

	@Nullable
	public static String getSortOrderS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ROUTES:
			return ROUTE_SORT_ORDER;
		case TRIPS:
			return TRIP_SORT_ORDER;
		case STOPS:
			return STOP_SORT_ORDER;
		case ROUTES_TRIPS_STOPS:
		case ROUTES_TRIPS_STOPS_SEARCH:
			return ROUTE_TRIP_STOP_SORT_ORDER;
		case TRIPS_STOPS:
			return TRIP_STOP_SORT_ORDER;
		case ROUTES_TRIPS:
			return ROUTE_TRIP_SORT_ORDER;
		default:
			return null; // not processed
		}
	}

	private static final String ROUTE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".route";
	private static final String TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".trip";
	private static final String STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".stop";
	private static final String ROUTE_TRIP_STOP_CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".routetripstop";
	private static final String TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".tripstop";
	private static final String ROUTE_TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".routetrip";

	@Nullable
	public static String getTypeS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ROUTES:
			return ROUTE_CONTENT_TYPE;
		case TRIPS:
			return TRIP_CONTENT_TYPE;
		case STOPS:
			return STOP_CONTENT_TYPE;
		case ROUTES_TRIPS_STOPS:
		case ROUTES_TRIPS_STOPS_SEARCH:
			return ROUTE_TRIP_STOP_CONTENT_TYPE;
		case ROUTES_TRIPS:
			return ROUTE_TRIP_CONTENT_TYPE;
		case TRIPS_STOPS:
			return TRIP_STOP_CONTENT_TYPE;
		default:
			return null; // not processed
		}
	}

}
