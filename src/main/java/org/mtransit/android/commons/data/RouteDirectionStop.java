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

public class RouteDirectionStop extends DefaultPOI {

	private static final String LOG_TAG = RouteDirectionStop.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final Route route;
	@NonNull
	private final Direction direction;
	@NonNull
	private final Stop stop;
	private final boolean noPickup;

	public RouteDirectionStop(@NonNull String authority,
							  @DataSourceType int dataSourceTypeId,
							  @NonNull Route route,
							  @NonNull Direction direction,
							  @NonNull Stop stop,
							  boolean noPickup) {
		super(authority, -1, dataSourceTypeId, POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP, POI.ITEM_STATUS_TYPE_SCHEDULE, POI.ITEM_ACTION_TYPE_ROUTE_DIRECTION_STOP);
		this.route = route;
		this.direction = direction;
		this.stop = stop;
		this.noPickup = noPickup;
		resetUUID();
	}

	/**
	 * Only useful when POI needs to be stored in DB like Modules (from JSON)
	 * @deprecated use getRoute().getId(), getDirection().getId(), getStop().getId()
	 */
	@Deprecated
	@Override
	public int getId() {
		return super.getId();
	}

	@Nullable
	private String uuid = null;

	@NonNull
	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(getAuthority(), getRoute().getId(), getDirection().getId(), getStop().getId());
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
		if (another instanceof RouteDirectionStop) {
			// RDS = Route Short Name > Direction Heading > Stop Name
			RouteDirectionStop anotherRds = (RouteDirectionStop) another;
			if (Route.SHORT_NAME_COMPARATOR.areDifferent(getRoute(), anotherRds.getRoute())) {
				if (Route.SHORT_NAME_COMPARATOR.areComparable(getRoute(), anotherRds.getRoute())) {
					return Route.SHORT_NAME_COMPARATOR.compare(getRoute(), anotherRds.getRoute());
				}
			}
			if (Direction.HEAD_SIGN_COMPARATOR.areDifferent(getDirection(), anotherRds.getDirection())) {
				if (Direction.HEAD_SIGN_COMPARATOR.areComparable(getDirection(), anotherRds.getDirection())) {
					return Direction.HEAD_SIGN_COMPARATOR.compare(getDirection(), anotherRds.getDirection());
				}
			}
		}
		return super.compareToAlpha(contextOrNull, another);
	}

	public boolean equals(int routeId, int directionIdId, int stopId) {
		return getRoute().getId() == routeId && getDirection().getId() == directionIdId && getStop().getId() == stopId;
	}

	@NonNull
	@Override
	public String toString() {
		return RouteDirectionStop.class.getSimpleName() + "{" +
				"route=" + route +
				", direction=" + direction +
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
				.append(getDirection().getHeadsignValue()).append('>') //
				.append(getStop().getName()).append(',') //
				.append('(').append(getAuthority()).append(')'); //
		return sb.toString();
	}

	@NonNull
	public String toStringShort() {
		return getRoute().getShortName() +
				'-' +
				getDirection().getHeadsignValue() +
				'>' +
				getStop().getCode();
	}

	private static final String JSON_ROUTE = "route";
	private static final String JSON_DIRECTION = "trip"; // do not change to avoid breaking compat w/ old modules
	private static final String JSON_STOP = "stop";
	private static final String JSON_NO_PICKUP = "decentOnly";

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_ROUTE, Route.toJSON(getRoute()));
			json.put(JSON_DIRECTION, Direction.toJSON(getDirection()));
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
	public static RouteDirectionStop fromJSONStatic(@NonNull JSONObject json) {
		try {
			final RouteDirectionStop rds = new RouteDirectionStop( //
					DefaultPOI.getAuthorityFromJSON(json),//
					DefaultPOI.getDSTypeIdFromJSON(json),//
					Route.fromJSON(json.getJSONObject(JSON_ROUTE)), //
					Direction.fromJSON(json.getJSONObject(JSON_DIRECTION)), //
					Stop.fromJSON(json.getJSONObject(JSON_STOP)), //
					json.getBoolean(JSON_NO_PICKUP) //
			);
			DefaultPOI.fromJSON(json, rds);
			return rds;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		final ContentValues values = super.toContentValues();
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ID, getRoute().getId());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_SHORT_NAME, getRoute().getShortName());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_LONG_NAME, getRoute().getLongName());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_COLOR, getRoute().getColor());
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			values.put(GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH, getRoute().getOriginalIdHash());
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				values.put(GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_TYPE, getRoute().getType());
			}
		}
		//
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID, getDirection().getId());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_TYPE, getDirection().getHeadsignType());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_VALUE, getDirection().getHeadsignValue());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ROUTE_ID, getDirection().getRouteId());
		//
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ID, getStop().getId());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_CODE, getStop().getCode());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_NAME, getStop().getName());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_LAT, getStop().getLat());
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_LNG, getStop().getLng());
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ACCESSIBLE, getStop().getAccessible());
		}
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			values.put(GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ORIGINAL_ID_HASH, getStop().getOriginalIdHash());
		}
		// T_DIRECTION_STOPS_K_STOP_SEQUENCE not used in RouteDirectionStop class
		values.put(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_NO_PICKUP, SqlUtils.toSQLBoolean(isNoPickup()));
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static RouteDirectionStop fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		final RouteDirectionStop rds = new RouteDirectionStop(
				authority,
				getDataSourceTypeIdFromCursor(c),
				new Route(
						CursorExtKt.getLong(c, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ID),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_SHORT_NAME),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_LONG_NAME),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_COLOR),
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? CursorExtKt.optInt(c, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH,
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT && FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE ? CursorExtKt.optInt(c, GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_TYPE, GTFSCommons.DEFAULT_ROUTE_TYPE) : GTFSCommons.DEFAULT_ROUTE_TYPE
				),
				new Direction(
						CursorExtKt.getLong(c, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID),
						CursorExtKt.getInt(c, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_TYPE),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_HEADSIGN_VALUE),
						CursorExtKt.getInt(c, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ROUTE_ID)
				),
				new Stop(
						CursorExtKt.getInt(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ID),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_CODE),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_NAME),
						CursorExtKt.getDouble(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_LAT),
						CursorExtKt.getDouble(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_LNG),
						FeatureFlags.F_ACCESSIBILITY_CONSUMER ? CursorExtKt.optIntNN(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ACCESSIBLE, Accessibility.DEFAULT) : Accessibility.DEFAULT,
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? CursorExtKt.optInt(c, GTFSProviderContract.RouteDirectionStopColumns.T_STOP_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH
				),
				CursorExtKt.getBoolean(c, GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_NO_PICKUP)
		);
		DefaultPOI.fromCursor(c, rds);
		return rds;
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
	public Direction getDirection() {
		return direction;
	}

	@NonNull
	public Stop getStop() {
		return stop;
	}

	@Override
	public double getLat() {
		return stop.getLat();
	}

	@Override
	public double getLng() {
		return stop.getLng();
	}
}
