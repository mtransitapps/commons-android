package org.mtransit.android.commons.data;

import android.database.Cursor;
import android.database.MatrixCursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract;
import org.mtransit.commons.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ScheduleTimestamps implements MTLog.Loggable {

	private static final String LOG_TAG = ScheduleTimestamps.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private List<Schedule.Timestamp> timestamps = new ArrayList<>();
	@Nullable
	private String sourceLabel = null;
	@NonNull
	private final String targetUUID;
	private final long startsAtInMs;
	private final long endsAtInMs;

	public ScheduleTimestamps(@NonNull String targetUUID, long startsAtInMs, long endsAtInMs) {
		this.targetUUID = targetUUID;
		this.startsAtInMs = startsAtInMs;
		this.endsAtInMs = endsAtInMs;
	}

	private void addTimestampWithoutSort(Schedule.Timestamp newTimestamp) {
		this.timestamps.add(newTimestamp);
	}

	public void setTimestampsAndSort(@NonNull List<Schedule.Timestamp> timestamps) {
		this.timestamps = timestamps;
		sortTimestamps();
	}

	public void sortTimestamps() {
		CollectionUtils.sort(this.timestamps, Schedule.TIMESTAMPS_COMPARATOR);
	}

	@NonNull
	public List<Schedule.Timestamp> getTimestamps() {
		return this.timestamps;
	}

	public int getTimestampsCount() {
		return this.timestamps.size();
	}

	public void setSourceLabel(@Nullable String sourceLabel) {
		this.sourceLabel = sourceLabel;
	}

	@Nullable
	public String getSourceLabel() {
		return sourceLabel;
	}

	@Nullable
	public static ScheduleTimestamps fromCursor(@NonNull Cursor cursor) {
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID));
		long startsAtInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_STARTS_AT));
		long endsAtInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_ENDS_AT));
		ScheduleTimestamps scheduleTimestamps = new ScheduleTimestamps(targetUUID, startsAtInMs, endsAtInMs);
		String extrasJSONString = cursor.getString(cursor.getColumnIndexOrThrow(ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_EXTRAS));
		return fromExtraJSONString(scheduleTimestamps, extrasJSONString);
	}

	@Nullable
	private static ScheduleTimestamps fromExtraJSONString(ScheduleTimestamps scheduleTimestamps, String extrasJSONString) {
		try {
			JSONObject json = extrasJSONString == null ? null : new JSONObject(extrasJSONString);
			if (json == null) {
				return null;
			}
			return fromExtraJSON(scheduleTimestamps, json);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static final String JSON_SOURCE_LABEL = "sourceLabel";
	private static final String JSON_TIMESTAMPS = "timestamps";

	private static ScheduleTimestamps fromExtraJSON(ScheduleTimestamps scheduleTimestamps, JSONObject extrasJSON) {
		try {
			scheduleTimestamps.setSourceLabel(extrasJSON.optString(JSON_SOURCE_LABEL, null));
			JSONArray jTimestamps = extrasJSON.getJSONArray(JSON_TIMESTAMPS);
			for (int i = 0; i < jTimestamps.length(); i++) {
				JSONObject jTimestamp = jTimestamps.getJSONObject(i);
				scheduleTimestamps.addTimestampWithoutSort(Schedule.Timestamp.parseJSON(jTimestamp));
			}
			scheduleTimestamps.sortTimestamps();
			return scheduleTimestamps;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@NonNull
	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(new String[]{
				ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID,
				ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_STARTS_AT,
				ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_ENDS_AT,
				ScheduleTimestampsProviderContract.Columns.T_SCHEDULE_TIMESTAMPS_K_EXTRAS
		});
		cursor.addRow(new Object[]{targetUUID, startsAtInMs, endsAtInMs, getExtrasJSONString()});
		return cursor;
	}

	@Nullable
	private String getExtrasJSONString() {
		try {
			JSONObject extrasJSON = getExtrasJSON();
			return extrasJSON == null ? null : extrasJSON.toString();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while converting JSON to String!");
			return null;
		}
	}

	@SuppressWarnings("WeakerAccess")
	@Nullable
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_SOURCE_LABEL, this.sourceLabel);
			JSONArray jTimestamps = new JSONArray();
			for (Schedule.Timestamp timestamp : this.timestamps) {
				jTimestamps.put(timestamp.toJSON());
			}
			json.put(JSON_TIMESTAMPS, jTimestamps);
			return json;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	@NonNull
	@Override
	public String toString() {
		return ScheduleTimestamps.class.getSimpleName() + "{" +
				"targetUUID='" + targetUUID + '\'' +
				", startsAtInMs=" + startsAtInMs +
				", endsAtInMs=" + endsAtInMs +
				", timestamps[" + timestamps.size() + "]=" + timestamps +
				'}';
	}
}
