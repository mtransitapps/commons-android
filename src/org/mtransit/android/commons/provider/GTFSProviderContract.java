package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.ArrayUtils;

import android.provider.BaseColumns;

public interface GTFSProviderContract {

	String POI_FILTER_EXTRA_DESCENT_ONLY = "descentOnly";

	String ROUTE_PATH = "route";
	String TRIP_PATH = "trip";
	String STOP_PATH = "stop";
	String ROUTE_LOGO_PATH = "route/logo";
	String ROUTE_TRIP_STOP_PATH = "route/trip/stop";
	String ROUTE_TRIP_STOP_SEARCH_PATH = "route/trip/stop/*";
	String ROUTE_TRIP_PATH = "route/trip";
	String TRIP_STOP_PATH = "trip/stop";

	String[] PROJECTION_ROUTE_TRIP_STOP = new String[] { RouteTripStopColumns.T_ROUTE_K_ID, RouteTripStopColumns.T_ROUTE_K_SHORT_NAME,
			RouteTripStopColumns.T_ROUTE_K_LONG_NAME, RouteTripStopColumns.T_ROUTE_K_COLOR, RouteTripStopColumns.T_TRIP_K_ID,
			RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, RouteTripStopColumns.T_TRIP_K_ROUTE_ID,
			RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE, RouteTripStopColumns.T_TRIP_STOPS_K_DESCENT_ONLY, RouteTripStopColumns.T_STOP_K_ID,
			RouteTripStopColumns.T_STOP_K_CODE, RouteTripStopColumns.T_STOP_K_NAME, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG };

	String[] PROJECTION_ROUTE =
			new String[] { RouteColumns.T_ROUTE_K_ID, RouteColumns.T_ROUTE_K_SHORT_NAME, RouteColumns.T_ROUTE_K_LONG_NAME, RouteColumns.T_ROUTE_K_COLOR };

	String[] PROJECTION_TRIP =
			new String[] { TripColumns.T_TRIP_K_ID, TripColumns.T_TRIP_K_HEADSIGN_TYPE, TripColumns.T_TRIP_K_HEADSIGN_VALUE, TripColumns.T_TRIP_K_ROUTE_ID };

	String[] PROJECTION_RTS_POI = ArrayUtils.addAllNonNull(POIProvider.PROJECTION_POI, PROJECTION_ROUTE_TRIP_STOP);

	class RouteColumns {
		public static final String T_ROUTE_K_ID = BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = "short_name";
		public static final String T_ROUTE_K_LONG_NAME = "long_name";
		public static final String T_ROUTE_K_COLOR = "color";
	}

	class RouteTripColumns {
		private static final String T_ROUTE = "route";
		public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
		public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
		public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
		public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
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
		private static final String T_TRIP_STOPS = "trip_stops";
		public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = T_TRIP_STOPS + "_" + "stop_sequence";
		public static final String T_TRIP_STOPS_K_DESCENT_ONLY = T_TRIP_STOPS + "_" + "decent_only";
	}

	class StopColumns {
		public static final String T_STOP_K_ID = BaseColumns._ID;
		public static final String T_STOP_K_CODE = "code";
		public static final String T_STOP_K_NAME = "name";
		public static final String T_STOP_K_LAT = "lat";
		public static final String T_STOP_K_LNG = "lng";
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
		private static final String T_TRIP_STOPS = "trip_stops";
		public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = T_TRIP_STOPS + "_" + "stop_sequence";
		public static final String T_TRIP_STOPS_K_DESCENT_ONLY = T_TRIP_STOPS + "_" + "decent_only";
	}
}
