package org.mtransit.android.commons.data;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSProviderContract;

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

	public Stop(int id,
				@NonNull String code,
				@NonNull String name,
				double lat,
				double lng) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
	}

	public Stop(@NonNull Stop stop) {
		this(
				stop.id,
				stop.code,
				stop.name,
				stop.lat,
				stop.lng
		);
	}

	@NonNull
	public static Stop fromCursor(@NonNull Cursor c) {
		return new Stop(
				c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_ID)),
				c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_CODE)),
				c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_NAME)),
				c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_LAT)),
				c.getDouble(c.getColumnIndexOrThrow(GTFSProviderContract.StopColumns.T_STOP_K_LNG))
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
				'}';
	}

	private static final String JSON_ID = "id";
	private static final String JSON_CODE = "code";
	private static final String JSON_NAME = "name";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LNG = "lng";

	@Nullable
	public static JSONObject toJSON(@NonNull Stop stop) {
		try {
			return new JSONObject() //
					.put(JSON_ID, stop.getId()) //
					.put(JSON_CODE, stop.getCode()) //
					.put(JSON_NAME, stop.getName()) //
					.put(JSON_LAT, stop.getLat()) //
					.put(JSON_LNG, stop.getLng());
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
					jStop.getDouble(JSON_LNG)
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
}
