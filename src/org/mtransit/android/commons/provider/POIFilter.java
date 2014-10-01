package org.mtransit.android.commons.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;

public class POIFilter implements MTLog.Loggable {

	private static final String TAG = POIFilter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private double lat;
	private double lng;
	private double aroundDiff;

	private Map<String, Object> extras = new HashMap<String, Object>();

	public POIFilter(double lat, double lng, double aroundDiff) {
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
	}

	@Override
	public String toString() {
		return new StringBuilder(POIFilter.class.getSimpleName()).append('[') //
				.append("lat:").append(lat).append(',') //
				.append("lng:").append(lng).append(',') //
				.append("aroundDiff:").append(aroundDiff).append(',') //
				.append("extras:").append(extras)//
				.append(']').toString();
	}

	public void addExtra(String key, Object value) {
		this.extras.put(key, value);
	}

	public String getSqlSelection(String latTableColumn, String lngTableColumn) {
		return LocationUtils.genAroundWhere(this.lat, this.lng, latTableColumn, lngTableColumn, this.aroundDiff);
	}

	public static POIFilter fromJSONString(String jsonString) {
		try {
			return fromJSON(new JSONObject(jsonString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
			return null;
		}
	}

	private static POIFilter fromJSON(JSONObject json) {
		try {
			double lat = json.getDouble("lat");
			double lng = json.getDouble("lng");
			double aroundDiff = json.getDouble("aroundDiff");
			POIFilter poiFilter = new POIFilter(lat, lng, aroundDiff);
			JSONArray jExtras = json.getJSONArray("extras");
			for (int i = 0; i < jExtras.length(); i++) {
				JSONObject jExtra = jExtras.getJSONObject(i);
				final String key = jExtra.getString("key");
				final Object value = jExtra.get("value");
				poiFilter.addExtra(key, value);
			}
			return poiFilter;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
			return null;
		}
	}

	public static JSONObject toJSON(POIFilter poiFilter) throws JSONException {
		try {
			JSONObject json = new JSONObject();
			json.put("lat", poiFilter.lat);
			json.put("lng", poiFilter.lng);
			json.put("aroundDiff", poiFilter.aroundDiff);
			JSONArray jExtras = new JSONArray();
			if (poiFilter.extras != null) {
				for (Entry<String, Object> extra : poiFilter.extras.entrySet()) {
					JSONObject jExtra = new JSONObject();
					jExtra.put("key", extra.getKey());
					jExtra.put("value", extra.getValue());
					jExtras.put(jExtra);
				}
			}
			json.put("extras", jExtras);
			return json;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", poiFilter);
			return null;
		}
	}

	public boolean getExtra(String key, boolean defaultValue) {
		if (this.extras == null || !this.extras.containsKey(key)) {
			return defaultValue;
		}
		return (Boolean) this.extras.get(key);
	}
}
