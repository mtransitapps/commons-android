package org.mtransit.android.commons.data;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;

import kotlinx.serialization.SerializationException;
import kotlinx.serialization.json.JsonObject;

public class AppStatus extends POIStatus implements MTLog.Loggable {

	protected static final String LOG_TAG = AppStatus.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private boolean appInstalled = false;

	private boolean appEnabled = true;

	private boolean updateAvailable = false;

	protected AppStatus(@NonNull POIStatus status, boolean appInstalled, boolean appEnabled, boolean updateAvailable) {
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

	public AppStatus(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateInMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			boolean appInstalled,
			boolean appEnabled,
			boolean updateAvailable,
			@Nullable String sourceLabel
	) {
		this(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, appInstalled, appEnabled, updateAvailable, sourceLabel, false);
	}

	private AppStatus(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateInMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			boolean appInstalled,
			boolean appEnabled,
			boolean updateAvailable,
			@Nullable String sourceLabel,
			boolean noData
	) {
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

	private void setAppInstalled(boolean appInstalled) {
		if (this.appInstalled != appInstalled) {
			this.appInstalled = appInstalled;
			this.statusMsg = null; // reset
			this.statusMsgA11y = null; // reset
		}
	}

	private void setAppEnabled(boolean appEnabled) {
		if (this.appEnabled != appEnabled) {
			this.appEnabled = appEnabled;
			this.statusMsg = null; // reset
			this.statusMsgA11y = null; // reset
		}
	}

	private void setUpdateAvailable(boolean updateAvailable) {
		if (this.updateAvailable != updateAvailable) {
			this.updateAvailable = updateAvailable;
			this.statusMsg = null; // reset
			this.statusMsgA11y = null; // reset
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
	@Nullable
	private CharSequence statusMsgA11y;

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
						SpanUtils.setAllNN(statusMsbSSB, SpanUtils.getNew150PercentSizeSpan());
						SpanUtils.setAllNN(statusMsbSSB, SpanUtils.getNewBoldStyleSpan());
					} else {
						statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_installed));
						// Bold & big enough by default
					}
				} else { // APP NOT ENABLED!
					statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_warning));
					SpanUtils.setAllNN(statusMsbSSB, SpanUtils.getNew150PercentSizeSpan());
					SpanUtils.setAllNN(statusMsbSSB, SpanUtils.getNewBoldStyleSpan());
				}
			} else {
				statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.app_status_not_installed));
				// Big enough by default
				SpanUtils.setAllNN(statusMsbSSB, SpanUtils.getNewBoldStyleSpan());
			}
			SpanUtils.setAllNN(statusMsbSSB, STATUS_FONT, getStatusTextColor(context));
			this.statusMsg = statusMsbSSB;
		}
		return this.statusMsg;
	}

	public @NonNull CharSequence getStatusMsgA11y(@NonNull Context context) {
		if (this.statusMsgA11y == null) {
			SpannableStringBuilder statusMsbA11ySSB;
			if (isAppInstalled()) {
				if (isAppEnabled()) {
					if (isUpdateAvailable()) {
						statusMsbA11ySSB = new SpannableStringBuilder(context.getString(R.string.app_status_update_available_a11y));
					} else {
						statusMsbA11ySSB = new SpannableStringBuilder(context.getString(R.string.app_status_installed_a11y));
					}
				} else { // APP NOT ENABLED!
					statusMsbA11ySSB = new SpannableStringBuilder(context.getString(R.string.app_status_warning_a11y));
				}
			} else {
				statusMsbA11ySSB = new SpannableStringBuilder(context.getString(R.string.app_status_not_installed_a11y));
			}
			this.statusMsgA11y = statusMsbA11ySSB;
		}
		return this.statusMsgA11y;
	}

	@NonNull
	public static AppStatus fromCursorWithExtra(@NonNull Cursor cursor) {
		final POIStatus status = POIStatus.fromCursor(cursor);
		final String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	@NonNull
	private static AppStatus fromExtraJSONString(@NonNull POIStatus status, @Nullable String extrasJSONString) {
		try {
			return AppStatusExtKt.toAppStatus(status, extrasJSONString);
		} catch (SerializationException se) {
			MTLog.w(LOG_TAG, se, "Error while retrieving extras information from cursor.");
			return AppStatusExtKt.toAppStatus(status);
		}
	}

	@NonNull
	@Override
	public String getExtrasJsonString() {
		return AppStatusExtKt.toJsonString(this);
	}
}
