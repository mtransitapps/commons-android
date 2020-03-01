package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

public interface POI extends MTLog.Loggable {

	int ITEM_VIEW_TYPE_ROUTE_TRIP_STOP = 0;
	int ITEM_VIEW_TYPE_BASIC_POI = 1;
	int ITEM_VIEW_TYPE_MODULE = 2;
	int ITEM_VIEW_TYPE_TEXT_MESSAGE = 3;

	int ITEM_STATUS_TYPE_NONE = -1;
	int ITEM_STATUS_TYPE_SCHEDULE = 0;
	int ITEM_STATUS_TYPE_AVAILABILITY_PERCENT = 1;
	int ITEM_STATUS_TYPE_APP = 3;

	int ITEM_ACTION_TYPE_NONE = -1;
	int ITEM_ACTION_TYPE_ROUTE_TRIP_STOP = 0;
	int ITEM_ACTION_TYPE_FAVORITABLE = 1;
	int ITEM_ACTION_TYPE_APP = 2;
	int ITEM_ACTION_TYPE_PLACE = 3;

	int getId();

	void setId(int id);

	CharSequence getLabel();

	@NonNull
	String getName();

	void setName(@NonNull String name);

	double getLat();

	void setLat(double lat);

	double getLng();

	void setLng(double lng);

	boolean hasLocation();

	@NonNull
	String getUUID();

	String getAuthority();

	void setAuthority(String authority);

	int getDataSourceTypeId();

	void setDataSourceTypeId(int dataSourceTypeId);

	int getType();

	void setType(int type);

	int getStatusType();

	void setStatusType(int statusType);

	int getActionsType();

	void setActionsType(int actionsType);

	Integer getScore();

	void setScore(Integer score);

	JSONObject toJSON();

	POI fromJSON(JSONObject json);

	ContentValues toContentValues();

	POI fromCursor(Cursor cursor, String authority);

	int compareToAlpha(@Nullable Context contextOrNull, POI another);

	class POIUtils implements MTLog.Loggable {

		private static final String LOG_TAG = POIUtils.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		public static final String UID_SEPARATOR = "-";

		@NonNull
		public static String getUUID(@NonNull String authority, @NonNull Object... poiUIDs) {
			StringBuilder sb = new StringBuilder(authority);
			for (Object poiUID : poiUIDs) {
				sb.append(UID_SEPARATOR).append(poiUID);
			}
			return sb.toString();
		}

		@Nullable
		public static String extractAuthorityFromUUID(String uuid) {
			if (TextUtils.isEmpty(uuid)) {
				return null;
			}
			String[] split = uuid.split(UID_SEPARATOR);
			if (split.length < 1) {
				return null;
			}
			return split[0];
		}
	}
}