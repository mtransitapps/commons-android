package org.mtransit.android.commons.provider;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkManager;

import com.google.android.gms.security.ProviderInstaller;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.Area;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.commons.FeatureFlags;

public abstract class AgencyProvider extends MTContentProvider implements AgencyProviderContract, ProviderInstaller.ProviderInstallListener {

	@MainThread
	@CallSuper
	@Override
	public boolean onCreateMT() {
		updateSecurityProviderIfNeeded(getContext()); // cannot use requireContext() in onCreate()
		return true;
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, AgencyProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, AgencyProviderContract.VERSION_PATH, ContentProviderConstants.VERSION);
		uriMatcher.addURI(authority, AgencyProviderContract.DEPLOYED_PATH, ContentProviderConstants.DEPLOYED);
		uriMatcher.addURI(authority, AgencyProviderContract.LABEL_PATH, ContentProviderConstants.LABEL);
		uriMatcher.addURI(authority, AgencyProviderContract.COLOR_PATH, ContentProviderConstants.COLOR);
		uriMatcher.addURI(authority, AgencyProviderContract.SHORT_NAME_PATH, ContentProviderConstants.SHORT_NAME);
		uriMatcher.addURI(authority, AgencyProviderContract.SETUP_REQUIRED_PATH, ContentProviderConstants.SETUP_REQUIRED);
		uriMatcher.addURI(authority, AgencyProviderContract.AREA_PATH, ContentProviderConstants.AREA);
		uriMatcher.addURI(authority, AgencyProviderContract.MAX_VALID_SEC, ContentProviderConstants.MAX_VALID_SEC);
		uriMatcher.addURI(authority, AgencyProviderContract.AVAILABLE_VERSION_CODE, ContentProviderConstants.AVAILABLE_VERSION_CODE);
		uriMatcher.addURI(authority, AgencyProviderContract.CONTACT_US, ContentProviderConstants.CONTACT_US);
		uriMatcher.addURI(authority, AgencyProviderContract.ALL_PATH, ContentProviderConstants.ALL);
		uriMatcher.addURI(authority, AgencyProviderContract.EXTENDED_TYPE_ID, ContentProviderConstants.EXTENDED_TYPE_ID);
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
			shouldTriggerDeploySync(selection);
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.VERSION:
			return getVersion();
		case ContentProviderConstants.LABEL:
			return getLabel();
		case ContentProviderConstants.COLOR:
			return getColor();
		case ContentProviderConstants.SHORT_NAME:
			return getShortName();
		case ContentProviderConstants.DEPLOYED:
			return isDeployed();
		case ContentProviderConstants.SETUP_REQUIRED:
			return isSetupRequired();
		case ContentProviderConstants.AREA:
			return getArea();
		case ContentProviderConstants.MAX_VALID_SEC:
			return getMaxValidSec();
		case ContentProviderConstants.AVAILABLE_VERSION_CODE:
			return getAvailableVersionCode(selection);
		case ContentProviderConstants.CONTACT_US:
			return getContactUs();
		case ContentProviderConstants.EXTENDED_TYPE_ID:
			return getExtendedTypeId();
		case ContentProviderConstants.ALL:
			return getAll();
		default:
			return null; // not processed
		}
	}

	private void shouldTriggerDeploySync(@Nullable String selection) {
		if (FeatureFlags.F_PROVIDER_DEPLOY_SYNC_GTFS_ONLY) {
			if (!(this instanceof GTFSProvider)) {
				return; // SKIP, not GTFS DB
			}
		}
		if (!FeatureFlags.F_WORK_MANAGER_DB_DEPLOY) {
			deploySync();
			return;
		}
		final boolean setupRequired = isAgencySetupRequired();
		if (!setupRequired) {
			return;
		}
		final WorkManager workManager = WorkManager.getInstance(requireContextCompat());
		workManager.cancelAllWorkByTag(AgencyProviderDeployWorker.WORK_MANAGER_TAG);
		if (!AgencyProviderDeployWorker.FROM_WORKER.equals(selection)) {
			workManager.enqueue(AgencyProviderDeployWorker.makeWorkRequest());
			return;
		}
		deploySync();
	}

	public void deploySync() {
		// DO NOTHING
	}

	@Nullable
	public String getSortOrder(@NonNull Uri uri) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.DEPLOYED:
		case ContentProviderConstants.LABEL:
		case ContentProviderConstants.COLOR:
		case ContentProviderConstants.SHORT_NAME:
		case ContentProviderConstants.VERSION:
		case ContentProviderConstants.SETUP_REQUIRED:
		case ContentProviderConstants.AREA:
		case ContentProviderConstants.MAX_VALID_SEC:
		case ContentProviderConstants.AVAILABLE_VERSION_CODE:
		case ContentProviderConstants.CONTACT_US:
		case ContentProviderConstants.EXTENDED_TYPE_ID:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
		}
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.DEPLOYED:
		case ContentProviderConstants.LABEL:
		case ContentProviderConstants.COLOR:
		case ContentProviderConstants.SHORT_NAME:
		case ContentProviderConstants.VERSION:
		case ContentProviderConstants.SETUP_REQUIRED:
		case ContentProviderConstants.AREA:
		case ContentProviderConstants.MAX_VALID_SEC:
		case ContentProviderConstants.AVAILABLE_VERSION_CODE:
		case ContentProviderConstants.CONTACT_US:
		case ContentProviderConstants.EXTENDED_TYPE_ID:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	@NonNull
	private Cursor getAll() {
		final Area area = getAgencyArea(requireContextCompat());
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{
				VERSION_PATH,
				LABEL_PATH,
				COLOR_PATH,
				SHORT_NAME_PATH,
				DEPLOYED_PATH,
				SETUP_REQUIRED_PATH,
				AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LNG, AREA_MAX_LNG,
				MAX_VALID_SEC,
				AVAILABLE_VERSION_CODE,
				CONTACT_US_WEB, CONTACT_US_WEB_FR,
				EXTENDED_TYPE_ID,
		});
		matrixCursor.addRow(new Object[]{
				getAgencyVersion(),
				getAgencyLabel(),
				getAgencyColor(),
				getAgencyShortName(),
				isAgencyDeployedInt(),
				isAgencySetupRequired(),
				area.getMinLat(), area.getMaxLat(), area.getMinLng(), area.getMaxLng(),
				getAgencyMaxValidSec(requireContextCompat()),
				getAvailableVersionCode(requireContextCompat(), null),
				getContactUsWeb(requireContextCompat()), getContactUsWebFr(requireContextCompat()),
				getExtendedTypeId(requireContextCompat()),
		});
		return matrixCursor;
	}

	@NonNull
	public abstract UriMatcher getAgencyUriMatcher();

	private Cursor getVersion() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{VERSION_PATH});
		matrixCursor.addRow(new Object[]{getAgencyVersion()});
		return matrixCursor;
	}

	public abstract int getAgencyVersion();

	@NonNull
	private Cursor getLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{LABEL_PATH});
		matrixCursor.addRow(new Object[]{getAgencyLabel()});
		return matrixCursor;
	}

	@NonNull
	private String getAgencyLabel() {
		return requireContextCompat().getString(getAgencyLabelResId());
	}

	public abstract int getAgencyLabelResId();

	@NonNull
	public Cursor getColor() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{COLOR_PATH});
		matrixCursor.addRow(new Object[]{getAgencyColor()});
		return matrixCursor;
	}

	private String getAgencyColor() {
		return getAgencyColorString(requireContextCompat());
	}

	@Nullable
	public abstract String getAgencyColorString(@NonNull Context context);

	private Cursor getShortName() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{SHORT_NAME_PATH});
		matrixCursor.addRow(new Object[]{getAgencyShortName()});
		return matrixCursor;
	}

	private String getAgencyShortName() {
		return requireContextCompat().getString(getAgencyShortNameResId());
	}

	public abstract int getAgencyShortNameResId();

	private Cursor isDeployed() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{DEPLOYED_PATH});
		matrixCursor.addRow(new Object[]{isAgencyDeployedInt()});
		return matrixCursor;
	}

	private int isAgencyDeployedInt() {
		return isAgencyDeployed() ? 1 : 0;
	}

	public abstract boolean isAgencyDeployed();

	private Cursor isSetupRequired() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{SETUP_REQUIRED_PATH});
		matrixCursor.addRow(new Object[]{isAgencySetupRequiredInt()});
		return matrixCursor;
	}

	private int isAgencySetupRequiredInt() {
		return isAgencySetupRequired() ? 1 : 0;
	}

	public abstract boolean isAgencySetupRequired();

	@NonNull
	private Cursor getArea() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LNG, AREA_MAX_LNG});
		Area area = getAgencyArea(requireContextCompat());
		matrixCursor.addRow(new Object[]{area.getMinLat(), area.getMaxLat(), area.getMinLng(), area.getMaxLng()});
		return matrixCursor;
	}

	@NonNull
	public abstract Area getAgencyArea(@NonNull Context context);

	@NonNull
	private Cursor getMaxValidSec() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{MAX_VALID_SEC});
		matrixCursor.addRow(new Object[]{getAgencyMaxValidSec(requireContextCompat())});
		return matrixCursor;
	}

	public abstract int getAgencyMaxValidSec(@NonNull Context context);

	@NonNull
	private Cursor getAvailableVersionCode(@Nullable String filterS) {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{AVAILABLE_VERSION_CODE});
		matrixCursor.addRow(new Object[]{getAvailableVersionCode(requireContextCompat(), filterS)});
		return matrixCursor;
	}

	public abstract int getAvailableVersionCode(@NonNull Context context, @Nullable String filterS);

	@NonNull
	private Cursor getContactUs() {
		final MatrixCursor matrixCursor = new MatrixCursor(new String[]{
				CONTACT_US_WEB,
				CONTACT_US_WEB_FR,
		});
		matrixCursor.addRow(new Object[]{
				getContactUsWeb(requireContextCompat()),
				getContactUsWebFr(requireContextCompat()),
		});
		return matrixCursor;
	}

	@NonNull
	public abstract String getContactUsWeb(@NonNull Context context);

	@NonNull
	public abstract String getContactUsWebFr(@NonNull Context context);

	@NonNull
	private Cursor getExtendedTypeId() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{EXTENDED_TYPE_ID});
		matrixCursor.addRow(new Object[]{getExtendedTypeId(requireContextCompat())});
		return matrixCursor;
	}

	@DataSourceTypeId.DataSourceType
	public abstract int getExtendedTypeId(@NonNull Context context);

	@MainThread
	private void updateSecurityProviderIfNeeded(@Nullable Context context) {
		try {
			if (context != null) {
				ProviderInstaller.installIfNeededAsync(context, this);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Unexpected error while updating security provider!");
		}
	}

	@Override
	public void onProviderInstalled() {
		MTLog.d(this, "Security provider is up-to-date.");
	}

	@Override
	public void onProviderInstallFailed(int i, @Nullable Intent intent) {
		MTLog.w(this, "Unexpected error while updating security provider (%s,%s)!", i, intent);
	}
}
