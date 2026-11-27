package org.mtransit.android.commons.data;

import static org.mtransit.android.commons.data.POI.ItemStatusType;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.status.StatusProviderContract;

import kotlinx.serialization.json.JsonObject;

// TODO abstract
public class POIStatus implements MTLog.Loggable {

	protected static final String LOG_TAG = POIStatus.class.getSimpleName();

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
	protected final Integer id; // internal DB ID (useful to delete) OR NULL
	@NonNull
	protected String targetUUID;
	@ItemStatusType
	private final int type;
	protected long lastUpdateInMs;
	protected long maxValidityInMs;
	protected long readFromSourceAtInMs;
	@Nullable
	protected String sourceLabel;
	protected final boolean noData; // status with 'no data' means loaded from provider w/o any data, no need to fetch another one

	public POIStatus(
			@Nullable Integer id,
			@NonNull String targetUUID,
			@ItemStatusType int type,
			long lastUpdateInMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			@Nullable String sourceLabel,
			boolean noData
	) {
		this.id = id;
		this.targetUUID = targetUUID;
		this.type = type;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
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
				", maxValidity=" + MTLog.formatDuration(maxValidityInMs) +
				", readFromSourceAt=" + MTLog.formatDateTime(readFromSourceAtInMs) +
				", sourceLabel='" + sourceLabel + '\'' +
				", noData=" + noData +
				'}';
	}

	@NonNull
	public static POIStatus fromCursor(@NonNull Cursor cursor) {
		final int idIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_ID);
		final Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		final String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID));
		final int type = cursor.getInt(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TYPE));
		final long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE));
		final long validityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_MAX_VALIDITY));
		final long readFromSourceAtInMs; // optional
		final int readFromSourceAtColumnIndex = cursor.getColumnIndex(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT);
		if (readFromSourceAtColumnIndex < 0) {
			readFromSourceAtInMs = -1L;
		} else {
			readFromSourceAtInMs = cursor.getLong(readFromSourceAtColumnIndex);
		}
		String sourceLabel = null; // optional
		boolean noData = false; // optional
		try {
			final String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
			final JsonObject extrasJSON = POIStatusExtKt.toJsonObject(extrasJSONString);
			if (extrasJSON != null) {
				noData = POIStatusExtKt.getNoDataFromJsonOrDefault(extrasJSON);
				sourceLabel = POIStatusExtKt.getSourceLabelFromJson(extrasJSON);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while retrieving extras information from cursor.");
		}
		return new POIStatus(id, targetUUID, type, lastUpdateInMs, validityInMs, readFromSourceAtInMs, sourceLabel, noData);
	}

	@NonNull
	public Cursor toCursor() {
		final MatrixCursor cursor = new MatrixCursor(StatusProviderContract.getPROJECTION_STATUS());
		cursor.addRow(new Object[]{
				this.id,
				this.type,
				this.targetUUID,
				this.lastUpdateInMs,
				this.maxValidityInMs,
				this.readFromSourceAtInMs,
				getExtrasJsonStringOrNull()
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
		final ContentValues contentValues = new ContentValues();
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_TYPE, this.type);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, this.targetUUID);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_MAX_VALIDITY, this.maxValidityInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT, this.readFromSourceAtInMs);
		contentValues.put(StatusProviderContract.Columns.T_STATUS_K_EXTRAS, getExtrasJsonStringOrNull());
		return contentValues;
	}

	public boolean isUseful() {
		return isValid();
	}

	public boolean isValid() {
		return TimeUtils.currentTimeMillis() <= this.lastUpdateInMs + this.maxValidityInMs;
	}

	public boolean isNoData() {
		return this.noData;
	}

	public boolean isUsefulOrNoData() {
		return isUseful() || isNoData();
	}

	@Nullable
	public String getSourceLabel() {
		return this.sourceLabel;
	}

	public void setSourceLabel(@Nullable String sourceLabel) {
		this.sourceLabel = sourceLabel;
	}

	@Nullable
	public String getExtrasJsonStringOrNull() {
		return POIStatusExtKt.toJsonStringOrNull(this); // to override in super class
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

	public long getMaxValidityInMs() {
		return maxValidityInMs;
	}

	public void setMaxValidityInMs(long maxValidityInMs) {
		this.maxValidityInMs = maxValidityInMs;
	}

	public long getReadFromSourceAtInMs() {
		return readFromSourceAtInMs;
	}

	public void setReadFromSourceAtInMs(long readFromSourceAtInMs) {
		this.readFromSourceAtInMs = readFromSourceAtInMs;
	}
}
