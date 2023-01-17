package org.mtransit.android.commons.data;

import static org.mtransit.android.commons.data.DataSourceTypeId.DataSourceType;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

import java.lang.annotation.Retention;

public interface POI extends MTLog.Loggable {

	@Retention(SOURCE)
	@IntDef({ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, ITEM_VIEW_TYPE_BASIC_POI, ITEM_VIEW_TYPE_MODULE, ITEM_VIEW_TYPE_TEXT_MESSAGE})
	@interface ItemViewType {
	}

	int ITEM_VIEW_TYPE_ROUTE_TRIP_STOP = 0;
	int ITEM_VIEW_TYPE_BASIC_POI = 1;
	int ITEM_VIEW_TYPE_MODULE = 2;
	int ITEM_VIEW_TYPE_TEXT_MESSAGE = 3;

	@Retention(SOURCE)
	@IntDef({ITEM_STATUS_TYPE_NONE, ITEM_STATUS_TYPE_SCHEDULE, ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, ITEM_STATUS_TYPE_APP})
	@interface ItemStatusType {
	}

	int ITEM_STATUS_TYPE_NONE = -1;
	int ITEM_STATUS_TYPE_SCHEDULE = 0;
	int ITEM_STATUS_TYPE_AVAILABILITY_PERCENT = 1;
	int ITEM_STATUS_TYPE_APP = 3;

	@Retention(SOURCE)
	@IntDef({ITEM_ACTION_TYPE_NONE, ITEM_ACTION_TYPE_ROUTE_TRIP_STOP, ITEM_ACTION_TYPE_FAVORITABLE, ITEM_ACTION_TYPE_APP, ITEM_ACTION_TYPE_PLACE})
	@interface ItemActionType {
	}

	int ITEM_ACTION_TYPE_NONE = -1;
	int ITEM_ACTION_TYPE_ROUTE_TRIP_STOP = 0;
	int ITEM_ACTION_TYPE_FAVORITABLE = 1;
	int ITEM_ACTION_TYPE_APP = 2;
	int ITEM_ACTION_TYPE_PLACE = 3;

	int getId();

	void setId(int id);

	@NonNull
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

	@NonNull
	String getAuthority();

	@DataSourceType
	int getDataSourceTypeId();

	void setDataSourceTypeId(@DataSourceType int dataSourceTypeId);

	/**
	 * @return item view type (see {@link #getDataSourceTypeId()} for data source type)
	 */
	@ItemViewType
	int getType();

	void setType(@ItemViewType int type);

	@ItemStatusType
	int getStatusType();

	void setStatusType(@ItemStatusType int statusType);

	@ItemActionType
	int getActionsType();

	void setActionsType(@ItemActionType int actionsType);

	@Nullable
	Integer getScore();

	void setScore(@Nullable Integer score);

	@Nullable
	JSONObject toJSON();

	@Nullable
	POI fromJSON(@NonNull JSONObject json);

	@NonNull
	ContentValues toContentValues();

	@NonNull
	POI fromCursor(@NonNull Cursor cursor, @NonNull String authority);

	int compareToAlpha(@Nullable Context contextOrNull, @Nullable POI another);

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
		public static String extractAuthorityFromUUID(@Nullable String uuid) {
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