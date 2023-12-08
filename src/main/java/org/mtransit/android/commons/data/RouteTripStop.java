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
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.DataSourceTypeId.DataSourceType;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.GTFSCommons;

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
	private final boolean noPickup;

	public RouteTripStop(@NonNull String authority,
						 @DataSourceType int dataSourceTypeId,
						 @NonNull Route route,
						 @NonNull Trip trip,
						 @NonNull Stop stop,
						 boolean noPickup) {
		super(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, POI.ITEM_STATUS_TYPE_SCHEDULE, POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP);
		this.route = route;
		this.trip = trip;
		this.stop = stop;
		this.noPickup = noPickup;
		resetUUID();
	}

	@ItemViewType
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
		if (!TextUtils.isEmpty(getStop().getCode())) {
			SpannableStringBuilder ssb = new SpannableStringBuilder(super.getLabel());
			ssb.append(StringUtils.SPACE_CAR);
			final int startStopCode = ssb.length();
			ssb.append(getStop().getCode());
			final int endStopCode = ssb.length();
			return SpanUtils.setNN(ssb, startStopCode, endStopCode, STOP_CODE_SIZE);
		}
		return super.getLabel();
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
				", noPickup=" + noPickup +
				", uuid='" + uuid + '\'' +
				'}';
	}

	@SuppressWarnings("unused")
	@NonNull
	public String toStringSimple() {
		StringBuilder sb = new StringBuilder(); //
		if (isNoPickup()) {
			sb.append("noPickup-");
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
	private static final String JSON_NO_PICKUP = "decentOnly";

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_ROUTE, Route.toJSON(getRoute()));
			json.put(JSON_TRIP, Trip.toJSON(getTrip()));
			json.put(JSON_STOP, Stop.toJSON(getStop()));
			json.put(JSON_NO_PICKUP, isNoPickup());
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
					json.getBoolean(JSON_NO_PICKUP) //
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
		final ContentValues values = super.toContentValues();
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID, getRoute().getId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME, getRoute().getShortName());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME, getRoute().getLongName());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR, getRoute().getColor());
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			values.put(GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH, getRoute().getOriginalIdHash());
		}
		//
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID, getTrip().getId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, getTrip().getHeadsignType());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, getTrip().getHeadsignValue());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID, getTrip().getRouteId());
		//
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID, getStop().getId());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE, getStop().getCode());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME, getStop().getName());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT, getStop().getLat());
		values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG, getStop().getLng());
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ACCESSIBLE, getStop().getAccessible());
		}
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			values.put(GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ORIGINAL_ID_HASH, getStop().getOriginalIdHash());
		}
		// T_TRIP_STOPS_K_STOP_SEQUENCE not used in RouteTripStop class
		values.put(GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_NO_PICKUP, SqlUtils.toSQLBoolean(isNoPickup()));
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
						CursorExtKt.getLong(c, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ID),
						CursorExtKt.getString(c, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_SHORT_NAME),
						CursorExtKt.getString(c, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_LONG_NAME),
						CursorExtKt.getString(c, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_COLOR),
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? CursorExtKt.optInt(c, GTFSProviderContract.RouteTripStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH
				),
				new Trip(
						CursorExtKt.getLong(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID),
						CursorExtKt.getInt(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE),
						CursorExtKt.getString(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE),
						CursorExtKt.getInt(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ROUTE_ID)
				),
				new Stop(
						CursorExtKt.getInt(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ID),
						CursorExtKt.getString(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_CODE),
						CursorExtKt.getString(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_NAME),
						CursorExtKt.getDouble(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LAT),
						CursorExtKt.getDouble(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_LNG),
						FeatureFlags.F_ACCESSIBILITY_CONSUMER ? CursorExtKt.optIntNN(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ACCESSIBLE, Accessibility.DEFAULT) : Accessibility.DEFAULT,
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? CursorExtKt.optInt(c, GTFSProviderContract.RouteTripStopColumns.T_STOP_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH
				),
				CursorExtKt.getBoolean(c, GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_NO_PICKUP)
		);
		DefaultPOI.fromCursor(c, rts);
		return rts;
	}

	@Deprecated
	public boolean isDropOffOnly() {
		return isNoPickup();
	}

	public boolean isNoPickup() {
		return noPickup;
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
