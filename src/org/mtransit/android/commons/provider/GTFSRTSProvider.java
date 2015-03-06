package org.mtransit.android.commons.provider;

import java.util.HashMap;
import java.util.Locale;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class GTFSRTSProvider implements MTLog.Loggable {

	private static final String TAG = GTFSRTSProvider.class.getSimpleName();

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

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_PATH, ROUTES);
		uriMatcher.addURI(authority, GTFSProviderContract.TRIP_PATH, TRIPS);
		uriMatcher.addURI(authority, GTFSProviderContract.STOP_PATH, STOPS);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_TRIP_STOP_PATH, ROUTES_TRIPS_STOPS);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_TRIP_STOP_SEARCH_PATH, ROUTES_TRIPS_STOPS_SEARCH);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_TRIP_PATH, ROUTES_TRIPS);
		uriMatcher.addURI(authority, GTFSProviderContract.TRIP_STOP_PATH, TRIPS_STOPS);
	}

	// @formatter:off
	private static final HashMap<String, String> ROUTE_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteColumns.T_ROUTE_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteColumns.T_ROUTE_K_SHORT_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteColumns.T_ROUTE_K_LONG_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteColumns.T_ROUTE_K_COLOR) //
			.build();

	private static final HashMap<String, String> TRIP_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.TripColumns.T_TRIP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_TYPE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_VALUE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID) //
			.build();

	private static final HashMap<String, String> STOP_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.StopColumns.T_STOP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.StopColumns.T_STOP_K_CODE) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.StopColumns.T_STOP_K_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.StopColumns.T_STOP_K_LAT) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.StopColumns.T_STOP_K_LNG) //
			.build();

	private static final HashMap<String, String> ROUTE_TRIP_STOP_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG) //
			//
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_DESCENT_ONLY, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_DESCENT_ONLY) //
			//
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID) //
			//
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR) //
			.build();

	private static final HashMap<String, String> ROUTE_TRIP_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.RouteTripColumns.T_TRIP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.RouteTripColumns.T_TRIP_K_ROUTE_ID) //
			//
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_SHORT_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_LONG_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteTripColumns.T_ROUTE_K_COLOR) //
			.build();

	private static final HashMap<String, String> TRIP_STOP_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.TripStopColumns.T_STOP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.TripStopColumns.T_STOP_K_CODE) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.TripStopColumns.T_STOP_K_NAME) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.TripStopColumns.T_STOP_K_LAT) //
			.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.TripStopColumns.T_STOP_K_LNG) //
			//
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE, GTFSProviderContract.TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_DESCENT_ONLY, GTFSProviderContract.TripStopColumns.T_TRIP_STOPS_K_DESCENT_ONLY) //
			//
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID, GTFSProviderContract.TripStopColumns.T_TRIP_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_TYPE, GTFSProviderContract.TripStopColumns.T_TRIP_K_HEADSIGN_TYPE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_HEADSIGN_VALUE, GTFSProviderContract.TripStopColumns.T_TRIP_K_HEADSIGN_VALUE) //
			.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.TripStopColumns.T_TRIP_K_ROUTE_ID) //
			.build();
	// @formatter:on

	public static final String ROUTE_TRIP_TRIP_STOPS_STOP_JOIN = SqlUtils.JoinBuilder.getNew(GTFSProviderDbHelper.T_STOP) //
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
	private static final String TRIP_STOPS_STOP_JOIN = SqlUtils.JoinBuilder.getNew(GTFSProviderDbHelper.T_TRIP_STOPS) //
			.innerJoin(GTFSProviderDbHelper.T_STOP, //
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_ID, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID) //
			.build();

	private static final String TRIP_TRIP_STOPS_STOP_JOIN = SqlUtils.JoinBuilder.getNew(GTFSProviderDbHelper.T_STOP) //
			.innerJoin(GTFSProviderDbHelper.T_TRIP_STOPS, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID,//
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_STOP_ID) //
			.innerJoin(GTFSProviderDbHelper.T_TRIP, //
					GTFSProviderDbHelper.T_TRIP_STOPS, GTFSProviderDbHelper.T_TRIP_STOPS_K_TRIP_ID,//
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ID) //
			.build();

	private static final String ROUTE_TRIP_JOIN = SqlUtils.JoinBuilder.getNew(GTFSProviderDbHelper.T_TRIP) //
			.innerJoin(GTFSProviderDbHelper.T_ROUTE,//
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, //
					GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID)//
			.build();

	public static Cursor queryS(GTFSProvider provider, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
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
			db = provider.getDBHelper().getReadableDatabase();
			cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder, null);
			if (cursor != null) {
				cursor.setNotificationUri(provider.getContext().getContentResolver(), uri);
			}
			return cursor;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while resolving query '%s'!", uri);
			return null;
		} finally {
			SqlUtils.closeQuietly(db);
		}
	}

	private static void appendRouteTripStopSearch(Uri uri, SQLiteQueryBuilder qb) {
		String search = uri.getLastPathSegment().toLowerCase(Locale.ENGLISH);
		if (!TextUtils.isEmpty(search)) {
			String[] keywords = search.split(ContentProviderConstants.SEARCH_SPLIT_ON);
			StringBuilder inWhere = new StringBuilder();
			for (String keyword : keywords) {
				if (inWhere.length() > 0) {
					inWhere.append(SqlUtils.AND);
				}
				inWhere.append(SqlUtils.getWhereGroup(
						SqlUtils.OR, //
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE), keyword),
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME), keyword),
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME), keyword),
						SqlUtils.getLike(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME), keyword)));
			}
			qb.appendWhere(inWhere);
		}
	}

	private static final String ROUTE_SORT_ORDER = SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE,
			GTFSProviderDbHelper.T_ROUTE_K_ID));
	private static final String TRIP_SORT_ORDER = SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_TRIP,
			GTFSProviderDbHelper.T_TRIP_K_ID));
	private static final String STOP_SORT_ORDER = SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP,
			GTFSProviderDbHelper.T_STOP_K_ID));
	private static final String ROUTE_TRIP_STOP_SORT_ORDER = SqlUtils.mergeSortOrder(ROUTE_SORT_ORDER, TRIP_SORT_ORDER, STOP_SORT_ORDER);
	private static final String ROUTE_TRIP_SORT_ORDER = SqlUtils.mergeSortOrder(ROUTE_SORT_ORDER, TRIP_SORT_ORDER);
	private static final String TRIP_STOP_SORT_ORDER = SqlUtils.mergeSortOrder(TRIP_SORT_ORDER, STOP_SORT_ORDER);

	public static String getSortOrderS(GTFSProvider provider, Uri uri) {
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
	private static final String ROUTE_TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME
			+ ".routetripstop";
	private static final String TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".tripstop";
	private static final String ROUTE_TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".routetrip";

	public static String getTypeS(GTFSProvider provider, Uri uri) {
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
