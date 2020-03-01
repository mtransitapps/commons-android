package org.mtransit.android.commons.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;

public class AppStatus extends POIStatus implements MTLog.Loggable {

	private static final String LOG_TAG = AppStatus.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private boolean appInstalled = false;

	private boolean appEnabled = true;

	public AppStatus(@NonNull POIStatus status, boolean appInstalled, boolean appEnabled) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), status.getReadFromSourceAtInMs(), appInstalled, appEnabled,
				status.isNoData());
	}

	public AppStatus(String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled, boolean appEnabled) {
		this(null, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, appInstalled, appEnabled);
	}

	public AppStatus(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled, boolean appEnabled) {
		this(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, appInstalled, appEnabled, false);
	}

	public AppStatus(Integer id, String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, boolean appInstalled, boolean appEnabled, boolean noData) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_APP, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, noData);
		setAppInstalled(appInstalled);
		setAppEnabled(appEnabled);
	}

	@NonNull
	@Override
	public String toString() {
		return new StringBuilder().append(AppStatus.class.getSimpleName()).append(":[") //
				.append("targetUUID:").append(getTargetUUID()).append(',') //
				.append("appInstalled:").append(this.appInstalled) //
				.append("appEnabled:").append(this.appEnabled) //
				.append(']').toString();
	}

	public void setAppInstalled(boolean appInstalled) {
		if (this.appInstalled != appInstalled) {
			this.appInstalled = appInstalled;
			this.statusMsg = null; // reset
		}
	}

	public void setAppEnabled(boolean appEnabled) {
		if (this.appEnabled != appEnabled) {
			this.appEnabled = appEnabled;
			this.statusMsg = null; // reset
		}
	}

	public boolean isAppInstalled() {
		return appInstalled;
	}

	public boolean isAppEnabled() {
		return appEnabled;
	}

	@Nullable
	private CharSequence statusMsg;

	private static final TypefaceSpan STATUS_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	@Nullable
	private static ForegroundColorSpan statusTextColor = null;

	private static ForegroundColorSpan getStatusTextColor(@NonNull Context context) {
		if (statusTextColor == null) {
			statusTextColor = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusTextColor;
	}

	public static void resetColorCache() {
		statusTextColor = null;
	}

	@NonNull
	public CharSequence getStatusMsg(@NonNull Context context) {
		if (this.statusMsg == null) {
			SpannableStringBuilder statusMsbSSB;
			if (this.appInstalled) {
				if (this.appEnabled) {
					statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_installed));
				} else { // APP NOT ENABLED!
					statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_warning));
				}
			} else {
				statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_not_installed));
			}
			statusMsbSSB = SpanUtils.setAllNN(statusMsbSSB, //
					STATUS_FONT, getStatusTextColor(context));
			this.statusMsg = statusMsbSSB;
		}
		return this.statusMsg;
	}

	@Nullable
	public static AppStatus fromCursorWithExtra(@NonNull Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	@Nullable
	private static AppStatus fromExtraJSONString(@NonNull POIStatus status, @Nullable String extrasJSONString) {
		try {
			JSONObject json = extrasJSONString == null ? null : new JSONObject(extrasJSONString);
			if (json == null) {
				return null;
			}
			return fromExtraJSON(status, json);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static final String JSON_APP_INSTALLED = "appInstalled";
	private static final String JSON_APP_ENABLED = "appEnabled";

	@Nullable
	private static AppStatus fromExtraJSON(@NonNull POIStatus status, @NonNull JSONObject extrasJSON) {
		try {
			boolean appInstalled = extrasJSON.getBoolean(JSON_APP_INSTALLED);
			boolean appEnabled = extrasJSON.optBoolean(JSON_APP_ENABLED, true);
			return new AppStatus(status, appInstalled, appEnabled);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Nullable
	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_APP_INSTALLED, this.appInstalled);
			json.put(JSON_APP_ENABLED, this.appEnabled);
			return json;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	public static class AppStatusFilter extends StatusProviderContract.Filter {

		private static final String LOG_TAG = AppStatusFilter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final String pkg;

		public AppStatusFilter(@NonNull String targetUUID, @NonNull String pkg) {
			super(POI.ITEM_STATUS_TYPE_APP, targetUUID);
			this.pkg = pkg;
		}

		@NonNull
		public String getPkg() {
			return pkg;
		}

		@Nullable
		@Override
		public StatusProviderContract.Filter fromJSONStringStatic(@Nullable String jsonString) {
			return fromJSONString(jsonString);
		}

		@Nullable
		public static StatusProviderContract.Filter fromJSONString(@Nullable String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_PKG = "pkg";

		@Nullable
		public static StatusProviderContract.Filter fromJSON(@NonNull JSONObject json) {
			try {
				String targetUUID = StatusProviderContract.Filter.getTargetUUIDFromJSON(json);
				String pkg = json.getString(JSON_PKG);
				AppStatusFilter appStatusFilter = new AppStatusFilter(targetUUID, pkg);
				StatusProviderContract.Filter.fromJSON(appStatusFilter, json);
				return appStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Nullable
		@Override
		public String toJSONStringStatic(@NonNull StatusProviderContract.Filter statusFilter) {
			return toJSONString(statusFilter);
		}

		@Nullable
		private static String toJSONString(@NonNull StatusProviderContract.Filter statusFilter) {
			JSONObject json = toJSON(statusFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
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
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
				return null;
			}
		}
	}
}
