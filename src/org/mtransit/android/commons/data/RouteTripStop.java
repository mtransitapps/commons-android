package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProviderContract;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class RouteTripStop extends DefaultPOI {

	private static final String TAG = RouteTripStop.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Route route;
	private Trip trip;
	private Stop stop;
	private boolean decentOnly = false;

	public RouteTripStop(String authority, int dataSourceTypeId, Route route, Trip trip, Stop stop, boolean decentOnly) {
		super(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, POI.ITEM_STATUS_TYPE_SCHEDULE, POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP);
		setRoute(route);
		setTrip(trip);
		setStop(stop);
		setDecentOnly(decentOnly);
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP;
	}

	private String uuid = null;

	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(getAuthority(), getRoute().getId(), getTrip().getId(), getStop().getId());
		}
		return this.uuid;
	}

	@Override
	public void resetUUID() {
		this.uuid = null;
	}

	@Override
	public int compareToAlpha(Context contextOrNull, POI another) {
		if (another != null && another instanceof RouteTripStop) {
			// RTS = Route Short Name > Trip Heading > Stop Name
			RouteTripStop anotherRts = (RouteTripStop) another;
			if (getRoute().getId() != anotherRts.getRoute().getId()) {
				if (!TextUtils.isEmpty(getRoute().getShortName()) && !TextUtils.isEmpty(anotherRts.getRoute().getShortName())) {
					return Route.SHORT_NAME_COMPATOR.compare(getRoute(), anotherRts.getRoute());
				}
			}
			if (getTrip().getId() != anotherRts.getTrip().getId()) {
				if (contextOrNull != null && getTrip().getHeadsignType() == Trip.HEADSIGN_TYPE_STRING) {
					return getTrip().getHeading(contextOrNull).compareTo(anotherRts.getTrip().getHeading(contextOrNull));
				}
			}
		}
		return super.compareToAlpha(contextOrNull, another);
	}

	public boolean equals(int routeId, int tripId, int stopId) {
		return getRoute().getId() == routeId && getTrip().getId() == tripId && getStop().getId() == stopId;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(RouteTripStop.class.getSimpleName()).append(":[") //
				.append("authority:").append(getAuthority()) //
				.append(',') //
				.append("decentOnly:").append(isDecentOnly()) //
				.append(',') //
				.append(getStop()) //
				.append(',') //
				.append(getTrip())//
				.append(',') //
				.append(getRoute()) //
				.append(',') //
				.append(']').toString();
	}

	public String toStringSimple() {
		StringBuilder sb = new StringBuilder(); //
		if (isDecentOnly()) {
			sb.append("decentOnly-");
		}
		sb.append(getRoute().getShortName()).append('-') //
				.append(getTrip().getHeadsignValue()).append('>') //
				.append(getStop().getName()).append(',') //
				.append('(').append(getAuthority()).append(')'); //
		return sb.toString();
	}

	private static final String JSON_ROUTE = "route";
	private static final String JSON_TRIP = "trip";
	private static final String JSON_STOP = "stop";
	private static final String JSON_DECENT_ONLY = "decentOnly";

	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_ROUTE, Route.toJSON(getRoute()));
			json.put(JSON_TRIP, Trip.toJSON(getTrip()));
			json.put(JSON_STOP, Stop.toJSON(getStop()));
			json.put(JSON_DECENT_ONLY, isDecentOnly());
			DefaultPOI.toJSON(this, json);
			return json;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

	@Override
	public POI fromJSON(JSONObject json) {
		return fromJSONStatic(json);
	}

	public static RouteTripStop fromJSONStatic(JSONObject json) {
		try {
			RouteTripStop rts = new RouteTripStop( //
					DefaultPOI.getAuthorityFromJSON(json),//
					DefaultPOI.getDSTypeIdFromJSON(json),//
					Route.fromJSON(json.getJSONObject(JSON_ROUTE)), //
					Trip.fromJSON(json.getJSONObject(JSON_TRIP)), //
					Stop.fromJSON(json.getJSONObject(JSON_STOP)), //
					json.getBoolean(JSON_DECENT_ONLY) //
			);
			DefaultPOI.fromJSON(json, rts);
			return rts;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_ID, getRoute().getId());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME, getRoute().getShortName());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME, getRoute().getLongName());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR, getRoute().getColor());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_ID, getTrip().getId());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, getTrip().getHeadsignType());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, getTrip().getHeadsignValue());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID, getTrip().getRouteId());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_ID, getStop().getId());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_CODE, getStop().getCode());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_NAME, getStop().getName());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_LAT, getStop().getLat());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_LNG, getStop().getLng());
		values.put(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_DECENT_ONLY, SqlUtils.toSQLBoolean(isDecentOnly()));
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static RouteTripStop fromCursorStatic(Cursor c, String authority) {
		Route route = new Route();
		route.setId(c.getLong(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_ID)));
		route.setShortName(c.getString(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME)));
		route.setLongName(c.getString(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME)));
		route.setColor(c.getString(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR)));
		Trip trip = new Trip();
		trip.setId(c.getLong(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_ID)));
		trip.setHeadsignType(c.getInt(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE)));
		trip.setHeadsignValue(c.getString(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE)));
		trip.setRouteId(c.getInt(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID)));
		Stop stop = new Stop();
		stop.setId(c.getInt(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_ID)));
		stop.setCode(c.getString(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_CODE)));
		stop.setName(c.getString(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_NAME)));
		stop.setLat(c.getDouble(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_LAT)));
		stop.setLng(c.getDouble(c.getColumnIndexOrThrow(GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_STOP_K_LNG)));
		boolean decentOnly = SqlUtils.getBoolean(c, GTFSRouteTripStopProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_DECENT_ONLY);
		int dataSourceTypeId = getDataSourceTypeIdFromCursor(c);
		RouteTripStop rts = new RouteTripStop(authority, dataSourceTypeId, route, trip, stop, decentOnly);
		DefaultPOI.fromCursor(c, rts);
		return rts;
	}

	public boolean isDecentOnly() {
		return this.decentOnly;
	}

	private void setDecentOnly(boolean decentOnly) {
		this.decentOnly = decentOnly;
	}

	public Route getRoute() {
		return route;
	}

	private void setRoute(Route route) {
		this.route = route;
		resetUUID();
	}

	public Trip getTrip() {
		return trip;
	}

	private void setTrip(Trip trip) {
		this.trip = trip;
		resetUUID();
	}

	public Stop getStop() {
		return stop;
	}

	private void setStop(Stop stop) {
		this.stop = stop;
		resetUUID();
	}
}
