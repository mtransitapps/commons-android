package org.mtransit.android.commons.data;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AppStatus extends POIStatus implements MTLog.Loggable {

	private static final String LOG_TAG = AppStatus.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private boolean appInstalled = false;

	private boolean appEnabled = true;

	private boolean updateAvailable = false;

	public AppStatus(@NonNull POIStatus status, boolean appInstalled, boolean appEnabled, boolean updateAvailable) {
		this(
				status.getId(),
				status.getTargetUUID(),
				status.getLastUpdateInMs(),
				status.getMaxValidityInMs(),
				status.getReadFromSourceAtInMs(),
				appInstalled,
				appEnabled,
				updateAvailable,
				status.getSourceLabel(),
				status.isNoData()
		);
	}

	public AppStatus(@Nullable Integer id, @NonNull String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs,
					 boolean appInstalled, boolean appEnabled, boolean updateAvailable,
					 @Nullable String sourceLabel, boolean noData) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_APP, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, noData);
		setAppInstalled(appInstalled);
		setAppEnabled(appEnabled);
		setUpdateAvailable(updateAvailable);
	}

	@NonNull
	@Override
	public String toString() {
		return AppStatus.class.getSimpleName() + "{" +
				"targetUUID:" + getTargetUUID() +
				", appInstalled=" + appInstalled +
				", appEnabled=" + appEnabled +
				", updateAvailable=" + updateAvailable +
				'}';
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

	public void setUpdateAvailable(boolean updateAvailable) {
		if (this.updateAvailable != updateAvailable) {
			this.updateAvailable = updateAvailable;
			this.statusMsg = null; // reset
		}
	}

	public boolean isAppInstalled() {
		return appInstalled;
	}

	public boolean isAppEnabled() {
		return appEnabled;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable;
	}

	@Nullable
	private CharSequence statusMsg;

	private static final TypefaceSpan STATUS_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	@Nullable
	private static ForegroundColorSpan statusTextColor = null;

	@NonNull
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
			if (isAppInstalled()) {
				if (isAppEnabled()) {
					if (isUpdateAvailable()) {
						statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_update_available));
					} else {
						statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_installed));
					}
				} else { // APP NOT ENABLED!
					statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_warning));
					SpanUtils.setAllNN(statusMsbSSB, SpanUtils.getNew150PercentSizeSpan());
				}
			} else {
				statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_not_installed));
			}
			SpanUtils.setAllNN(statusMsbSSB, STATUS_FONT, getStatusTextColor(context));
			this.statusMsg = statusMsbSSB;
		}
		return this.statusMsg;
	}

	@NonNull
	public static AppStatus fromCursorWithExtra(@NonNull Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	@NonNull
	private static AppStatus fromExtraJSONString(@NonNull POIStatus status, @Nullable String extrasJSONString) {
		try {
			final JSONObject json = extrasJSONString == null ? null : new JSONObject(extrasJSONString);
			return fromExtraJSON(status, json);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return fromExtraJSON(status, null);
		}
	}

	private static final String JSON_APP_INSTALLED = "appInstalled";
	private static final String JSON_APP_ENABLED = "appEnabled";
	private static final String JSON_UPDATE_AVAILABLE = "updateAvailable";

	@NonNull
	private static AppStatus fromExtraJSON(@NonNull POIStatus status, @Nullable JSONObject extrasJSON) {
		return new AppStatus(status,
				JSONUtils.optBoolean(extrasJSON, JSON_APP_INSTALLED, false),
				JSONUtils.optBoolean(extrasJSON, JSON_APP_ENABLED, true),
				JSONUtils.optBoolean(extrasJSON, JSON_UPDATE_AVAILABLE, false)
		);
	}

	@Nullable
	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_APP_INSTALLED, this.appInstalled);
			json.put(JSON_APP_ENABLED, this.appEnabled);
			json.put(JSON_UPDATE_AVAILABLE, this.updateAvailable);
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
				MTLog.w(LOG_TAG, jsone, "Error while making JSON object '%s'", statusFilter);
				return null;
			}
		}
	}
}
