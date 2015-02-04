package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.provider.StatusFilter;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;

public class AppStatus extends POIStatus implements MTLog.Loggable {

	private static final String TAG = AppStatus.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private boolean appInstalled = false;

	public AppStatus(POIStatus status, boolean appInstalled) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), status.getReadFromSourceAtInMs(), appInstalled);
	}

	public AppStatus(String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled) {
		this(null, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, appInstalled);
	}

	public AppStatus(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_APP, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs);
		setAppInstalled(appInstalled);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(AppStatus.class.getSimpleName()).append(":[") //
				.append("targetUUID:").append(getTargetUUID()).append(',') //
				.append("appInstalled:").append(this.appInstalled) //
				.append(']').toString();
	}

	public void setAppInstalled(boolean appInstalled) {
		if (this.appInstalled != appInstalled) {
			this.appInstalled = appInstalled;
			this.statusMsg = null; // reset
		}
	}

	public boolean isAppInstalled() {
		return appInstalled;
	}

	private CharSequence statusMsg;

	public CharSequence getStatusMsg(Context context) {
		if (this.statusMsg == null) {
			SpannableStringBuilder statusMsbSSB = new SpannableStringBuilder();
			statusMsbSSB.append(isAppInstalled() ? context.getString(R.string.app_status_installed) : context.getString(R.string.app_status_not_installed));
			SpanUtils.set(statusMsbSSB, POIStatus.STATUS_TEXT_FONT);
			SpanUtils.set(statusMsbSSB, POIStatus.getDefaultStatusTextColorSpan(context));
			this.statusMsg = statusMsbSSB;
		}
		return this.statusMsg;
	}

	public static AppStatus fromCursor(Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	private static AppStatus fromExtraJSONString(POIStatus status, String extrasJSONString) {
		try {
			return fromExtraJSON(status, new JSONObject(extrasJSONString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static final String JSON_APP_INSTALLED = "appInstalled";

	private static AppStatus fromExtraJSON(POIStatus status, JSONObject extrasJSON) {
		try {
			boolean appInstalled = extrasJSON.getBoolean(JSON_APP_INSTALLED);
			return new AppStatus(status, appInstalled);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("appInstalled", this.appInstalled);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	public static class AppStatusFilter extends StatusFilter {

		private static final String TAG = AppStatusFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private String pkg;

		public AppStatusFilter(String targetUUID, String pkg) {
			super(POI.ITEM_STATUS_TYPE_APP, targetUUID);
			this.pkg = pkg;
		}

		public String getPkg() {
			return pkg;
		}

		public void setPkg(String pkg) {
			this.pkg = pkg;
		}

		@Override
		public StatusFilter fromJSONStringStatic(String jsonString) {
			return fromJSONString(jsonString);
		}

		public static StatusFilter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		public static StatusFilter fromJSON(JSONObject json) {
			try {
				String targetUUID = StatusFilter.getTargetUUIDFromJSON(json);
				String pkg = json.getString("pkg");
				AppStatusFilter appStatusFilter = new AppStatusFilter(targetUUID, pkg);
				StatusFilter.fromJSON(appStatusFilter, json);
				return appStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Override
		public String toJSONStringStatic(StatusFilter statusFilter) {
			return toJSONString(statusFilter);
		}

		private static String toJSONString(StatusFilter statusFilter) {
			try {
				JSONObject json = toJSON(statusFilter);
				return json == null ? null : json.toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", statusFilter);
				return null;
			}
		}

		private static JSONObject toJSON(StatusFilter statusFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				StatusFilter.toJSON(statusFilter, json);
				if (statusFilter instanceof AppStatusFilter) {
					AppStatusFilter appStatusFilter = (AppStatusFilter) statusFilter;
					json.put("pkg", appStatusFilter.pkg);
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
				return null;
			}
		}
	}

}
