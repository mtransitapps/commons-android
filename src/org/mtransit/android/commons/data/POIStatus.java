package org.mtransit.android.commons.data;

import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;

public class POIStatus implements MTLog.Loggable {

	private static final String TAG = POIStatus.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final TypefaceSpan STATUS_TEXT_FONT = SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN;

	private static Integer defaultStatusTextColor = null;

	public static int getDefaultStatusTextColor(Context context) {
		if (defaultStatusTextColor == null) {
			defaultStatusTextColor = ColorUtils.getTextColorTertiary(context);
		}
		return defaultStatusTextColor;
	}

	private static ForegroundColorSpan defaultStatusTextColorSpan = null;

	public static ForegroundColorSpan getDefaultStatusTextColorSpan(Context context) {
		if (defaultStatusTextColorSpan == null) {
			defaultStatusTextColorSpan = SpanUtils.getTextColor(getDefaultStatusTextColor(context));
		}
		return defaultStatusTextColorSpan;
	}

	private Integer id; // internal DB ID (useful to delete) OR NULL
	private String targetUUID;
	private int type;
	private long lastUpdateInMs;
	private long maxValidityInMs;
	private long readFromSourceAtInMs;

	public POIStatus(Integer id, String targetUUID, int type, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs) {
		this.id = id;
		this.targetUUID = targetUUID;
		this.type = type;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
		this.readFromSourceAtInMs = readFromSourceAtInMs;
	}

	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getSimpleName()).append('[') //
				.append("id:").append(this.id) //
				.append(',') //
				.append("targetUUID:").append(this.targetUUID) //
				.append(',') //
				.append("type:").append(this.type) //
				.append(',') //
				.append("readFromSourceAtInMs:").append(this.readFromSourceAtInMs) //
				.append(']').toString();
	}

	public static POIStatus fromCursor(Cursor cursor) {
		int idIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_ID);
		Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID));
		int type = cursor.getInt(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TYPE));
		long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE));
		long maxValidityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_MAX_VALIDITY_IN_MS));
		long readFromSourceAtInMs; // optional
		int readFromSourceAtColumnIndex = cursor.getColumnIndex(StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS);
		if (readFromSourceAtColumnIndex < 0) {
			readFromSourceAtInMs = -1;
		} else {
			readFromSourceAtInMs = cursor.getLong(readFromSourceAtColumnIndex);
		}
		return new POIStatus(id, targetUUID, type, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs);
	}

	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(StatusProviderContract.PROJECTION_STATUS);
		cursor.addRow(new Object[] { this.id, this.type, this.targetUUID, this.lastUpdateInMs, this.maxValidityInMs, this.readFromSourceAtInMs,
				getExtrasJSONString() });
		return cursor;
	}

	public static int getTypeFromCursor(Cursor c) {
		return c.getInt(c.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TYPE));
	}

	public static String getTargetUUIDFromCursor(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID));
	}

	public static long getLastUpdateInMsFromCursor(Cursor c) {
		return c.getLong(c.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE));
	}

	public static String getExtrasFromCursor(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(StatusProviderContract.Columns.T_STATUS_K_EXTRAS));
	}

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

	private String getExtrasJSONString() {
		try {
			JSONObject extrasJSON = getExtrasJSON();
			if (extrasJSON == null) {
				return null;
			}
			return extrasJSON.toString();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting JSON to String!");
			return null;
		}
	}

	public JSONObject getExtrasJSON() {
		return null; // no extra JSON in default status implementation
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTargetUUID() {
		return targetUUID;
	}

	public void setTargetUUID(String targetUUID) {
		this.targetUUID = targetUUID;
	}

	public int getType() {
		return type;
	}

	public long getLastUpdateInMs() {
		return lastUpdateInMs;
	}

	public void setLastUpdateMs(long lastUpdateMs) {
		this.lastUpdateInMs = lastUpdateMs;
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
