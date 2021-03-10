package org.mtransit.android.commons.provider;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;

public abstract class AgencyProvider extends MTContentProvider implements AgencyProviderContract {

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
		uriMatcher.addURI(authority, AgencyProviderContract.ALL_PATH, ContentProviderConstants.ALL);
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
			ping();
			deploySync();
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
		case ContentProviderConstants.ALL:
			return getAll();
		default:
			return null; // not processed
		}
	}

	private void deploySync() {
		try {
			getReadDB(); // trigger create/update DB if necessary
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deploying DB!");
		}
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
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	@NonNull
	private Cursor getAll() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{ //
				VERSION_PATH, LABEL_PATH, COLOR_PATH, SHORT_NAME_PATH, DEPLOYED_PATH, SETUP_REQUIRED_PATH, //
				AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LNG, AREA_MAX_LNG});
		LocationUtils.Area area = getAgencyArea(getContext());
		matrixCursor.addRow(new Object[]{ //
				getAgencyVersion(), //
				getAgencyLabel(), //
				getAgencyColor(), //
				getAgencyShortName(), //
				isAgencyDeployedInt(), //
				isAgencySetupRequired(), //
				area.minLat, area.maxLat, area.minLng, area.maxLng});
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

	private Cursor getLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{LABEL_PATH});
		matrixCursor.addRow(new Object[]{getAgencyLabel()});
		return matrixCursor;
	}

	private String getAgencyLabel() {
		return getContext().getString(getAgencyLabelResId());
	}

	public abstract int getAgencyLabelResId();

	@NonNull
	public Cursor getColor() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{COLOR_PATH});
		matrixCursor.addRow(new Object[]{getAgencyColor()});
		return matrixCursor;
	}

	private String getAgencyColor() {
		return getAgencyColorString(getContext());
	}

	@Nullable
	public abstract String getAgencyColorString(@NonNull Context context);

	private Cursor getShortName() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{SHORT_NAME_PATH});
		matrixCursor.addRow(new Object[]{getAgencyShortName()});
		return matrixCursor;
	}

	private String getAgencyShortName() {
		return getContext().getString(getAgencyShortNameResId());
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
		LocationUtils.Area area = getAgencyArea(getContext());
		matrixCursor.addRow(new Object[]{area.minLat, area.maxLat, area.minLng, area.maxLng});
		return matrixCursor;
	}

	@NonNull
	public abstract LocationUtils.Area getAgencyArea(@NonNull Context context);
}
