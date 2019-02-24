package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;

public class AppStatus extends POIStatus implements MTLog.Loggable {

	private static final String TAG = AppStatus.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private boolean appInstalled = false;

	public AppStatus(POIStatus status, boolean appInstalled) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), status.getReadFromSourceAtInMs(), appInstalled,
				status.isNoData());
	}

	public AppStatus(String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled) {
		this(null, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, appInstalled);
	}

	public AppStatus(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled) {
		this(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, appInstalled, false);
	}

	public AppStatus(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled, boolean noData) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_APP, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, noData);
		setAppInstalled(appInstalled);
	}

	@NonNull
	@Override
	public String toString() {
		return new StringBuilder().append(AppStatus.class.getSimpleName()).append(":[") //
				.append("targetUUID:").append(getTargetUUID()).append(',') //
				.append("appInstalled:").append(this.appInstalled).append(',') //
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

	@Nullable
	private CharSequence statusMsg;

	private static final TypefaceSpan STATUS_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static ForegroundColorSpan statusTextColor = null;

	private static ForegroundColorSpan getStatusTextColor(Context context) {
		if (statusTextColor == null) {
			statusTextColor = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusTextColor;
	}

	@NonNull
	public CharSequence getStatusMsg(Context context) {
		if (this.statusMsg == null) {
			SpannableStringBuilder statusMsbSSB = new SpannableStringBuilder( //
					isAppInstalled() ? context.getString(R.string.app_status_installed) : context.getString(R.string.app_status_not_installed));
			statusMsbSSB = SpanUtils.setAll(statusMsbSSB, //
					STATUS_FONT, getStatusTextColor(context));
			this.statusMsg = statusMsbSSB;
		}
		return this.statusMsg;
	}

	@Nullable
	public static AppStatus fromCursor(Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	private static AppStatus fromExtraJSONString(POIStatus status, String extrasJSONString) {
		try {
			JSONObject json = extrasJSONString == null ? null : new JSONObject(extrasJSONString);
			if (json == null) {
				return null;
			}
			return fromExtraJSON(status, json);
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
			json.put(JSON_APP_INSTALLED, this.appInstalled);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	public static class AppStatusFilter extends StatusProviderContract.Filter {

		private static final String TAG = AppStatusFilter.class.getSimpleName();

		@NonNull
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
		public StatusProviderContract.Filter fromJSONStringStatic(String jsonString) {
			return fromJSONString(jsonString);
		}

		public static StatusProviderContract.Filter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_PKG = "pkg";

		public static StatusProviderContract.Filter fromJSON(JSONObject json) {
			try {
				String targetUUID = StatusProviderContract.Filter.getTargetUUIDFromJSON(json);
				String pkg = json.getString(JSON_PKG);
				AppStatusFilter appStatusFilter = new AppStatusFilter(targetUUID, pkg);
				StatusProviderContract.Filter.fromJSON(appStatusFilter, json);
				return appStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Override
		public String toJSONStringStatic(@NonNull StatusProviderContract.Filter statusFilter) {
			return toJSONString(statusFilter);
		}

		private static String toJSONString(StatusProviderContract.Filter statusFilter) {
			JSONObject json = toJSON(statusFilter);
			return json == null ? null : json.toString();
		}

		private static JSONObject toJSON(@NonNull StatusProviderContract.Filter statusFilter) {
			try {
				JSONObject json = new JSONObject();
				StatusProviderContract.Filter.toJSON(statusFilter, json);
				if (statusFilter instanceof AppStatusFilter) {
					AppStatusFilter appStatusFilter = (AppStatusFilter) statusFilter;
					json.put(JSON_PKG, appStatusFilter.pkg);
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
				return null;
			}
		}

		@NonNull
		@Override
		public String toString() {
			return new StringBuilder(AppStatusFilter.class.getSimpleName()).append('[') //
					.append("pkg: ").append(this.pkg).append(",") //
					.append(']').toString();
		}
	}
}
