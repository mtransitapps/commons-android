package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class Trip {

	private static final String TAG = Trip.class.getSimpleName();

	public static final int HEADSIGN_TYPE_STRING = 0;
	public static final int HEADSIGN_TYPE_DIRECTION = 1;
	public static final int HEADSIGN_TYPE_INBOUND = 2;
	public static final int HEADSIGN_TYPE_STOP_ID = 3;

	private long id;
	private int headsignType = HEADSIGN_TYPE_STRING; // 0 = String, 1 = direction, 2= inbound, 3=stopId
	private String headsignValue = "";
	private int routeId;

	public static Trip fromCursor(Cursor c) {
		Trip trip = new Trip();
		trip.setId(c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_ID)));
		trip.setHeadsignType(c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_TYPE)));
		trip.setHeadsignValue(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_VALUE)));
		trip.setRouteId(c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID)));
		return trip;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Trip.class.getSimpleName()).append(":[") //
				.append("id:").append(getId()).append(',') //
				.append("headsignType:").append(getHeadsignType()).append(',') //
				.append("headsignValue:").append(getHeadsignValue()).append(',') //
				.append("routeId:").append(getRouteId()) //
				.append(']').toString();
	}

	private static final String JSON_ID = "id";
	private static final String JSON_HEADSIGN_TYPE = "headsignType";
	private static final String JSON_HEADSIGN_VALUE = "headsignValue";
	private static final String JSON_ROUTE_ID = "routeId";

	public static JSONObject toJSON(Trip trip) {
		try {
			return new JSONObject() //
					.put(JSON_ID, trip.getId()) //
					.put(JSON_HEADSIGN_TYPE, trip.getHeadsignType()) //
					.put(JSON_HEADSIGN_VALUE, trip.getHeadsignValue()) //
					.put(JSON_ROUTE_ID, trip.getRouteId());
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", trip);
			return null;
		}
	}

	public static Trip fromJSON(JSONObject jTrip) {
		try {
			Trip trip = new Trip();
			trip.setId(jTrip.getLong(JSON_ID));
			trip.setHeadsignType(jTrip.getInt(JSON_HEADSIGN_TYPE));
			trip.setHeadsignValue(jTrip.getString(JSON_HEADSIGN_VALUE));
			trip.setRouteId(jTrip.getInt(JSON_ROUTE_ID));
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

	public static final String HEADING_OUTBOUND = "0";
	public static final String HEADING_INBOUND = "1";

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
			break;
		case HEADSIGN_TYPE_INBOUND:
			if (HEADING_INBOUND.equals(headsignValue)) {
				return context.getString(R.string.inbound);
			} else if (HEADING_OUTBOUND.equals(headsignValue)) {
				return context.getString(R.string.outbound);
			}
			break;
		default:
			break;
		}
		MTLog.w(TAG, "Unknown trip heading type: %s | value: %s !", headsignType, headsignValue);
		return context.getString(R.string.ellipsis);
	}

	public static boolean isSameHeadsign(String stringHeadsign1, String stringHeadsign2) {
		boolean stringHeadsign1Empty = TextUtils.isEmpty(stringHeadsign1);
		boolean stringHeadsign2Empty = TextUtils.isEmpty(stringHeadsign2);
		if (stringHeadsign1Empty) {
			if (stringHeadsign2Empty) {
				return true; // same (empty)
			}
			return false; // not the same
		} else if (stringHeadsign2Empty) {
			return false; // not the same
		}
		return StringUtils.equalsAlphabeticsAndDigits(stringHeadsign1, stringHeadsign2);
	}

	public long getId() {
		return this.id;
	}

	protected void setId(long id) {
		this.id = id;
	}

	public int getHeadsignType() {
		return headsignType;
	}

	protected void setHeadsignType(int headsignType) {
		this.headsignType = headsignType;
	}

	public String getHeadsignValue() {
		return headsignValue;
	}

	protected void setHeadsignValue(String headsignValue) {
		this.headsignValue = headsignValue;
	}

	public int getRouteId() {
		return routeId;
	}

	protected void setRouteId(int routeId) {
		this.routeId = routeId;
	}
}
