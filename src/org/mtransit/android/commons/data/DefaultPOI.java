package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.POIProvider.POIColumns;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class DefaultPOI implements POI {

	private static final String TAG = DefaultPOI.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private String authority;
	private int id;
	private String name;
	private double lat;
	private double lng;
	private int type = POI.ITEM_VIEW_TYPE_BASIC_POI;

	public DefaultPOI(String authority, int type) {
		this.authority = authority;
		this.type = type;
	}

	@Override
	public int getType() {
		return this.type;
	}

	@Override
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String getUUID() {
		return POI.POIUtils.getUUID(this.authority, getId());
	}

	@Override
	public String getAuthority() {
		return authority;
	}

	@Override
	public void setAuthority(String authority) {
		this.authority = authority;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setLat(Double lat) {
		this.lat = lat;
	}

	@Override
	public void setLng(Double lng) {
		this.lng = lng;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Double getLat() {
		return lat;
	}

	@Override
	public Double getLng() {
		return lng;
	}

	@Override
	public boolean hasLocation() {
		return true;
	}

	@Override
	public float getDistance() {
		return -1;
	}

	@Override
	public void setDistance(float distance) {
	}

	@Override
	public CharSequence getDistanceString() {
		return null;
	}

	@Override
	public void setDistanceString(CharSequence distanceString) {
	}

	@Override
	public int getStatusType() {
		return -1;
	}

	@Override
	public void setStatus(POIStatus status) {
	}

	@Override
	public POIStatus getStatus(Context context) {
		return null;
	}

	@Override
	public POIStatus getStatusOrNull() {
		return null;
	}

	@Override
	public boolean hasStatus() {
		return false;
	}

	@Override
	public boolean pingStatus(Context context) {
		return false;
	}

	@Override
	public int getActionsType() {
		return -1;
	}

	@Override
	public CharSequence[] getActionsItems(Context context, CharSequence defaultAction, boolean isFavorite) {
		return new CharSequence[] { defaultAction };
	}

	@Override
	public boolean onActionItemClick(Activity activity) {
		return false; // NOT HANDLED
	}

	@Override
	public boolean onActionsItemClick(Activity activity, int itemClicked, boolean isFavorite, POIUpdateListener listener) {
		return false; // NOT HANDLED
	}

	@Override
	public ContentValues toContentValues() {
		final ContentValues values = new ContentValues();
		values.put(POIColumns.T_POI_K_ID, getId());
		values.put(POIColumns.T_POI_K_NAME, getName());
		values.put(POIColumns.T_POI_K_LAT, getLat());
		values.put(POIColumns.T_POI_K_LNG, getLng());
		values.put(POIColumns.T_POI_K_TYPE, getType());
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static DefaultPOI fromCursorStatic(Cursor c, String authority) {
		final DefaultPOI defaultPOI = new DefaultPOI(authority, POI.ITEM_VIEW_TYPE_BASIC_POI);// getNewDefaultPOI(authority);
		fromCursor(c, defaultPOI);
		return defaultPOI;
	}

	public static void fromCursor(Cursor c, DefaultPOI defaultPOI) {
		defaultPOI.id = c.getInt(c.getColumnIndexOrThrow(POIColumns.T_POI_K_ID));
		defaultPOI.name = c.getString(c.getColumnIndexOrThrow(POIColumns.T_POI_K_NAME));
		defaultPOI.lat = c.getDouble(c.getColumnIndexOrThrow(POIColumns.T_POI_K_LAT));
		defaultPOI.lng = c.getDouble(c.getColumnIndexOrThrow(POIColumns.T_POI_K_LNG));
		defaultPOI.type = c.getInt(c.getColumnIndexOrThrow(POIColumns.T_POI_K_TYPE));
	}

	public static int getTypeFromCursor(Cursor c) {
		try {
			return c.getInt(c.getColumnIndexOrThrow(POIColumns.T_POI_K_TYPE));
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while retreiving POI type!");
			return POI.ITEM_VIEW_TYPE_BASIC_POI; // default
		}
	}

	@Override
	public POI fromJSON(JSONObject json) {
		// MTLog.v(TAG, "fromJSON(%s)", jRouteTripStop);
		try {
			final DefaultPOI defaultPOI = new DefaultPOI(authority, POI.ITEM_VIEW_TYPE_BASIC_POI);
			fromJSON(defaultPOI, json);
			return defaultPOI;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	public static void fromJSON(POI defaultPOI, JSONObject json) throws JSONException {
		defaultPOI.setId(json.getInt("id"));
		defaultPOI.setName(json.getString("name"));
		defaultPOI.setLat(json.getDouble("lat"));
		defaultPOI.setLng(json.getDouble("lng"));
		defaultPOI.setType(json.getInt("type"));
	}

	@Override
	public JSONObject toJSON() {
		// MTLog.v(TAG, "toJSON(%s)", routeTripStop);
		try {
			return new JSONObject() //
					.put("authority", getAuthority()) //
					.put("id", getId()) //
					.put("name", getName()) //
					.put("lat", getLat()) //
					.put("lng", getLng()) //
					.put("type", getType()) //
			;
		} catch (JSONException jsone) {
			MTLog.w(this, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

}
