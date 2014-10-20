package org.mtransit.android.commons.data;

import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public interface POI extends MTLog.Loggable {

	public static final int ITEM_VIEW_TYPE_ROUTE_TRIP_STOP = 0;
	public static final int ITEM_VIEW_TYPE_BASIC_POI = 1;
	public static final int ITEM_VIEW_TYPE_MODULE = 2;

	public static final int ITEM_STATUS_TYPE_SCHEDULE = 0;
	public static final int ITEM_STATUS_TYPE_AVAILABILITY_PERCENT = 1;
	public static final int ITEM_STATUS_TYPE_APP = 3;

	public static final int ITEM_ACTION_TYPE_ROUTE_TRIP_STOP = 0;
	public static final int ITEM_ACTION_TYPE_FAVORITABLE = 1;
	public static final int ITEM_ACTION_TYPE_APP = 2;


	public int getId();

	public void setId(int id);

	public String getName();

	public void setName(String name);


	public Double getLat();

	public void setLat(Double lat);

	public Double getLng();

	public void setLng(Double lng);

	public boolean hasLocation();

	public String getUUID();

	public String getAuthority();

	public void setAuthority(String authority);

	public int getType();

	public void setType(int type);


	public int getStatusType();

	public void setStatusType(int statusType);


	public int getActionsType();

	public void setActionsType(int actionsType);

	public JSONObject toJSON();

	public POI fromJSON(JSONObject json);

	public ContentValues toContentValues();

	public POI fromCursor(Cursor cursor, String authority);

	public int compareToAlpha(Context contextOrNull, POI another);

	public static class POIUtils implements MTLog.Loggable {

		private static final String TAG = POIUtils.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public static final String UID_SEPARATOR = "-";

		public static String getUUID(String authority, Object... poiUIDs) {
			StringBuilder sb = new StringBuilder(authority);
			for (Object poiUID : poiUIDs) {
				sb.append(UID_SEPARATOR).append(poiUID);
			}
			return sb.toString();
		}

		public static String extractAuthorityFromUUID(String uuid) {
			if (TextUtils.isEmpty(uuid)) {
				return null;
			}
			final String[] split = uuid.split(UID_SEPARATOR);
			if (split.length < 1) {
				return null;
			}
			return split[0];
		}

	}

}