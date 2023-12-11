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
	String TRIP_PATH = "trip";
	String STOP_PATH = "stop";
	String ROUTE_LOGO_PATH = "route/logo";
	String ROUTE_TRIP_STOP_PATH = "route/trip/stop";
	String ROUTE_TRIP_STOP_SEARCH_PATH = "route/trip/stop/*";
	String ROUTE_TRIP_PATH = "route/trip";
	String TRIP_STOP_PATH = "trip/stop";

	@NonNull
	static String[] makePROJECTION_ROUTE_TRIP_STOP() {
		ArrayList<String> projection = new ArrayList<>();
		projection.add(RouteTripStopColumns.T_ROUTE_K_ID);
		projection.add(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME);
		projection.add(RouteTripStopColumns.T_ROUTE_K_LONG_NAME);
		projection.add(RouteTripStopColumns.T_ROUTE_K_COLOR);
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			projection.add(RouteTripStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH);
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				projection.add(RouteTripStopColumns.T_ROUTE_K_TYPE);
			}
		}
		//
		projection.add(RouteTripStopColumns.T_TRIP_K_ID);
		projection.add(RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		projection.add(RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		projection.add(RouteTripStopColumns.T_TRIP_K_ROUTE_ID);
		//
		projection.add(RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		projection.add(RouteTripStopColumns.T_TRIP_STOPS_K_NO_PICKUP);
		//
		projection.add(RouteTripStopColumns.T_STOP_K_ID);
		projection.add(RouteTripStopColumns.T_STOP_K_CODE);
		projection.add(RouteTripStopColumns.T_STOP_K_NAME);
		projection.add(RouteTripStopColumns.T_STOP_K_LAT);
		projection.add(RouteTripStopColumns.T_STOP_K_LNG);
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			projection.add(RouteTripStopColumns.T_STOP_K_ACCESSIBLE);
		}
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			projection.add(RouteTripStopColumns.T_STOP_K_ORIGINAL_ID_HASH);
		}
		return projection.toArray(new String[0]);
	}

	String[] PROJECTION_ROUTE_TRIP_STOP = makePROJECTION_ROUTE_TRIP_STOP();

	@SuppressWarnings("unused")
	String[] PROJECTION_ROUTE =
			new String[]{RouteColumns.T_ROUTE_K_ID, RouteColumns.T_ROUTE_K_SHORT_NAME, RouteColumns.T_ROUTE_K_LONG_NAME, RouteColumns.T_ROUTE_K_COLOR};

	@SuppressWarnings("unused")
	String[] PROJECTION_TRIP =
			new String[]{TripColumns.T_TRIP_K_ID, TripColumns.T_TRIP_K_HEADSIGN_TYPE, TripColumns.T_TRIP_K_HEADSIGN_VALUE, TripColumns.T_TRIP_K_ROUTE_ID};

	String[] PROJECTION_RTS_POI = ArrayUtils.addAllNonNull(POIProvider.PROJECTION_POI, PROJECTION_ROUTE_TRIP_STOP);

	class RouteColumns {
		public static final String T_ROUTE_K_ID = BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = "short_name";
		public static final String T_ROUTE_K_LONG_NAME = "long_name";
		public static final String T_ROUTE_K_COLOR = "color";
		public static final String T_ROUTE_K_ORIGINAL_ID_HASH = "o_id_hash";
		public static final String T_ROUTE_K_TYPE = "type";
	}

	class RouteTripColumns {
		private static final String T_ROUTE = "route";
		public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
		public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
		public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
		public static final String T_ROUTE_K_ORIGINAL_ID_HASH = T_ROUTE + "_" + "o_id_hash";
		public static final String T_ROUTE_K_TYPE = T_ROUTE + "_" + "type";
		private static final String T_TRIP = "trip";
		public static final String T_TRIP_K_ID = T_TRIP + BaseColumns._ID;
		public static final String T_TRIP_K_HEADSIGN_TYPE = T_TRIP + "_" + "headsign_type";
		public static final String T_TRIP_K_HEADSIGN_VALUE = T_TRIP + "_" + "headsign_value";
		public static final String T_TRIP_K_ROUTE_ID = T_TRIP + "_" + "route_id";
	}

	class RouteTripStopColumns {
		private static final String T_ROUTE = "route";
		public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
		public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
		public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
		public static final String T_ROUTE_K_ORIGINAL_ID_HASH = T_ROUTE + "_" + "o_id_hash";
		public static final String T_ROUTE_K_TYPE = T_ROUTE + "_" + "type";
		private static final String T_TRIP = "trip";
		public static final String T_TRIP_K_ID = T_TRIP + BaseColumns._ID;
		public static final String T_TRIP_K_HEADSIGN_TYPE = T_TRIP + "_" + "headsign_type";
		public static final String T_TRIP_K_HEADSIGN_VALUE = T_TRIP + "_" + "headsign_value";
		public static final String T_TRIP_K_ROUTE_ID = T_TRIP + "_" + "route_id";
		private static final String T_STOP = "stop";
		public static final String T_STOP_K_ID = T_STOP + BaseColumns._ID;
		public static final String T_STOP_K_CODE = T_STOP + "_" + "code";
		public static final String T_STOP_K_NAME = T_STOP + "_" + "name";
		public static final String T_STOP_K_LAT = T_STOP + "_" + "lat";
		public static final String T_STOP_K_LNG = T_STOP + "_" + "lng";
		public static final String T_STOP_K_ACCESSIBLE = T_STOP + "_" + "a11y";
		public static final String T_STOP_K_ORIGINAL_ID_HASH = T_STOP + "_" + "o_id_hash";
		private static final String T_TRIP_STOPS = "trip_stops";
		public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = T_TRIP_STOPS + "_" + "stop_sequence";
		public static final String T_TRIP_STOPS_K_NO_PICKUP = T_TRIP_STOPS + "_" + "decent_only";
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

	class TripColumns {
		public static final String T_TRIP_K_ID = BaseColumns._ID;
		public static final String T_TRIP_K_HEADSIGN_TYPE = "headsign_type";
		public static final String T_TRIP_K_HEADSIGN_VALUE = "headsign_value";
		public static final String T_TRIP_K_ROUTE_ID = "route_id";
	}

	class TripStopColumns {
		private static final String T_TRIP = "trip";
		public static final String T_TRIP_K_ID = T_TRIP + BaseColumns._ID;
		public static final String T_TRIP_K_HEADSIGN_TYPE = T_TRIP + "_" + "headsign_type";
		public static final String T_TRIP_K_HEADSIGN_VALUE = T_TRIP + "_" + "headsign_value";
		public static final String T_TRIP_K_ROUTE_ID = T_TRIP + "_" + "route_id";
		private static final String T_STOP = "stop";
		public static final String T_STOP_K_ID = T_STOP + BaseColumns._ID;
		public static final String T_STOP_K_CODE = T_STOP + "_" + "code";
		public static final String T_STOP_K_NAME = T_STOP + "_" + "name";
		public static final String T_STOP_K_LAT = T_STOP + "_" + "lat";
		public static final String T_STOP_K_LNG = T_STOP + "_" + "lng";
		public static final String T_STOP_K_ACCESSIBLE = T_STOP + "_" + "a11y";
		public static final String T_STOP_K_ORIGINAL_ID_HASH = T_STOP + "_" + "o_id_hash";
		private static final String T_TRIP_STOPS = "trip_stops";
		public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = T_TRIP_STOPS + "_" + "stop_sequence";
		public static final String T_TRIP_STOPS_K_NO_PICKUP = T_TRIP_STOPS + "_" + "decent_only";
	}
}
