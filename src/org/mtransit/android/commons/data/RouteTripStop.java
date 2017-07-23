package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;

public class RouteTripStop extends DefaultPOI {

	private static final String TAG = RouteTripStop.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Route route;
	private Trip trip;
	private Stop stop;
	private boolean descentOnly = false;

	public RouteTripStop(String authority, int dataSourceTypeId, Route route, Trip trip, Stop stop, boolean descentOnly) {
		super(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, POI.ITEM_STATUS_TYPE_SCHEDULE, POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP);
		setRoute(route);
		setTrip(trip);
		setStop(stop);
		setDescentOnly(descentOnly);
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

	private static final RelativeSizeSpan STOP_CODE_SIZE = SpanUtils.getNew50PercentSizeSpan();

	@Override
	public CharSequence getLabel() {
		if (TextUtils.isEmpty(getStop().getCode())) {
			return getName();
		} else {
			SpannableStringBuilder ssb = new SpannableStringBuilder(getName()).append(StringUtils.SPACE_CAR);
			int startStopCode = ssb.length();
			ssb.append(getStop().getCode());
			int endStopCode = ssb.length();
			return SpanUtils.set(ssb, startStopCode, endStopCode, STOP_CODE_SIZE);
		}
	}

	@Override
	public int compareToAlpha(@Nullable Context contextOrNull, POI another) {
		if (another != null && another instanceof RouteTripStop) {
			// RTS = Route Short Name > Trip Heading > Stop Name
			RouteTripStop anotherRts = (RouteTripStop) another;
			if (Route.SHORT_NAME_COMPARATOR.areDifferent(getRoute(), anotherRts.getRoute())) {
				if (Route.SHORT_NAME_COMPARATOR.areComparable(getRoute(), anotherRts.getRoute())) {
					return Route.SHORT_NAME_COMPARATOR.compare(getRoute(), anotherRts.getRoute());
				}
			}
			if (Trip.HEAD_SIGN_COMPARATOR.areDifferent(getTrip(), anotherRts.getTrip())) {
				if (Trip.HEAD_SIGN_COMPARATOR.areComparable(getTrip(), anotherRts.getTrip())) {
					return Trip.HEAD_SIGN_COMPARATOR.compare(getTrip(), anotherRts.getTrip());
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
				.append("descentOnly:").append(isDescentOnly()) //
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
		if (isDescentOnly()) {
			sb.append("descentOnly-");
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
	private static final String JSON_DESCENT_ONLY = "decentOnly";

	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_ROUTE, Route.toJSON(getRoute()));
			json.put(JSON_TRIP, Trip.toJSON(getTrip()));
			json.put(JSON_STOP, Stop.toJSON(getStop()));
			json.put(JSON_DESCENT_ONLY, isDescentOnly());
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
					json.getBoolean(JSON_DESCENT_ONLY) //
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
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID, getRoute().getId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME, getRoute().getShortName());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME, getRoute().getLongName());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR, getRoute().getColor());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID, getTrip().getId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, getTrip().getHeadsignType());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, getTrip().getHeadsignValue());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID, getTrip().getRouteId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID, getStop().getId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE, getStop().getCode());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME, getStop().getName());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT, getStop().getLat());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG, getStop().getLng());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_DESCENT_ONLY, SqlUtils.toSQLBoolean(isDescentOnly()));
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static RouteTripStop fromCursorStatic(Cursor c, String authority) {
		Route route = new Route();
		route.setId(c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID)));
		route.setShortName(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME)));
		route.setLongName(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME)));
		route.setColor(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR)));
		Trip trip = new Trip();
		trip.setId(c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID)));
		trip.setHeadsignType(c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE)));
		trip.setHeadsignValue(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE)));
		trip.setRouteId(c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID)));
		Stop stop = new Stop();
		stop.setId(c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID)));
		stop.setCode(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE)));
		stop.setName(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME)));
		stop.setLat(c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT)));
		stop.setLng(c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG)));
		boolean descentOnly = SqlUtils.getBoolean(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_DESCENT_ONLY);
		int dataSourceTypeId = getDataSourceTypeIdFromCursor(c);
		RouteTripStop rts = new RouteTripStop(authority, dataSourceTypeId, route, trip, stop, descentOnly);
		DefaultPOI.fromCursor(c, rts);
		return rts;
	}

	public boolean isDescentOnly() {
		return this.descentOnly;
	}

	private void setDescentOnly(boolean descentOnly) {
		this.descentOnly = descentOnly;
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
