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
import org.mtransit.android.commons.provider.status.StatusProviderContract;

// TODO abstract
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
	private long lastUpdateInMs;
	private long validityInMs;
	private long readFromSourceAtInMs;
	@Nullable
	private String sourceLabel;
	private final boolean noData;

	public POIStatus(
			@Nullable Integer id,
			@NonNull String targetUUID,
			@ItemStatusType int type,
			long lastUpdateInMs,
			long validityInMs,
			long readFromSourceAtInMs,
			@Nullable String sourceLabel,
			boolean noData
	) {
		this.id = id;
		this.targetUUID = targetUUID;
		this.type = type;
		this.lastUpdateInMs = lastUpdateInMs;
		this.validityInMs = validityInMs;
		this.readFromSourceAtInMs = readFromSourceAtInMs;
		this.sourceLabel = sourceLabel;
		this.noData = noData;
	}

	@NonNull
	@Override
	public String toString() {
		return POIStatus.class.getSimpleName() + "{" +
				"id=" + id +
				", targetUUID='" + targetUUID + '\'' +
				", type=" + type +
				", lastUpdate=" + MTLog.formatDateTime(lastUpdateInMs) +
				", validity=" + MTLog.formatDuration(validityInMs) +
				", readFromSourceAt=" + MTLog.formatDateTime(readFromSourceAtInMs) +
				", sourceLabel='" + sourceLabel + '\'' +
				", noData=" + noData +
				'}';
	}

	private static final String JSON_SOURCE_LABEL = "sourceLabel";
	private static final String JSON_NO_DATA = "noData";

	@NonNull
	public static POIStatus fromCursor(@NonNull Cursor cursor) {
		int idIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_ID);
		Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID));
		int type = cursor.getInt(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TYPE));
		long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE));
		long validityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_VALIDITY));
		long readFromSourceAtInMs; // optional
		int readFromSourceAtColumnIndex = cursor.getColumnIndex(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT);
		if (readFromSourceAtColumnIndex < 0) {
			readFromSourceAtInMs = -1L;
		} else {
			readFromSourceAtInMs = cursor.getLong(readFromSourceAtColumnIndex);
		}
		String sourceLabel = null; // optional
		boolean noData = false; // optional
		try {
			String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
			JSONObject extrasJSON = extrasJSONString.isEmpty() ? null : new JSONObject(extrasJSONString);
			if (extrasJSON != null) {
				noData = extrasJSON.optBoolean(JSON_NO_DATA, false);
				sourceLabel = extrasJSON.optString(JSON_SOURCE_LABEL, null);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while retrieving extras information from cursor.");
		}
		return new POIStatus(id, targetUUID, type, lastUpdateInMs, validityInMs, readFromSourceAtInMs, sourceLabel, noData);
	}

	@NonNull
	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(StatusProviderContract.PROJECTION_STATUS);
		cursor.addRow(new Object[]{
				this.id,
				this.type,
				this.targetUUID,
				this.lastUpdateInMs,
				this.validityInMs,
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
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_VALIDITY, this.validityInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT, this.readFromSourceAtInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_EXTRAS, getExtrasJSONString());
		return contentValues;
	}

	public boolean isUseful() {
		return this.lastUpdateInMs + this.validityInMs >= TimeUtils.currentTimeMillis();
	}

	public boolean isNoData() {
		return this.noData;
	}

	@Nullable
	public String getSourceLabel() {
		return this.sourceLabel;
	}

	public void setSourceLabel(@Nullable String sourceLabel) {
		this.sourceLabel = sourceLabel;
	}

	@Nullable
	private String getExtrasJSONString() {
		try {
			JSONObject extrasJSON = getExtrasJSON();
			if (extrasJSON == null) {
				extrasJSON = new JSONObject();
			}
			extrasJSON.put(JSON_SOURCE_LABEL, this.sourceLabel);
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

	public void setLastUpdateInMs(long lastUpdateInMs) {
		this.lastUpdateInMs = lastUpdateInMs;
	}

	public long getValidityInMs() {
		return validityInMs;
	}

	public void setValidityInMs(long validityInMs) {
		this.validityInMs = validityInMs;
	}

	public long getReadFromSourceAtInMs() {
		return readFromSourceAtInMs;
	}

	public void setReadFromSourceAtInMs(long readFromSourceAtInMs) {
		this.readFromSourceAtInMs = readFromSourceAtInMs;
	}
}
