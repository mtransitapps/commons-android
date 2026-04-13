package org.mtransit.android.commons.data;

import static org.mtransit.android.commons.StringUtils.EMPTY;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.GTFSCommons;

import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class Stop {

	private static final String LOG_TAG = Stop.class.getSimpleName();

	private final int id;

	@NonNull
	private final String code;
	@NonNull
	private final String name;

	private final double lat;
	private final double lng;

	private final int accessible;

	@Nullable
	private final Integer originalIdHash;

	@Deprecated
	public Stop(int id,
				@NonNull String code,
				@NonNull String name,
				double lat,
				double lng) {
		this(id, code, name, lat, lng, Accessibility.DEFAULT, GTFSCommons.DEFAULT_ID_HASH);
	}

	@Deprecated
	public Stop(int id,
				@NonNull String code,
				@NonNull String name,
				double lat,
				double lng,
				int accessible
	) {
		this(id, code, name, lat, lng, accessible, GTFSCommons.DEFAULT_ID_HASH);
	}

	public Stop(int id,
				@NonNull String code,
				@NonNull String name,
				double lat,
				double lng,
				int accessible,
				@Nullable Integer originalIdHash
	) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
		this.accessible = accessible;
		this.originalIdHash = originalIdHash;
	}

	public Stop(@NonNull Stop stop) {
		this(
				stop.id,
				stop.code,
				stop.name,
				stop.lat,
				stop.lng,
				stop.accessible,
				stop.originalIdHash
		);
	}

	@NonNull
	public static Stop fromCursor(@NonNull Cursor c) {
		return new Stop(
				CursorExtKt.getInt(c, GTFSProviderContract.StopColumns.T_STOP_K_ID),
				CursorExtKt.getString(c, GTFSProviderContract.StopColumns.T_STOP_K_CODE),
				CursorExtKt.getString(c, GTFSProviderContract.StopColumns.T_STOP_K_NAME),
				CursorExtKt.getDouble(c, GTFSProviderContract.StopColumns.T_STOP_K_LAT),
				CursorExtKt.getDouble(c, GTFSProviderContract.StopColumns.T_STOP_K_LNG),
				CursorExtKt.optIntNN(c, GTFSProviderContract.StopColumns.T_STOP_K_ACCESSIBLE, Accessibility.DEFAULT),
				CursorExtKt.optInt(c, GTFSProviderContract.StopColumns.T_STOP_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH)
		);
	}

	@NonNull
	@Override
	public String toString() {
		return Stop.class.getSimpleName() + "{" +
				"id=" + id +
				", code='" + code + '\'' +
				", name='" + name + '\'' +
				", lat=" + lat +
				", lng=" + lng +
				", a11y=" + accessible +
				", odIDHash=" + originalIdHash +
				'}';
	}

	private static final String JSON_ID = "id";
	private static final String JSON_CODE = "code";
	private static final String JSON_NAME = "name";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LNG = "lng";
	private static final String JSON_ACCESSIBLE = "a11y";
	private static final String JSON_ORIGINAL_ID_HASH = "o_id_hash";

	@Nullable
	public static JSONObject toJSON(@NonNull Stop stop) {
		try {
			final JSONObject jStop = new JSONObject() //
					.put(JSON_ID, stop.getId()) //
					.put(JSON_CODE, stop.getCode()) //
					.put(JSON_NAME, stop.getName()) //
					.put(JSON_LAT, stop.getLat()) //
					.put(JSON_LNG, stop.getLng()) //
					;
			jStop.put(JSON_ACCESSIBLE, stop.getAccessible());
			jStop.put(JSON_ORIGINAL_ID_HASH, stop.getOriginalIdHash());
			return jStop;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while converting to JSON (%s)!", stop);
			return null;
		}
	}

	@NonNull
	public static Stop fromJSON(@NonNull JSONObject jStop) throws JSONException {
		try {
			return new Stop(
					jStop.getInt(JSON_ID),
					jStop.getString(JSON_CODE),
					jStop.getString(JSON_NAME),
					jStop.getDouble(JSON_LAT),
					jStop.getDouble(JSON_LNG),
					JSONUtils.optInt(jStop, JSON_ACCESSIBLE, Accessibility.DEFAULT),
					JSONUtils.optInt(jStop, JSON_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH)
			);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", jStop);
			throw jsone;
		}
	}

	public int getId() {
		return id;
	}

	@NonNull
	public String getUUID(@NonNull String authority) {
		return POI.POIUtils.makeUUID(authority, this.id);
	}

	@NonNull
	public String getCode() {
		return code;
	}

	@NonNull
	public String getName() {
		return name;
	}

	public double getLat() {
		return this.lat;
	}

	public double getLng() {
		return this.lng;
	}

	public int getAccessible() {
		return accessible;
	}

	@Nullable
	protected Integer getOriginalIdHash() {
		return originalIdHash;
	}

	@Nullable
	public String getOriginalIdHashString() {
		return originalIdHash == null ? null : String.valueOf(originalIdHash);
	}

	@Deprecated
	@NonNull
	public String getOriginalIdHashStringOrDefault() {
		return originalIdHash == null ? EMPTY : String.valueOf(originalIdHash);
	}

	public boolean isSameOriginalId(@Nullable String cleanedOriginalIdHash) {
		if (cleanedOriginalIdHash == null) return false;
		if (this.originalIdHash == null) return false;
		return this.originalIdHash.toString().equals(cleanedOriginalIdHash);

	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Stop)) return false;
		final Stop stop = (Stop) o;
		return id == stop.id
				&& code.equals(stop.code)
				&& name.equals(stop.name)
				&& Double.compare(lat, stop.lat) == 0
				&& Double.compare(lng, stop.lng) == 0
				&& accessible == stop.accessible
				&& Objects.equals(originalIdHash, stop.originalIdHash);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + id;
		result = 31 * result + code.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + Double.hashCode(lat);
		result = 31 * result + Double.hashCode(lng);
		result = 31 * result + accessible;
		result = 31 * result + Objects.hashCode(originalIdHash);
		return result;
	}
}
