package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract;

import java.util.Comparator;

public class ServiceUpdate implements MTLog.Loggable {

	private static final String TAG = ServiceUpdate.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final HigherSeverityFirstComparator HIGHER_SEVERITY_FIRST_COMPARATOR = new HigherSeverityFirstComparator();

	// order and values are important (force DB reset when changing values)
	public static final int SEVERITY_NONE = 0; // no message
	public static final int SEVERITY_INFO_UNKNOWN = 1; // unexpected information message
	public static final int SEVERITY_INFO_AGENCY = 2; // concerns most if not all POIs in this agency
	public static final int SEVERITY_INFO_RELATED_POI = 3; // RDS other stops on the same route [direction]
	public static final int SEVERITY_INFO_POI = 4; // related to this POI but not warning
	public static final int SEVERITY_WARNING_UNKNOWN = 5; // unexpected warning message
	public static final int SEVERITY_WARNING_AGENCY = 6; // unexpected warning message
	public static final int SEVERITY_WARNING_RELATED_POI = 7; // related to this POI and it's important enough to bother user with it
	public static final int SEVERITY_WARNING_POI = 8; // related to this POI and it's important enough to bother user with it

	@Nullable
	private final Integer id; // internal DB ID (useful to delete) OR NULL
	@NonNull
	private String targetUUID;
	@Nullable
	private final String targetTripId;
	private final long lastUpdateInMs;
	private final long maxValidityInMs;
	private final String text;
	@Nullable
	private String textHTML;
	private int severity;
	private final String language;
	@NonNull
	private final String sourceLabel;
	private final String sourceId;
	private final String originalId; // ID from the provider to remove duplicates

	public ServiceUpdate(
			@Nullable Integer optId,
			@NonNull String targetUUID,
			@Nullable String targetTripId,
			long lastUpdateInMs,
			long maxValidityInMs,
			@NonNull String text,
			@Nullable String optTextHTML,
			int severity,
			@NonNull String sourceId,
			@NonNull String sourceLabel,
			@Nullable String originalId,
			String language
	) {
		this.id = optId;
		this.targetUUID = targetUUID;
		this.targetTripId = targetTripId;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
		this.text = text;
		this.textHTML = optTextHTML;
		this.severity = severity;
		this.sourceId = sourceId;
		this.sourceLabel = sourceLabel;
		this.originalId = originalId;
		this.language = language;
	}

	public void setTargetUUID(@NonNull String targetUUID) {
		this.targetUUID = targetUUID;
	}

	@NonNull
	public String getTargetUUID() {
		return targetUUID;
	}

	@Nullable
	public String getTargetTripId() {
		return targetTripId;
	}

	public boolean isSeverityWarning() {
		return isSeverityWarning(this.severity);
	}

	public static boolean isSeverityWarning(int severity) {
		return severity == SEVERITY_WARNING_UNKNOWN //
				|| severity == SEVERITY_WARNING_AGENCY //
				|| severity == SEVERITY_WARNING_RELATED_POI //
				|| severity == SEVERITY_WARNING_POI; //
	}

	public boolean isSeverityInfo() {
		return isSeverityInfo(severity);
	}

	public static boolean isSeverityInfo(int severity) {
		return !isSeverityWarning(severity) //
				&& (severity == SEVERITY_INFO_UNKNOWN //
				|| severity == SEVERITY_INFO_AGENCY //
				|| severity == SEVERITY_INFO_RELATED_POI //
				|| severity == SEVERITY_INFO_POI);
	}

	public static boolean isSeverityWarning(@Nullable Iterable<ServiceUpdate> serviceUpdates) {
		if (serviceUpdates != null) {
			for (ServiceUpdate serviceUpdate : serviceUpdates) {
				if (serviceUpdate.isSeverityWarning()) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isSeverityInfo(@Nullable Iterable<ServiceUpdate> serviceUpdates) {
		if (serviceUpdates != null) {
			for (ServiceUpdate serviceUpdate : serviceUpdates) {
				if (serviceUpdate.isSeverityWarning()) {
					return false;
				}
				if (serviceUpdate.isSeverityInfo()) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	public boolean hasMessage() {
		return this.severity != SEVERITY_NONE;
	}

	public int getSeverity() {
		return severity;
	}

	public void setSeverity(int severity) {
		this.severity = severity;
	}

	@SuppressWarnings("unused")
	public String getSourceId() {
		return sourceId;
	}

	@NonNull
	public String getSourceLabel() {
		return sourceLabel;
	}

	@Nullable
	public String getOriginalId() {
		return originalId;
	}

	@NonNull
	public String getText() {
		return text;
	}

	@NonNull
	public String getTextHTML() {
		if (TextUtils.isEmpty(textHTML)) {
			return text;
		}
		return textHTML;
	}

	public void setTextHTML(@Nullable String textHTML) {
		this.textHTML = textHTML;
	}

	public String getLanguage() {
		return language;
	}

	public boolean shouldDisplay() {
		return this.severity != SEVERITY_NONE
				&& !(TextUtils.isEmpty(this.text) && TextUtils.isEmpty(this.textHTML));
	}

	@NonNull
	@Override
	public String toString() {
		return ServiceUpdate.class.getSimpleName() + '[' + //
				"id:" + this.id + //
				',' + //
				"oId:" + this.originalId + //
				',' + //
				"tUUID:" + this.targetUUID + //
				',' + //
				"tTrip:" + this.targetTripId + //
				',' + //
				"lang:" + this.language + //
				',' + //
				"txt:" + this.text + //
				',' + //
				"svrt:" + this.severity + //
				']';
	}

	public boolean isUseful() {
		return this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis();
	}

	@Nullable
	public Integer getId() {
		return this.id;
	}

	public long getLastUpdateInMs() {
		return lastUpdateInMs;
	}

	@NonNull
	public static ServiceUpdate fromCursor(@NonNull Cursor cursor) {
		final int idIdx = cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ID);
		final Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		final String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID));
		final String targetTripId = CursorExtKt.optString(cursor, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID, null);
		final long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LAST_UPDATE));
		final long maxValidityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS));
		final int severity = cursor.getInt(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SEVERITY));
		final String text = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT));
		final String htmlText = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT_HTML));
		final String language = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE));
		final String originalId = CursorExtKt.optString(cursor, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ORIGINAL_ID, null);
		final String sourceLabel = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL));
		final String sourceId = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID));
		return new ServiceUpdate(id, targetUUID, targetTripId, lastUpdateInMs, maxValidityInMs, text, htmlText, severity, sourceId, sourceLabel, originalId, language);
	}

	/**
	 * {@link ServiceUpdateProviderContract#PROJECTION_SERVICE_UPDATE}
	 */
	@NonNull
	public Object[] getCursorRow() {
		return new Object[]{
				id,
				targetUUID,
				targetTripId,
				lastUpdateInMs,
				maxValidityInMs,
				severity,
				text,
				textHTML,
				language,
				originalId,
				sourceLabel,
				sourceId
		};
	}

	@NonNull
	public ContentValues toContentValues() {
		ContentValues contentValues = new ContentValues();
		if (this.id != null) {
			contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ID, this.id);
		} // ELSE AUTO INCREMENT
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID, this.targetUUID);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID, this.targetTripId);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, this.maxValidityInMs);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SEVERITY, this.severity);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT, this.text);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT_HTML, this.textHTML);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE, this.language);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ORIGINAL_ID, this.originalId);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL, this.sourceLabel);
		contentValues.put(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID, this.sourceId);
		return contentValues;
	}

	public static class HigherSeverityFirstComparator implements Comparator<ServiceUpdate> {

		@Override
		public int compare(ServiceUpdate lhs, ServiceUpdate rhs) {
			if (lhs == null && rhs == null) {
				return ComparatorUtils.SAME;
			} else if (lhs == null) {
				return ComparatorUtils.AFTER;
			} else if (rhs == null) {
				return ComparatorUtils.BEFORE;
			}
			return rhs.severity - lhs.severity; // higher severity before
		}
	}
}
