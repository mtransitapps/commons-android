package org.mtransit.android.commons.data;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.GTFSCommons;

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

	private final int originalIdHash;

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
				int originalIdHash
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
		final int a11yIdx = FeatureFlags.F_ACCESSIBILITY_CONSUMER ? c.getColumnIndex(GTFSProviderContract.StopColumns.T_STOP_K_ACCESSIBLE) : -1;
		final int originalIdHashIdx = FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? c.getColumnIndex(GTFSProviderContract.StopColumns.T_STOP_K_ORIGINAL_ID_HASH) : -1;
		return new Stop(
				c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_ID)),
				c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_CODE)),
				c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_NAME)),
				c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_LAT)),
				c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_LNG)),
				a11yIdx < 0 ? Accessibility.DEFAULT : c.getInt(a11yIdx),
				originalIdHashIdx < 0 ? GTFSCommons.DEFAULT_ID_HASH : c.getInt(originalIdHashIdx)
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
			if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
				jStop.put(JSON_ACCESSIBLE, stop.getAccessible());
			}
			if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
				jStop.put(JSON_ORIGINAL_ID_HASH, stop.getOriginalIdHash());
			}
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
					FeatureFlags.F_ACCESSIBILITY_CONSUMER ? jStop.optInt(JSON_ACCESSIBLE, Accessibility.DEFAULT) : Accessibility.DEFAULT,
					FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? jStop.optInt(JSON_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH
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

	public int getOriginalIdHash() {
		return originalIdHash;
	}
}
