package org.mtransit.android.commons.data;

import static org.mtransit.android.commons.data.POI.ItemStatusType;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

public class POIStatus implements MTLog.Loggable {

	private static final String LOG_TAG = POIStatus.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static String getStatusTextFont() {
		return SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE;
	}

	@ColorInt
	protected static int getDefaultStatusTextColor(@NonNull Context context) {
		return ColorUtils.getTextColorTertiary(context);
	}

	@Nullable
	private final Integer id; // internal DB ID (useful to delete) OR NULL
	@NonNull
	private String targetUUID;
	@ItemStatusType
	private final int type;
	private final long lastUpdateInMs;
	private final long maxValidityInMs;
	private final long readFromSourceAtInMs;
	private final boolean noData;

	public POIStatus(@Nullable Integer id,
					 @NonNull String targetUUID,
					 @ItemStatusType int type,
					 long lastUpdateInMs,
					 long maxValidityInMs,
					 long readFromSourceAtInMs,
					 boolean noData) {
		this.id = id;
		this.targetUUID = targetUUID;
		this.type = type;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
		this.readFromSourceAtInMs = readFromSourceAtInMs;
		this.noData = noData;
	}

	@NonNull
	@Override
	public String toString() {
		return POIStatus.class.getSimpleName() + "{" +
				"id=" + id +
				", targetUUID='" + targetUUID + '\'' +
				", type=" + type +
				", lastUpdateInMs=" + lastUpdateInMs +
				", maxValidityInMs=" + maxValidityInMs +
				", readFromSourceAtInMs=" + readFromSourceAtInMs +
				", noData=" + noData +
				'}';
	}

	private static final String JSON_NO_DATA = "noData";

	@NonNull
	public static POIStatus fromCursor(@NonNull Cursor cursor) {
		int idIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_ID);
		Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID));
		int type = cursor.getInt(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TYPE));
		long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE));
		long maxValidityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_MAX_VALIDITY_IN_MS));
		long readFromSourceAtInMs; // optional
		int readFromSourceAtColumnIndex = cursor.getColumnIndex(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS);
		if (readFromSourceAtColumnIndex < 0) {
			readFromSourceAtInMs = -1L;
		} else {
			readFromSourceAtInMs = cursor.getLong(readFromSourceAtColumnIndex);
		}
		boolean noData = false; // optional
		try {
			String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
			JSONObject extrasJSON = extrasJSONString.isEmpty() ? null : new JSONObject(extrasJSONString);
			if (extrasJSON != null) {
				noData = extrasJSON.optBoolean(JSON_NO_DATA, false);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while retrieving extras information from cursor.");
		}
		return new POIStatus(id, targetUUID, type, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, noData);
	}

	@NonNull
	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(StatusProviderContract.PROJECTION_STATUS);
		cursor.addRow(new Object[]{
				this.id,
				this.type,
				this.targetUUID,
				this.lastUpdateInMs,
				this.maxValidityInMs,
				this.readFromSourceAtInMs,
				getExtrasJSONString()
		});
		return cursor;
	}

	public static int getTypeFromCursor(@NonNull Cursor c) {
		return CursorExtKt.getInt(c, StatusProviderContract.Columns.T_STATUS_K_TYPE);
	}

	@SuppressWarnings("unused")
	@NonNull
	public static String getTargetUUIDFromCursor(@NonNull Cursor c) {
		return CursorExtKt.getString(c, StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID);
	}

	@SuppressWarnings("unused")
	public static long getLastUpdateInMsFromCursor(@NonNull Cursor c) {
		return CursorExtKt.getLong(c, StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE);
	}

	@NonNull
	static String getExtrasFromCursor(@NonNull Cursor c) {
		return CursorExtKt.getString(c, StatusProviderContract.Columns.T_STATUS_K_EXTRAS);
	}

	@NonNull
	public ContentValues toContentValues() {
		ContentValues contentValues = new ContentValues();
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_TYPE, this.type);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, this.targetUUID);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_MAX_VALIDITY_IN_MS, this.maxValidityInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS, this.readFromSourceAtInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_EXTRAS, getExtrasJSONString());
		return contentValues;
	}

	public boolean isUseful() {
		return this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis();
	}

	public boolean isNoData() {
		return this.noData;
	}

	@Nullable
	private String getExtrasJSONString() {
		try {
			JSONObject extrasJSON = getExtrasJSON();
			if (extrasJSON == null) {
				extrasJSON = new JSONObject();
			}
			extrasJSON.put(JSON_NO_DATA, this.noData);
			return extrasJSON.toString();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while converting JSON to String!");
			return null;
		}
	}

	@Nullable
	public JSONObject getExtrasJSON() {
		return null; // to override in super class
	}

	@Nullable
	public Integer getId() {
		return id;
	}

	@NonNull
	public String getTargetUUID() {
		return targetUUID;
	}

	public void setTargetUUID(@NonNull String targetUUID) {
		this.targetUUID = targetUUID;
	}

	@ItemStatusType
	public int getType() {
		return type;
	}

	public long getLastUpdateInMs() {
		return lastUpdateInMs;
	}

	public long getMaxValidityInMs() {
		return maxValidityInMs;
	}

	public long getReadFromSourceAtInMs() {
		return readFromSourceAtInMs;
	}
}
