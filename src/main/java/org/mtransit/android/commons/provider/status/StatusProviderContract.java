package org.mtransit.android.commons.provider.status;

import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.provider.common.ProviderContract;

public interface StatusProviderContract extends ProviderContract {

	String STATUS_PATH = "status";

	long getStatusMaxValidityInMs();

	long getStatusValidityInMs(boolean inFocus);

	long getMinDurationBetweenRefreshInMs(boolean inFocus);

	@Nullable
	POIStatus getNewStatus(@NonNull Filter statusFilter);

	void cacheStatus(@NonNull POIStatus newStatusToCache);

	@Nullable
	POIStatus getCachedStatus(@NonNull Filter statusFilter);

	boolean purgeUselessCachedStatuses();

	boolean deleteCachedStatus(int cachedStatusId);

	@NonNull
	Uri getAuthorityUri();

	int getStatusType();

	@NonNull
	String getStatusDbTableName();

	String[] PROJECTION_STATUS = new String[]{
			Columns.T_STATUS_K_ID,
			Columns.T_STATUS_K_TYPE,
			Columns.T_STATUS_K_TARGET_UUID,
			Columns.T_STATUS_K_LAST_UPDATE,
			Columns.T_STATUS_K_VALIDITY,
			Columns.T_STATUS_K_READ_FROM_SOURCE_AT,
			Columns.T_STATUS_K_EXTRAS
	};

	class Columns {
		public static final String T_STATUS_K_ID = BaseColumns._ID;
		public static final String T_STATUS_K_TYPE = "type";
		public static final String T_STATUS_K_TARGET_UUID = "target";
		public static final String T_STATUS_K_EXTRAS = "extras";
		public static final String T_STATUS_K_LAST_UPDATE = "last_update";
		public static final String T_STATUS_K_VALIDITY = "max_validity";
		public static final String T_STATUS_K_READ_FROM_SOURCE_AT = "read_from_source_at";
	}

	@SuppressWarnings("WeakerAccess")
	abstract class Filter extends ProviderContract.Filter implements MTLog.Loggable {

		private static final String LOG_TAG = StatusProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private String targetUUID;
		@POI.ItemStatusType
		private int type;

		public Filter(@POI.ItemStatusType int type, @NonNull String targetUUID) {
			this.type = type;
			this.targetUUID = targetUUID;
		}

		@NonNull
		public String getTargetUUID() {
			return this.targetUUID;
		}

		@POI.ItemStatusType
		public int getType() {
			return this.type;
		}

		public static int getTypeFromJSONString(@Nullable String jsonString) {
			try {
				return jsonString == null ? -1 : getTypeFromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return -1;
			}
		}

		public static int getTypeFromJSON(@NonNull JSONObject json) throws JSONException {
			return json.getInt(JSON_TYPE);
		}

		@NonNull
		public static String getTargetUUIDFromJSON(@NonNull JSONObject json) throws JSONException {
			return json.getString(JSON_TARGET_UUID);
		}

		public static void toJSON(@NonNull Filter statusFilter, @NonNull JSONObject json) throws JSONException {
			ProviderContract.Filter.toJSON(statusFilter, json);
			json.put(JSON_TYPE, statusFilter.getType());
			json.put(JSON_TARGET_UUID, statusFilter.getTargetUUID());
		}

		private static final String JSON_TYPE = "type";
		private static final String JSON_TARGET_UUID = "target";

		public static void fromJSON(@NonNull Filter statusFilter, @NonNull JSONObject json) throws JSONException {
			ProviderContract.Filter.fromJSON(statusFilter, json);
			statusFilter.type = json.getInt(JSON_TYPE);
			statusFilter.targetUUID = json.getString(JSON_TARGET_UUID);
		}

		@Nullable
		@SuppressWarnings("unused")
		public abstract Filter fromJSONStringStatic(@Nullable String jsonString);

		@SuppressWarnings("unused")
		@Nullable
		public abstract String toJSONStringStatic(@NonNull Filter statusFilter);

		@NonNull
		@Override
		public String toString() {
			return Filter.class.getSimpleName() + "{" +
					"targetUUID='" + targetUUID + '\'' +
					", type=" + type +
					", " + super.toStringParts() +
					'}';
		}
	}
}
