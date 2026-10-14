package org.mtransit.android.commons.data;

import android.database.Cursor;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.TextUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class Route implements Targetable, MTLog.Loggable {

	private static final String LOG_TAG = Route.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final RouteShortNameComparator SHORT_NAME_COMPARATOR = new RouteShortNameComparator();

	@NonNull
	private final String authority;
	private final long id;
	@NonNull
	private final String shortName;
	@NonNull
	private final String longName;
	@NonNull
	private final String color;

	@Nullable
	private final Integer originalIdHash;
	@Nullable
	private final Integer type;

	@VisibleForTesting
	public Route(
			@NonNull String authority,
			long id,
			@NonNull String shortName,
			@NonNull String longName,
			@NonNull String color
	) {
		this(authority, id, shortName, longName, color, GTFSCommons.DEFAULT_ID_HASH, GTFSCommons.DEFAULT_ROUTE_TYPE);
	}

	public Route(
			@NonNull String authority,
			long id,
			@NonNull String shortName,
			@NonNull String longName,
			@NonNull String color,
			@Nullable Integer originalIdHash,
			@Nullable Integer type
	) {
		this.authority = authority;
		this.id = id;
		this.shortName = shortName;
		this.longName = longName;
		this.color = color;
		this.colorInt = null;
		this.originalIdHash = originalIdHash;
		this.type = type;
	}

	@NonNull
	public static Route fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return new Route(
				authority,
				CursorExtKt.getLong(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_ID),
				CursorExtKt.getString(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_SHORT_NAME),
				CursorExtKt.getString(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_LONG_NAME),
				CursorExtKt.getString(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_COLOR),
				CursorExtKt.optInt(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH),
				CursorExtKt.optInt(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_TYPE, GTFSCommons.DEFAULT_ROUTE_TYPE)
		);
	}

	public boolean hasColor() {
		return !TextUtils.isEmpty(this.color);
	}

	@NonNull
	public String getColor() {
		return color;
	}

	@Nullable
	private Integer colorInt;

	@SuppressWarnings("unused") // main app only
	@ColorInt
	public int getColorInt() {
		if (this.colorInt == null) {
			this.colorInt = ColorUtils.parseColor(getColor());
		}
		return this.colorInt;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Route)) return false;
		final Route route = (Route) o;
		return id == route.id
				&& authority.equals(route.authority)
				&& shortName.equals(route.shortName)
				&& longName.equals(route.longName)
				&& color.equals(route.color)
				&& Objects.equals(originalIdHash, route.originalIdHash)
				&& Objects.equals(type, route.type)
				;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				id,
				authority,
				shortName,
				longName,
				color,
				originalIdHash,
				type
		);
	}

	@NonNull
	@Override
	public String toString() {
		return Route.class.getSimpleName() + "{" +
				"authority=" + authority +
				", id=" + id +
				", shortName='" + shortName + '\'' +
				", longName='" + longName + '\'' +
				", color='" + color + '\'' +
				", colorInt=" + colorInt +
				", odIDHash=" + originalIdHash +
				", type=" + type +
				'}';
	}

	@Nullable
	public static JSONObject toJSON(@NonNull Route route) {
		try {
			final JSONObject jRoute = new JSONObject() //
					.put(JSON_AUTHORITY, route.getAuthority()) //
					.put(JSON_ID, route.getId()) //
					.put(JSON_SHORT_NAME, route.getShortName()) //
					.put(JSON_LONG_NAME, route.getLongName()) //
					.put(JSON_COLOR, route.getColor() //
					);
			jRoute.put(JSON_ORIGINAL_ID_HASH, route.getOriginalIdHash());
			jRoute.put(JSON_TYPE, route.getType());
			return jRoute;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while converting to JSON (%s)!", route);
			return null;
		}
	}

	private static final String JSON_AUTHORITY = "authority";
	private static final String JSON_ID = "id";
	private static final String JSON_SHORT_NAME = "shortName";
	private static final String JSON_LONG_NAME = "longName";
	private static final String JSON_COLOR = "color";
	private static final String JSON_ORIGINAL_ID_HASH = "o_id_hash";
	private static final String JSON_TYPE = "type";

	@NonNull
	public static Route fromJSON(@NonNull JSONObject jRoute, @NonNull String authority) throws JSONException {
		try {
			final String routeAuthority = JSONUtils.optString(jRoute, JSON_AUTHORITY);
			return new Route(
					routeAuthority == null ? authority : routeAuthority,
					jRoute.getLong(JSON_ID),
					jRoute.getString(JSON_SHORT_NAME),
					jRoute.getString(JSON_LONG_NAME),
					jRoute.getString(JSON_COLOR),
					JSONUtils.optInt(jRoute, JSON_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH),
					JSONUtils.optInt(jRoute, JSON_TYPE, GTFSCommons.DEFAULT_ROUTE_TYPE)
			);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", jRoute);
			throw jsone;
		}
	}

	@NonNull
	public String getAuthority() {
		return authority;
	}

	public long getId() {
		return id;
	}

	@Nullable
	private String uuid = null;

	@NonNull
	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.makeUUID(this.authority, this.id);
		}
		return this.uuid;
	}

	@SuppressWarnings("unused")
	public void resetUUID() {
		this.uuid = null;
	}

	@SuppressWarnings("unused") // main app only
	@NonNull
	public Collection<String> getAllUUIDs() {
		return Arrays.asList(
				this.authority,
				getUUID()
		);
	}

	@NonNull
	public String getShortestName() {
		if (TextUtils.isEmpty(getShortName())) {
			return getLongName();
		}
		return getShortName();
	}

	@SuppressWarnings("unused")
	@NonNull
	public String getLongestName() {
		if (TextUtils.isEmpty(getLongName())) {
			return getShortName();
		}
		return getLongName();
	}

	@NonNull
	public String getShortName() {
		return shortName;
	}

	@NonNull
	public String getLongName() {
		return longName;
	}

	@Nullable
	public Integer getOriginalIdHash() {
		return originalIdHash;
	}

	@Nullable
	public Integer getType() {
		return type;
	}
}
