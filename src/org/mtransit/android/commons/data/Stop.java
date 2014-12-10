package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider.StopColumns;

import android.database.Cursor;

public class Stop {

	private static final String TAG = Stop.class.getSimpleName();

	public int id;

	public String code;
	public String name;

	public double lat;
	public double lng;

	public Stop() {
	}

	public Stop(Stop stop) {
		id = stop.id;
		code = stop.code;
		name = stop.name;
		lat = stop.lat;
		lng = stop.lng;
	}

	public static Stop fromCursor(Cursor c) {
		Stop stop = new Stop();
		stop.id = c.getInt(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_ID));
		stop.code = c.getString(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_CODE));
		stop.name = c.getString(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_NAME));
		stop.lat = c.getDouble(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_LAT));
		stop.lng = c.getDouble(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_LNG));
		return stop;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Stop.class.getSimpleName()).append(":[") //
				.append("id:").append(id).append(',') //
				.append("code:").append(code).append(',') //
				.append("name:").append(name).append(',') //
				.append("lat:").append(lat).append(',') //
				.append("lng:").append(lng) //
				.append(']').toString();
	}

	private static final String JSON_ID = "id";
	private static final String JSON_CODE = "code";
	private static final String JSON_NAME = "name";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LNG = "lng";

	public static JSONObject toJSON(Stop stop) {
		try {
			return new JSONObject() //
					.put(JSON_ID, stop.id) //
					.put(JSON_CODE, stop.code) //
					.put(JSON_NAME, stop.name) //
					.put(JSON_LAT, stop.lat) //
					.put(JSON_LNG, stop.lng);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", stop);
			return null;
		}
	}

	public static Stop fromJSON(JSONObject jStop) {
		try {
			Stop stop = new Stop();
			stop.id = jStop.getInt(JSON_ID);
			stop.code = jStop.getString(JSON_CODE);
			stop.name = jStop.getString(JSON_NAME);
			stop.lat = jStop.getDouble(JSON_LAT);
			stop.lng = jStop.getDouble(JSON_LNG);
			return stop;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jStop);
			return null;
		}
	}

	public Double getLat() {
		return this.lat;
	}

	public Double getLng() {
		return this.lng;
	}


}
