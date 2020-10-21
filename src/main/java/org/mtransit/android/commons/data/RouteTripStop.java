package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;

public class RouteTripStop extends DefaultPOI {

	private static final String LOG_TAG = RouteTripStop.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final Route route;
	@NonNull
	private final Trip trip;
	@NonNull
	private final Stop stop;
	private final boolean descentOnly;

	public RouteTripStop(@NonNull String authority,
						 int dataSourceTypeId,
						 @NonNull Route route,
						 @NonNull Trip trip,
						 @NonNull Stop stop,
						 boolean descentOnly) {
		super(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, POI.ITEM_STATUS_TYPE_SCHEDULE, POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP);
		this.route = route;
		this.trip = trip;
		this.stop = stop;
		this.descentOnly = descentOnly;
		resetUUID();
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP;
	}

	@Nullable
	private String uuid = null;

	@NonNull
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

	@NonNull
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
	public int compareToAlpha(@Nullable Context contextOrNull, @Nullable POI another) {
		if (another instanceof RouteTripStop) {
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

	@NonNull
	@Override
	public String toString() {
		return RouteTripStop.class.getSimpleName() + "{" +
				"route=" + route +
				", trip=" + trip +
				", stop=" + stop +
				", descentOnly=" + descentOnly +
				", uuid='" + uuid + '\'' +
				'}';
	}

	@SuppressWarnings("unused")
	@NonNull
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

	@Nullable
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
			MTLog.w(LOG_TAG, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

	@Nullable
	@Override
	public POI fromJSON(@NonNull JSONObject json) {
		return fromJSONStatic(json);
	}

	@Nullable
	public static RouteTripStop fromJSONStatic(@NonNull JSONObject json) {
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
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
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

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static RouteTripStop fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		RouteTripStop rts = new RouteTripStop(
				authority,
				getDataSourceTypeIdFromCursor(c),
				new Route(
						c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID)),
						c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME)),
						c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME)),
						c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR))
				),
				new Trip(
						c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID)),
						c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE)),
						c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE)),
						c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID))
				),
				new Stop(
						c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID)),
						c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE)),
						c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME)),
						c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT)),
						c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG))
				),
				SqlUtils.getBoolean(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_DESCENT_ONLY)
		);
		DefaultPOI.fromCursor(c, rts);
		return rts;
	}

	public boolean isDescentOnly() {
		return this.descentOnly;
	}

	@NonNull
	public Route getRoute() {
		return route;
	}

	@NonNull
	public Trip getTrip() {
		return trip;
	}

	@NonNull
	public Stop getStop() {
		return stop;
	}
}
