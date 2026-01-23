package org.mtransit.android.commons.provider.gtfs;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.util.Pair;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.GTFSProvider;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.android.commons.provider.common.ContentProviderConstants;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.sql.SQLJoinBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class GTFSRDSProvider implements MTLog.Loggable {

	private static final String LOG_TAG = GTFSRDSProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final int ROUTES = 1;
	private static final int STOPS = 2;
	private static final int DIRECTIONS = 3;
	private static final int ROUTES_DIRECTIONS_STOPS = 4;
	private static final int ROUTES_DIRECTIONS_STOPS_SEARCH = 5;
	private static final int ROUTES_DIRECTIONS = 6;
	private static final int DIRECTIONS_STOPS = 7;
	private static final int TRIPS = 8;

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_PATH, ROUTES);
		uriMatcher.addURI(authority, GTFSProviderContract.DIRECTION_PATH, DIRECTIONS);
		uriMatcher.addURI(authority, GTFSProviderContract.STOP_PATH, STOPS);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_DIRECTION_STOP_PATH, ROUTES_DIRECTIONS_STOPS);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_DIRECTION_STOP_SEARCH_PATH, ROUTES_DIRECTIONS_STOPS_SEARCH);
		uriMatcher.addURI(authority, GTFSProviderContract.ROUTE_DIRECTION_PATH, ROUTES_DIRECTIONS);
		uriMatcher.addURI(authority, GTFSProviderContract.DIRECTION_STOP_PATH, DIRECTIONS_STOPS);
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			uriMatcher.addURI(authority, GTFSProviderContract.TRIP_PATH, TRIPS);
		}
	}

	private static final ArrayMap<String, String> ROUTE_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteColumns.T_ROUTE_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteColumns.T_ROUTE_K_SHORT_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteColumns.T_ROUTE_K_LONG_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteColumns.T_ROUTE_K_COLOR);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteColumns.T_ROUTE_K_TYPE);
		ROUTE_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> DIRECTION_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID, GTFSProviderContract.DirectionColumns.T_DIRECTION_K_ID) //
			.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_TYPE, GTFSProviderContract.DirectionColumns.T_DIRECTION_K_HEADSIGN_TYPE) //
			.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_VALUE, GTFSProviderContract.DirectionColumns.T_DIRECTION_K_HEADSIGN_VALUE) //
			.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ROUTE_ID, GTFSProviderContract.DirectionColumns.T_DIRECTION_K_ROUTE_ID) //
			.build();

	private static final ArrayMap<String, String> STOP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.StopColumns.T_STOP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.StopColumns.T_STOP_K_CODE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.StopColumns.T_STOP_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.StopColumns.T_STOP_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.StopColumns.T_STOP_K_LNG);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, GTFSProviderContract.StopColumns.T_STOP_K_ACCESSIBLE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ORIGINAL_ID_HASH, GTFSProviderContract.StopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		STOP_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> ROUTE_DIRECTION_STOP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
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
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_TYPE);
		ROUTE_DIRECTION_STOP_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> ROUTE_DIRECTION_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_TYPE, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_HEADSIGN_TYPE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_VALUE, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_HEADSIGN_VALUE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ROUTE_ID, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_ROUTE_ID);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_SHORT_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_LONG_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_COLOR, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_COLOR);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
		sb.appendTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_TYPE, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_TYPE);
		ROUTE_DIRECTION_PROJECTION_MAP = sb.build();
	}

	private static final ArrayMap<String, String> DIRECTION_STOP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, GTFSProviderContract.DirectionStopColumns.T_STOP_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE, GTFSProviderContract.DirectionStopColumns.T_STOP_K_CODE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME, GTFSProviderContract.DirectionStopColumns.T_STOP_K_NAME);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LAT, GTFSProviderContract.DirectionStopColumns.T_STOP_K_LAT);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_LNG, GTFSProviderContract.DirectionStopColumns.T_STOP_K_LNG);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ACCESSIBLE, GTFSProviderContract.DirectionStopColumns.T_STOP_K_ACCESSIBLE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ORIGINAL_ID_HASH, GTFSProviderContract.DirectionStopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_STOP_SEQUENCE, GTFSProviderContract.DirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_NO_PICKUP, GTFSProviderContract.DirectionStopColumns.T_DIRECTION_STOPS_K_NO_PICKUP);
		//
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID, GTFSProviderContract.DirectionStopColumns.T_DIRECTION_K_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_TYPE, GTFSProviderContract.DirectionStopColumns.T_DIRECTION_K_HEADSIGN_TYPE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_HEADSIGN_VALUE, GTFSProviderContract.DirectionStopColumns.T_DIRECTION_K_HEADSIGN_VALUE);
		sb.appendTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ROUTE_ID, GTFSProviderContract.DirectionStopColumns.T_DIRECTION_K_ROUTE_ID);
		DIRECTION_STOP_PROJECTION_MAP = sb.build();
	}

	static final String ROUTE_DIRECTION_DIRECTION_STOPS_STOP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_STOP) //
			.innerJoin(GTFSProviderDbHelper.T_DIRECTION_STOPS, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, //
					GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_STOP_ID) //
			.innerJoin(GTFSProviderDbHelper.T_DIRECTION, //
					GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_DIRECTION_ID, //
					GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID) //
			.innerJoin(GTFSProviderDbHelper.T_ROUTE, //
					GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ROUTE_ID, //
					GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID) //
			.build();

	@SuppressWarnings("unused")
	private static final String DIRECTION_STOPS_STOP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_DIRECTION_STOPS) //
			.innerJoin(GTFSProviderDbHelper.T_STOP, //
					GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_STOP_ID, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID) //
			.build();

	private static final String DIRECTION_DIRECTION_STOPS_STOP_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_STOP) //
			.innerJoin(GTFSProviderDbHelper.T_DIRECTION_STOPS, //
					GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID, //
					GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_STOP_ID) //
			.innerJoin(GTFSProviderDbHelper.T_DIRECTION, //
					GTFSProviderDbHelper.T_DIRECTION_STOPS, GTFSProviderDbHelper.T_DIRECTION_STOPS_K_DIRECTION_ID, //
					GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID) //
			.build();

	private static final String ROUTE_DIRECTION_JOIN = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_DIRECTION) //
			.innerJoin(GTFSProviderDbHelper.T_ROUTE, //
					GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ROUTE_ID, //
					GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID)//
			.build();

	private static final String TRIP_TRIP_IDS_SERVICE_IDS_JOIN;

	static {
		final SQLJoinBuilder bd = SQLJoinBuilder.getNew(GTFSProviderDbHelper.T_TRIP);
		if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
			bd.innerJoin(GTFSProviderDbHelper.T_TRIP_IDS, //
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT, //
					GTFSProviderDbHelper.T_TRIP_IDS, GTFSProviderDbHelper.T_TRIP_IDS_K_ID);
		}
		if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
			bd.innerJoin(GTFSProviderDbHelper.T_SERVICE_IDS, //
					GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_SERVICE_ID_OR_INT, //
					GTFSProviderDbHelper.T_SERVICE_IDS, GTFSProviderDbHelper.T_SERVICE_IDS_K_ID);
		}
		TRIP_TRIP_IDS_SERVICE_IDS_JOIN = bd.build();
	}

	private static final ArrayMap<String, String> TRIP_PROJECTION_MAP;

	static {
		final SqlUtils.ProjectionMapBuilder sb = SqlUtils.ProjectionMapBuilder.getNew();
		if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP_IDS, GTFSProviderDbHelper.T_TRIP_IDS_K_ID, GTFSProviderContract.TripColumns.T_TRIP_K_TRIP_ID);
		} else {
			sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT, GTFSProviderContract.TripColumns.T_TRIP_K_TRIP_ID);
		}
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID);
		sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_DIRECTION_ID, GTFSProviderContract.TripColumns.T_TRIP_K_DIRECTION_ID);
		if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
			sb.appendTableColumn(GTFSProviderDbHelper.T_SERVICE_IDS, GTFSProviderDbHelper.T_SERVICE_IDS_K_ID, GTFSProviderContract.TripColumns.T_TRIP_K_SERVICE_ID);
		} else {
			sb.appendTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_SERVICE_ID_OR_INT, GTFSProviderContract.TripColumns.T_TRIP_K_SERVICE_ID);
		}
		TRIP_PROJECTION_MAP = sb.build();
	}

	@Nullable
	public static Cursor queryS(@NonNull GTFSProvider provider, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			switch (provider.getURI_MATCHER().match(uri)) {
			case ROUTES:
				qb.setTables(GTFSProviderDbHelper.T_ROUTE);
				qb.setProjectionMap(ROUTE_PROJECTION_MAP);
				break;
			case DIRECTIONS:
				qb.setTables(GTFSProviderDbHelper.T_DIRECTION);
				qb.setProjectionMap(DIRECTION_PROJECTION_MAP);
				break;
			case STOPS:
				qb.setTables(GTFSProviderDbHelper.T_STOP);
				qb.setProjectionMap(STOP_PROJECTION_MAP);
				break;
			case ROUTES_DIRECTIONS_STOPS:
				qb.setTables(ROUTE_DIRECTION_DIRECTION_STOPS_STOP_JOIN);
				qb.setProjectionMap(ROUTE_DIRECTION_STOP_PROJECTION_MAP);
				break;
			case ROUTES_DIRECTIONS_STOPS_SEARCH:
				qb.setTables(ROUTE_DIRECTION_DIRECTION_STOPS_STOP_JOIN);
				qb.setProjectionMap(ROUTE_DIRECTION_STOP_PROJECTION_MAP);
				appendRouteDirectionStopSearch(uri, qb);
				break;
			case ROUTES_DIRECTIONS:
				qb.setTables(ROUTE_DIRECTION_JOIN);
				qb.setProjectionMap(ROUTE_DIRECTION_PROJECTION_MAP);
				break;
			case DIRECTIONS_STOPS:
				qb.setTables(DIRECTION_DIRECTION_STOPS_STOP_JOIN);
				qb.setProjectionMap(DIRECTION_STOP_PROJECTION_MAP);
				break;
			case TRIPS:
				if (FeatureFlags.F_EXPORT_TRIP_ID) {
					qb.setTables(TRIP_TRIP_IDS_SERVICE_IDS_JOIN);
					qb.setProjectionMap(TRIP_PROJECTION_MAP);
					break;
				} else {
					return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
				}
			default:
				return null; // not processed
			}
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = provider.getSortOrder(uri);
			}
			final Cursor cursor = qb.query(provider.getReadDB(), projection, selection, selectionArgs, null, null, sortOrder, null);
			if (cursor != null) {
				cursor.setNotificationUri(provider.requireContextCompat().getContentResolver(), uri);
			}
			return cursor;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	private static void appendRouteDirectionStopSearch(Uri uri, SQLiteQueryBuilder qb) {
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
						SqlUtils.getLikeContains(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_CODE), keyword),
						SqlUtils.getLikeContains(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_SHORT_NAME), keyword),
						SqlUtils.getLikeContains(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_LONG_NAME), keyword),
						SqlUtils.getLikeContains(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_NAME), keyword)));
			}
			qb.appendWhere(inWhere);
		}
	}

	@SuppressWarnings("unused")
	private static final String[] PROJECTION_TRIPS;

	static {
		ArrayList<String> list = new ArrayList<>();
		list.add(GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID);
		list.add(GTFSProviderDbHelper.T_TRIP_K_DIRECTION_ID);
		list.add(GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT);
		list.add(GTFSProviderDbHelper.T_TRIP_K_SERVICE_ID_OR_INT);
		PROJECTION_TRIPS = list.toArray(new String[0]);
	}

	@SuppressWarnings("unused")
	@NonNull
	public static HashSet<String> findDirectionTripIdOrIntList(@NonNull GTFSProvider provider, long directionId) {
		final HashSet<String> routeTripIdsOrInt = new HashSet<>();
		Cursor cursor = null;
		try {
			final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSProviderDbHelper.T_TRIP);
			cursor = qb.query(provider.getReadDB(),
					new String[]{GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT},
					SqlUtils.getWhereEquals(GTFSProviderDbHelper.T_TRIP_K_DIRECTION_ID, directionId),
					null,
					null,
					null,
					null,
					null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						final String tripIdOrInt = CursorExtKt.getString(cursor, GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT);
						if (!TextUtils.isEmpty(tripIdOrInt)) {
							routeTripIdsOrInt.add(tripIdOrInt);
						}
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return routeTripIdsOrInt;
	}

	/**
	 * @return should only return 1 route+direction for a trip ID
	 */
	@SuppressWarnings("unused")
	@NonNull
	public static Map<String, Pair<Long, Long>> findTripRouteAndDirectionIds(@NonNull GTFSProvider provider, @NonNull Collection<String> tripIdOrInts) {
		final Map<String, Pair<Long, Long>> tripRouteDirectionIds = new HashMap<>();
		Cursor cursor = null;
		try {
			final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSProviderDbHelper.T_TRIP);
			cursor = qb.query(provider.getReadDB(),
					new String[]{GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID, GTFSProviderDbHelper.T_TRIP_K_DIRECTION_ID},
					SqlUtils.getWhereInString(GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT, tripIdOrInts),
					null,
					null,
					null,
					null,
					null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						final String tripIdOrInt = CursorExtKt.getString(cursor, GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT);
						final Long routeId = CursorExtKt.getLong(cursor, GTFSProviderDbHelper.T_TRIP_K_ROUTE_ID);
						final Long directionId = CursorExtKt.optLong(cursor, GTFSProviderDbHelper.T_TRIP_K_DIRECTION_ID, null);
						tripRouteDirectionIds.put(tripIdOrInt, new Pair<>(routeId, directionId));
					}
					while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return tripRouteDirectionIds;
	}

	private static final String ROUTE_SORT_ORDER =
			SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_ROUTE, GTFSProviderDbHelper.T_ROUTE_K_ID));
	private static final String DIRECTION_SORT_ORDER =
			SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_DIRECTION, GTFSProviderDbHelper.T_DIRECTION_K_ID));
	private static final String STOP_SORT_ORDER =
			SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_STOP, GTFSProviderDbHelper.T_STOP_K_ID));
	private static final String ROUTE_DIRECTION_STOP_SORT_ORDER = SqlUtils.mergeSortOrder(ROUTE_SORT_ORDER, DIRECTION_SORT_ORDER, STOP_SORT_ORDER);
	private static final String ROUTE_DIRECTION_SORT_ORDER = SqlUtils.mergeSortOrder(ROUTE_SORT_ORDER, DIRECTION_SORT_ORDER);
	private static final String DIRECTION_STOP_SORT_ORDER = SqlUtils.mergeSortOrder(DIRECTION_SORT_ORDER, STOP_SORT_ORDER);
	private static final String TRIP_SORT_ORDER = SqlUtils.getSortOrderAscending(SqlUtils.getTableColumn(GTFSProviderDbHelper.T_TRIP, GTFSProviderDbHelper.T_TRIP_K_TRIP_ID_OR_INT));

	@Nullable
	public static String getSortOrderS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ROUTES:
			return ROUTE_SORT_ORDER;
		case DIRECTIONS:
			return DIRECTION_SORT_ORDER;
		case STOPS:
			return STOP_SORT_ORDER;
		case ROUTES_DIRECTIONS_STOPS:
		case ROUTES_DIRECTIONS_STOPS_SEARCH:
			return ROUTE_DIRECTION_STOP_SORT_ORDER;
		case DIRECTIONS_STOPS:
			return DIRECTION_STOP_SORT_ORDER;
		case ROUTES_DIRECTIONS:
			return ROUTE_DIRECTION_SORT_ORDER;
		case TRIPS:
			if (FeatureFlags.F_EXPORT_TRIP_ID) {
				return TRIP_SORT_ORDER;
			}
		default:
			return null; // not processed
		}
	}

	// do not change to avoid breaking changes
	private static final String ROUTE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".route";
	private static final String DIRECTION_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".trip"; // do not change to avoid breaking changes
	private static final String STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".stop";
	private static final String ROUTE_DIRECTION_STOP_CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".routetripstop"; // do not change to avoid breaking changes
	private static final String DIRECTION_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".tripstop"; // do not change to avoid breaking changes
	private static final String ROUTE_DIRECTION_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".routetrip"; // do not change to avoid breaking changes
	private static final String TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + Constants.MAIN_APP_PACKAGE_NAME + ".path"; // do not change to avoid breaking changes

	@Nullable
	public static String getTypeS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ROUTES:
			return ROUTE_CONTENT_TYPE;
		case DIRECTIONS:
			return DIRECTION_CONTENT_TYPE;
		case STOPS:
			return STOP_CONTENT_TYPE;
		case ROUTES_DIRECTIONS_STOPS:
		case ROUTES_DIRECTIONS_STOPS_SEARCH:
			return ROUTE_DIRECTION_STOP_CONTENT_TYPE;
		case ROUTES_DIRECTIONS:
			return ROUTE_DIRECTION_CONTENT_TYPE;
		case DIRECTIONS_STOPS:
			return DIRECTION_STOP_CONTENT_TYPE;
		case TRIPS:
			if (FeatureFlags.F_EXPORT_TRIP_ID) {
				return TRIP_CONTENT_TYPE;
			}
		default:
			return null; // not processed
		}
	}
}
