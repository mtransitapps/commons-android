package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider.TripColumns;

import android.content.Context;
import android.database.Cursor;

public class Trip {

	private static final String TAG = Trip.class.getSimpleName();

	public static final int HEADSIGN_TYPE_STRING = 0;
	public static final int HEADSIGN_TYPE_DIRECTION = 1;
	public static final int HEADSIGN_TYPE_INBOUND = 2;
	public static final int HEADSIGN_TYPE_STOP_ID = 3;

	public int id;
	public int headsignType = HEADSIGN_TYPE_STRING; // 0 = String, 1 = direction, 2= inbound, 3=stopId
	public String headsignValue = "";
	public int routeId;

	public static Trip fromCursor(Cursor c) {
		final Trip trip = new Trip();
		trip.id = c.getInt(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_ID));
		trip.headsignType = c.getInt(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_HEADSIGN_TYPE));
		trip.headsignValue = c.getString(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_HEADSIGN_VALUE));
		trip.routeId = c.getInt(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_ROUTE_ID));
		return trip;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Trip.class.getSimpleName()).append(":[") //
				.append("id:").append(id).append(',') //
				.append("headsignType:").append(headsignType).append(',') //
				.append("headsignValue:").append(headsignValue).append(',') //
				.append("routeId:").append(routeId) //
				.append(']').toString();
	}

	public static JSONObject toJSON(Trip trip) {
		try {
			return new JSONObject() //
					.put("id", trip.id) //
					.put("headsignType", trip.headsignType) //
					.put("headsignValue", trip.headsignValue) //
					.put("routeId", trip.routeId);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", trip);
			return null;
		}
	}

	public static Trip fromJSON(JSONObject jTrip) {
		try {
			final Trip trip = new Trip();
			trip.id = jTrip.getInt("id");
			trip.headsignType = jTrip.getInt("headsignType");
			trip.headsignValue = jTrip.getString("headsignValue");
			trip.routeId = jTrip.getInt("routeId");
			return trip;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jTrip);
			return null;
		}
	}

	private String heading = null;

	public String getHeading(Context context) {
		if (heading == null) {
			heading = getNewHeading(context, this.headsignType, this.headsignValue);
		}
		return heading;
	}

	public static final String HEADING_INBOUND = "0";
	public static final String HEADING_OUTBOUND = "1";

	public static final String HEADING_EAST = "E";
	public static final String HEADING_NORTH = "N";
	public static final String HEADING_WEST = "W";
	public static final String HEADING_SOUTH = "S";

	public static String getNewHeading(Context context, int headsignType, String headsignValue) {
		switch (headsignType) {
		case HEADSIGN_TYPE_STRING:
			return headsignValue;
		case HEADSIGN_TYPE_DIRECTION:
			if (HEADING_EAST.equals(headsignValue)) {
				return context.getString(R.string.east);
			} else if (HEADING_NORTH.equals(headsignValue)) {
				return context.getString(R.string.north);
			} else if (HEADING_WEST.equals(headsignValue)) {
				return context.getString(R.string.west);
			} else if (HEADING_SOUTH.equals(headsignValue)) {
				return context.getString(R.string.south);
			}
		case HEADSIGN_TYPE_INBOUND:
			if (HEADING_INBOUND.equals(headsignValue)) {
				return context.getString(R.string.inbound);
			} else if (HEADING_OUTBOUND.equals(headsignValue)) {
				return context.getString(R.string.outbound);
			}
		default:
			break;
		}
		MTLog.w(TAG, "Unknown trip heading type: %s | value: %s !", headsignType, headsignValue);
		return context.getString(R.string.ellipsis);
	}

	public int getId() {
		return id;
	}

}
