package org.mtransit.android.commons.provider;

import android.provider.BaseColumns;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.commons.FeatureFlags;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public interface GTFSProviderContract {

	String POI_FILTER_EXTRA_NO_PICKUP = "descentOnly";

	String ROUTE_PATH = "route";
	String DIRECTION_PATH = "trip"; // do not change to avoid breaking compat w/ old modules
	String STOP_PATH = "stop";
	String ROUTE_LOGO_PATH = "route/logo";
	String ROUTE_DIRECTION_STOP_PATH = "route/trip/stop"; // do not change to avoid breaking compat w/ old modules
	String ROUTE_DIRECTION_STOP_SEARCH_PATH = "route/trip/stop/*"; // do not change to avoid breaking compat w/ old modules
	String ROUTE_DIRECTION_PATH = "route/trip"; // do not change to avoid breaking compat w/ old modules
	String DIRECTION_STOP_PATH = "trip/stop"; // do not change to avoid breaking compat w/ old modules

	@NonNull
	static String[] makePROJECTION_ROUTE_DIRECTION_STOP() {
		ArrayList<String> projection = new ArrayList<>();
		projection.add(RouteDirectionStopColumns.T_ROUTE_K_ID);
		projection.add(RouteDirectionStopColumns.T_ROUTE_K_SHORT_NAME);
		projection.add(RouteDirectionStopColumns.T_ROUTE_K_LONG_NAME);
		projection.add(RouteDirectionStopColumns.T_ROUTE_K_COLOR);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			projection.add(RouteDirectionStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				projection.add(RouteDirectionStopColumns.T_ROUTE_K_TYPE);
			}
		}
		//
		projection.add(RouteDirectionStopColumns.T_DIRECTION_K_ID);
		projection.add(RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_TYPE);
		projection.add(RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_VALUE);
		projection.add(RouteDirectionStopColumns.T_DIRECTION_K_ROUTE_ID);
		//
		projection.add(RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE);
		projection.add(RouteDirectionStopColumns.T_DIRECTION_STOPS_K_NO_PICKUP);
		//
		projection.add(RouteDirectionStopColumns.T_STOP_K_ID);
		projection.add(RouteDirectionStopColumns.T_STOP_K_CODE);
		projection.add(RouteDirectionStopColumns.T_STOP_K_NAME);
		projection.add(RouteDirectionStopColumns.T_STOP_K_LAT);
		projection.add(RouteDirectionStopColumns.T_STOP_K_LNG);
		projection.add(RouteDirectionStopColumns.T_STOP_K_ACCESSIBLE);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			projection.add(RouteDirectionStopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		}
		return projection.toArray(new String[0]);
	}

	String[] PROJECTION_ROUTE_DIRECTION_STOP = makePROJECTION_ROUTE_DIRECTION_STOP();

	@NonNull
	static String[] makePROJECTION_ROUTE() {
		ArrayList<String> projection = new ArrayList<>();
		projection.add(RouteColumns.T_ROUTE_K_ID);
		projection.add(RouteColumns.T_ROUTE_K_SHORT_NAME);
		projection.add(RouteColumns.T_ROUTE_K_LONG_NAME);
		projection.add(RouteColumns.T_ROUTE_K_COLOR);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			projection.add(RouteColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				projection.add(RouteColumns.T_ROUTE_K_TYPE);
			}
		}
		return projection.toArray(new String[0]);
	}

	String[] PROJECTION_ROUTE = makePROJECTION_ROUTE();

	String[] PROJECTION_DIRECTION = new String[]{
			DirectionColumns.T_DIRECTION_K_ID,
			DirectionColumns.T_DIRECTION_K_HEADSIGN_TYPE,
			DirectionColumns.T_DIRECTION_K_HEADSIGN_VALUE,
			DirectionColumns.T_DIRECTION_K_ROUTE_ID
	};

	String[] PROJECTION_RDS_POI = ArrayUtils.addAllNonNull(POIProvider.PROJECTION_POI, PROJECTION_ROUTE_DIRECTION_STOP);

	class RouteColumns {
		public static final String T_ROUTE_K_ID = BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = "short_name";
		public static final String T_ROUTE_K_LONG_NAME = "long_name";
		public static final String T_ROUTE_K_COLOR = "color";
		public static final String T_ROUTE_K_ORIGINAL_ID_HASH = "o_id_hash";
		public static final String T_ROUTE_K_TYPE = "type";
	}

	class RouteDirectionColumns {
		private static final String T_ROUTE = "route";
		public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
		public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
		public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
		public static final String T_ROUTE_K_ORIGINAL_ID_HASH = T_ROUTE + "_" + "o_id_hash";
		public static final String T_ROUTE_K_TYPE = T_ROUTE + "_" + "type";
		private static final String T_DIRECTION = "trip"; // do not change to avoid breaking compat w/ old modules
		public static final String T_DIRECTION_K_ID = T_DIRECTION + BaseColumns._ID;
		public static final String T_DIRECTION_K_HEADSIGN_TYPE = T_DIRECTION + "_" + "headsign_type";
		public static final String T_DIRECTION_K_HEADSIGN_VALUE = T_DIRECTION + "_" + "headsign_value";
		public static final String T_DIRECTION_K_ROUTE_ID = T_DIRECTION + "_" + "route_id";
	}

	class RouteDirectionStopColumns {
		private static final String T_ROUTE = "route";
		public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
		public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
		public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
		public static final String T_ROUTE_K_ORIGINAL_ID_HASH = T_ROUTE + "_" + "o_id_hash";
		public static final String T_ROUTE_K_TYPE = T_ROUTE + "_" + "type";
		private static final String T_DIRECTION = "trip"; // do not change to avoid breaking compat w/ old modules
		public static final String T_DIRECTION_K_ID = T_DIRECTION + BaseColumns._ID;
		public static final String T_DIRECTION_K_HEADSIGN_TYPE = T_DIRECTION + "_" + "headsign_type";
		public static final String T_DIRECTION_K_HEADSIGN_VALUE = T_DIRECTION + "_" + "headsign_value";
		public static final String T_DIRECTION_K_ROUTE_ID = T_DIRECTION + "_" + "route_id";
		private static final String T_STOP = "stop";
		public static final String T_STOP_K_ID = T_STOP + BaseColumns._ID;
		public static final String T_STOP_K_CODE = T_STOP + "_" + "code";
		public static final String T_STOP_K_NAME = T_STOP + "_" + "name";
		public static final String T_STOP_K_LAT = T_STOP + "_" + "lat";
		public static final String T_STOP_K_LNG = T_STOP + "_" + "lng";
		public static final String T_STOP_K_ACCESSIBLE = T_STOP + "_" + "a11y";
		public static final String T_STOP_K_ORIGINAL_ID_HASH = T_STOP + "_" + "o_id_hash";
		private static final String T_DIRECTION_STOPS = "trip_stops"; // do not change to avoid breaking compat w/ old modules
		public static final String T_DIRECTION_STOPS_K_STOP_SEQUENCE = T_DIRECTION_STOPS + "_" + "stop_sequence";
		public static final String T_DIRECTION_STOPS_K_NO_PICKUP = T_DIRECTION_STOPS + "_" + "decent_only";
	}

	class StopColumns {
		public static final String T_STOP_K_ID = BaseColumns._ID;
		public static final String T_STOP_K_CODE = "code";
		public static final String T_STOP_K_NAME = "name";
		public static final String T_STOP_K_LAT = "lat";
		public static final String T_STOP_K_LNG = "lng";
		public static final String T_STOP_K_ACCESSIBLE = "a11y";
		public static final String T_STOP_K_ORIGINAL_ID_HASH = "o_id_hash";
	}

	class DirectionColumns {
		public static final String T_DIRECTION_K_ID = BaseColumns._ID;
		public static final String T_DIRECTION_K_HEADSIGN_TYPE = "headsign_type";
		public static final String T_DIRECTION_K_HEADSIGN_VALUE = "headsign_value";
		public static final String T_DIRECTION_K_ROUTE_ID = "route_id";
	}

	class DirectionStopColumns {
		private static final String T_DIRECTION = "trip"; // do not change to avoid breaking compat w/ old modules
		public static final String T_DIRECTION_K_ID = T_DIRECTION + BaseColumns._ID;
		public static final String T_DIRECTION_K_HEADSIGN_TYPE = T_DIRECTION + "_" + "headsign_type";
		public static final String T_DIRECTION_K_HEADSIGN_VALUE = T_DIRECTION + "_" + "headsign_value";
		public static final String T_DIRECTION_K_ROUTE_ID = T_DIRECTION + "_" + "route_id";
		private static final String T_STOP = "stop";
		public static final String T_STOP_K_ID = T_STOP + BaseColumns._ID;
		public static final String T_STOP_K_CODE = T_STOP + "_" + "code";
		public static final String T_STOP_K_NAME = T_STOP + "_" + "name";
		public static final String T_STOP_K_LAT = T_STOP + "_" + "lat";
		public static final String T_STOP_K_LNG = T_STOP + "_" + "lng";
		public static final String T_STOP_K_ACCESSIBLE = T_STOP + "_" + "a11y";
		public static final String T_STOP_K_ORIGINAL_ID_HASH = T_STOP + "_" + "o_id_hash";
		private static final String T_DIRECTION_STOPS = "trip_stops"; // do not change to avoid breaking compat w/ old modules
		public static final String T_DIRECTION_STOPS_K_STOP_SEQUENCE = T_DIRECTION_STOPS + "_" + "stop_sequence";
		public static final String T_DIRECTION_STOPS_K_NO_PICKUP = T_DIRECTION_STOPS + "_" + "decent_only";
	}
}
