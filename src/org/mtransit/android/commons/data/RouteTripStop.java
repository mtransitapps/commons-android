package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider.RouteTripStopColumns;

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

	public Route route;
	public Trip trip;
	public Stop stop;
	public boolean decentOnly = false;

	public RouteTripStop(String authority, int dataSourceTypeId, Route route, Trip trip, Stop stop, boolean decentOnly) {
		super(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, POI.ITEM_STATUS_TYPE_SCHEDULE, POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP);
		this.route = route;
		this.trip = trip;
		this.stop = stop;
		this.decentOnly = decentOnly;
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP;
	}

	@Override
	public String getUUID() {
		return POI.POIUtils.getUUID(getAuthority(), this.route.id, this.trip.id, this.stop.id);
	}

	@Override
	public int compareToAlpha(Context contextOrNull, POI another) {
		if (another != null && another instanceof RouteTripStop) {
			// RTS = Route Short Name > Trip Heading > Stop Name
			RouteTripStop anotherRts = (RouteTripStop) another;
			if (this.route.id != anotherRts.route.id) {
				if (!TextUtils.isEmpty(this.route.shortName) && !TextUtils.isEmpty(anotherRts.route.shortName)) {
					return Route.SHORT_NAME_COMPATOR.compare(this.route, anotherRts.route);
				}
			}
			if (this.trip.id != anotherRts.trip.id) {
				if (contextOrNull != null && this.trip.headsignType == Trip.HEADSIGN_TYPE_STRING) {
					return this.trip.getHeading(contextOrNull).compareTo(anotherRts.trip.getHeading(contextOrNull));
				}
			}
		}
		return super.compareToAlpha(contextOrNull, another);
	}

	public boolean equals(int routeId, int tripId, int stopId) {
		return route.id == routeId && trip.id == tripId && stop.id == stopId;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(RouteTripStop.class.getSimpleName()).append(":[") //
				.append("authority:").append(getAuthority()) //
				.append(',') //
				.append("decentOnly:").append(decentOnly) //
				.append(',') //
				.append(stop) //
				.append(',') //
				.append(trip)//
				.append(',') //
				.append(route) //
				.append(',') //
				.append(']').toString();
	}

	public String toStringSimple() {
		StringBuilder sb = new StringBuilder(); //
		if (decentOnly) {
			sb.append("decentOnly-");
		}
		sb.append(route.shortName).append('-') //
				.append(trip.headsignValue).append('>') //
				.append(stop.name).append(',') //
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
			json.put(JSON_ROUTE, Route.toJSON(route)) //
					.put(JSON_TRIP, Trip.toJSON(trip)) //
					.put(JSON_STOP, Stop.toJSON(stop)) //
					.put(JSON_DECENT_ONLY, decentOnly) //
			;
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
		//
		values.put(RouteTripStopColumns.T_ROUTE_K_ID, route.id);
		values.put(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME, route.shortName);
		values.put(RouteTripStopColumns.T_ROUTE_K_LONG_NAME, route.longName);
		values.put(RouteTripStopColumns.T_ROUTE_K_COLOR, route.getColor());
		//
		values.put(RouteTripStopColumns.T_TRIP_K_ID, trip.id);
		values.put(RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, trip.headsignType);
		values.put(RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, trip.headsignValue);
		values.put(RouteTripStopColumns.T_TRIP_K_ROUTE_ID, trip.routeId);
		//
		values.put(RouteTripStopColumns.T_STOP_K_ID, stop.id);
		values.put(RouteTripStopColumns.T_STOP_K_CODE, stop.code);
		values.put(RouteTripStopColumns.T_STOP_K_NAME, stop.name);
		values.put(RouteTripStopColumns.T_STOP_K_LAT, stop.lat);
		values.put(RouteTripStopColumns.T_STOP_K_LNG, stop.lng);
		//
		values.put(RouteTripStopColumns.T_TRIP_STOPS_K_DECENT_ONLY, SqlUtils.toSQLBoolean(decentOnly));
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static RouteTripStop fromCursorStatic(Cursor c, String authority) {
		Route route = new Route();
		route.id = c.getLong(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_LONG_NAME));
		route.setColor(c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_COLOR)));
		Trip trip = new Trip();
		trip.id = c.getLong(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_ID));
		trip.headsignType = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE));
		trip.headsignValue = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE));
		trip.routeId = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_ROUTE_ID));
		Stop stop = new Stop();
		stop.id = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_ID));
		stop.code = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_CODE));
		stop.name = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_NAME));
		stop.lat = c.getDouble(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_LAT));
		stop.lng = c.getDouble(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_LNG));
		boolean decentOnly = SqlUtils.getBoolean(c, RouteTripStopColumns.T_TRIP_STOPS_K_DECENT_ONLY);
		int dataSourceTypeId = getDataSourceTypeIdFromCursor(c);
		RouteTripStop rts = new RouteTripStop(authority, dataSourceTypeId, route, trip, stop, decentOnly);
		DefaultPOI.fromCursor(c, rts);
		return rts;
	}

}
