package org.mtransit.android.commons.data;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.commons.provider.ScheduleTimestampsProvider.ScheduleTimeStampsColumns;

import android.database.Cursor;
import android.database.MatrixCursor;

public class ScheduleTimestamps implements MTLog.Loggable {

	private static final String TAG = ScheduleTimestamps.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<Timestamp> timestamps = new ArrayList<Timestamp>();
	private String targetUUID;
	private long startsAtInMs;
	private long endsAtInMs;

	public ScheduleTimestamps(String targetUUID, long startsAtInMs, long endsAtInMs) {
		this.targetUUID = targetUUID;
		this.startsAtInMs = startsAtInMs;
		this.endsAtInMs = endsAtInMs;
	}

	private void addTimestampWithoutSort(Timestamp newTimestamp) {
		this.timestamps.add(newTimestamp);
	}

	public void setTimestampsAndSort(ArrayList<Timestamp> timestamps) {
		this.timestamps = timestamps;
		sortTimestamps();
	}

	public void sortTimestamps() {
		CollectionUtils.sort(this.timestamps, Schedule.TIMESTAMPS_COMPARATOR);
	}

	public ArrayList<Timestamp> getTimestamps() {
		return this.timestamps;
	}

	public int getTimestampsCount() {
		if (this.timestamps == null) {
			return 0;
		}
		return this.timestamps.size();
	}

	public static ScheduleTimestamps fromCursor(Cursor cursor) {
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID));
		long startsAtInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_STARTS_AT));
		long endsAtInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_ENDS_AT));
		ScheduleTimestamps scheduleTimestamps = new ScheduleTimestamps(targetUUID, startsAtInMs, endsAtInMs);
		String extrasJSONString = cursor.getString(cursor.getColumnIndexOrThrow(ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_EXTRAS));
		return fromExtraJSONString(scheduleTimestamps, extrasJSONString);
	}

	private static ScheduleTimestamps fromExtraJSONString(ScheduleTimestamps scheduleTimestamps, String extrasJSONString) {
		try {
			return fromExtraJSON(scheduleTimestamps, new JSONObject(extrasJSONString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static ScheduleTimestamps fromExtraJSON(ScheduleTimestamps scheduleTimestamps, JSONObject extrasJSON) {
		try {
			JSONArray jTimestamps = extrasJSON.getJSONArray("timestamps");
			for (int i = 0; i < jTimestamps.length(); i++) {
				JSONObject jTimestamp = jTimestamps.getJSONObject(i);
				scheduleTimestamps.addTimestampWithoutSort(Timestamp.parseJSON(jTimestamp));
			}
			scheduleTimestamps.sortTimestamps();
			return scheduleTimestamps;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(new String[] { ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID,
				ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_STARTS_AT, ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_ENDS_AT,
				ScheduleTimeStampsColumns.T_SCHEDULE_TIMESTAMPS_K_EXTRAS });
		cursor.addRow(new Object[] { targetUUID, startsAtInMs, endsAtInMs, getExtrasJSONString() });
		return cursor;
	}

	private String getExtrasJSONString() {
		try {
			return getExtrasJSON().toString();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting JSON to String!");
			return null;
		}
	}

	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			JSONArray jTimestamps = new JSONArray();
			for (Timestamp timestamp : this.timestamps) {
				jTimestamps.put(timestamp.toJSON());
			}
			json.put("timestamps", jTimestamps);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}
}
