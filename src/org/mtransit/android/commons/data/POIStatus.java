package org.mtransit.android.commons.data;

import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.StatusProvider.StatusColumns;

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
			// defaultStatusTextColor = ColorUtils.getTextColorSecondary(context);
			defaultStatusTextColor = ColorUtils.getTextColorTertiary(context);
		}
		return defaultStatusTextColor.intValue();
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

	public POIStatus(Integer id, String targetUUID, int type, long lastUpdateInMs, long maxValidityInMs) {
		this.id = id;
		this.targetUUID = targetUUID;
		this.type = type;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
	}

	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getSimpleName()).append('[') //
				.append("id:").append(this.id) //
				.append(',') //
				.append("targetUUID:").append(this.targetUUID) //
				.append(',') //
				.append("type:").append(this.type) //
				.append(']').toString();
	}

	public String toJSONString() {
		try {
			return toJSON().toString();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("type", getType());
			json.put("target", getTargetUUID());
			json.put("lastUpdateInMs", getLastUpdateInMs());
			json.put("extras", getExtrasJSONString());
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	public static POIStatus fromCursor(Cursor cursor) {
		final int idIdx = cursor.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_ID);
		final Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		final String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_TARGET_UUID));
		final int type = cursor.getInt(cursor.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_TYPE));
		final long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_LAST_UPDATE));
		final long maxValidityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_MAX_VALIDITY_IN_MS));
		return new POIStatus(id, targetUUID, type, lastUpdateInMs, maxValidityInMs);
	}

	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(new String[] { StatusColumns.T_STATUS_K_ID, StatusColumns.T_STATUS_K_TARGET_UUID, StatusColumns.T_STATUS_K_TYPE,
				StatusColumns.T_STATUS_K_LAST_UPDATE, StatusColumns.T_STATUS_K_MAX_VALIDITY_IN_MS, StatusColumns.T_STATUS_K_EXTRAS });
		cursor.addRow(new Object[] { id, targetUUID, type, lastUpdateInMs, maxValidityInMs, getExtrasJSONString() });
		return cursor;
	}

	public static int getTypeFromCursor(Cursor c) {
		return c.getInt(c.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_TYPE));
	}

	public static String getTargetUUIDFromCursor(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_TARGET_UUID));
	}

	public static long getLastUpdateInMsFromCursor(Cursor c) {
		return c.getLong(c.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_LAST_UPDATE));
	}

	public static String getExtrasFromCursor(Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(StatusColumns.T_STATUS_K_EXTRAS));
	}

	public ContentValues toContentValues() {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(StatusColumns.T_STATUS_K_TYPE, this.type);
		contentValues.put(StatusColumns.T_STATUS_K_TARGET_UUID, this.targetUUID);
		contentValues.put(StatusColumns.T_STATUS_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(StatusColumns.T_STATUS_K_MAX_VALIDITY_IN_MS, this.maxValidityInMs);
		contentValues.put(StatusColumns.T_STATUS_K_EXTRAS, getExtrasJSONString());
		return contentValues;
	}

	public JSONObject getExtrasJSON() {
		return null; // no extra JSON in default status implementation
	}

	public boolean isUseful() {
		return this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis();
	}

	private String getExtrasJSONString() {
		try {
			return getExtrasJSON().toString();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting JSON to String!");
			return null;
		}
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

}
