package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.GTFSCommons;

public class RouteDirection implements MTLog.Loggable {

	private static final String LOG_TAG = RouteDirection.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final Route route;
	@NonNull
	private final Direction direction;

	public RouteDirection(
			@NonNull Route route,
			@NonNull Direction direction
	) {
		this.route = route;
		this.direction = direction;
	}

	@NonNull
	public String getUUID() {
		return direction.getUUID(getAuthority());
	}

	public boolean equals(int routeId, int directionIdId) {
		return getRoute().getId() == routeId && getDirection().getId() == directionIdId;
	}

	@NonNull
	@Override
	public String toString() {
		return RouteDirection.class.getSimpleName() + "{" +
				"route=" + route +
				", direction=" + direction +
				", uuid='" + getUUID() + '\'' +
				'}';
	}

	@SuppressWarnings("unused")
	@NonNull
	public String toStringSimple() {
		StringBuilder sb = new StringBuilder();
		sb.append(getRoute().getShortName()).append('-');
		sb.append(getDirection().getHeadsignValue()).append('>');
		sb.append('(').append(getAuthority()).append(')');
		return sb.toString();
	}

	@SuppressWarnings("unused")
	@NonNull
	public String toStringShort() {
		return getRoute().getShortName() +
				'-' +
				getDirection().getHeadsignValue();
	}

	private static final String JSON_ROUTE = "route";
	private static final String JSON_DIRECTION = "trip"; // do not change to avoid breaking compat w/ old modules

	@Nullable
	public static JSONObject toJSON(@NonNull RouteDirection routeDirection) {
		return routeDirection.toJSON();
	}

	@Nullable
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_ROUTE, Route.toJSON(getRoute()));
			json.put(JSON_DIRECTION, Direction.toJSON(getDirection()));
			return json;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

	@Nullable
	public static RouteDirection fromJSON(@NonNull JSONObject json, @NonNull String authority) {
		try {
			return new RouteDirection(
					Route.fromJSON(json.getJSONObject(JSON_ROUTE), authority),
					Direction.fromJSON(json.getJSONObject(JSON_DIRECTION))
			);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
	public ContentValues toContentValues() {
		final ContentValues values = new ContentValues();
		values.put(GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_ID, getRoute().getId());
		values.put(GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_SHORT_NAME, getRoute().getShortName());
		values.put(GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_LONG_NAME, getRoute().getLongName());
		values.put(GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_COLOR, getRoute().getColor());
		if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
			values.put(GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_ORIGINAL_ID_HASH, getRoute().getOriginalIdHash());
			if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
				values.put(GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_TYPE, getRoute().getType());
			}
		}
		//
		values.put(GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_ID, getDirection().getId());
		values.put(GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_HEADSIGN_TYPE, getDirection().getHeadsignType());
		values.put(GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_HEADSIGN_VALUE, getDirection().getHeadsignValue());
		values.put(GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_ROUTE_ID, getDirection().getRouteId());
		//
		return values;
	}

	@NonNull
	public RouteDirection fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static RouteDirection fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		return new RouteDirection(
				new Route(
						authority,
						CursorExtKt.getLong(c, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_ID),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_SHORT_NAME),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_LONG_NAME),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_COLOR),
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? CursorExtKt.optInt(c, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH,
						FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT && FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE ? CursorExtKt.optInt(c, GTFSProviderContract.RouteDirectionColumns.T_ROUTE_K_TYPE, GTFSCommons.DEFAULT_ROUTE_TYPE) : GTFSCommons.DEFAULT_ROUTE_TYPE
				),
				new Direction(
						CursorExtKt.getLong(c, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_ID),
						CursorExtKt.getInt(c, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_HEADSIGN_TYPE),
						CursorExtKt.getString(c, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_HEADSIGN_VALUE),
						CursorExtKt.getInt(c, GTFSProviderContract.RouteDirectionColumns.T_DIRECTION_K_ROUTE_ID)
				)
		);
	}

	@NonNull
	public String getAuthority() {
		return this.route.getAuthority();
	}

	@NonNull
	public Route getRoute() {
		return route;
	}

	@NonNull
	public Direction getDirection() {
		return direction;
	}
}
