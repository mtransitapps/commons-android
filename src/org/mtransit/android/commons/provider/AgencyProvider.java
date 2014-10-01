package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.LocationUtils.Area;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public abstract class AgencyProvider extends MTContentProvider implements AgencyProviderContract {

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "version", ContentProviderConstants.VERSION);
		uriMatcher.addURI(authority, "deployed", ContentProviderConstants.DEPLOYED);
		uriMatcher.addURI(authority, "label", ContentProviderConstants.LABEL);
		uriMatcher.addURI(authority, "setuprequired", ContentProviderConstants.SETUP_REQUIRED);
		uriMatcher.addURI(authority, "type", ContentProviderConstants.TYPE);
		uriMatcher.addURI(authority, "area", ContentProviderConstants.AREA);
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.VERSION:
			return getVersion();
		case ContentProviderConstants.LABEL:
			return getLabel();
		case ContentProviderConstants.DEPLOYED:
			return isDeployed();
		case ContentProviderConstants.SETUP_REQUIRED:
			return isSetupRequired();
		case ContentProviderConstants.TYPE:
			return getType();
		case ContentProviderConstants.AREA:
			return getArea();
		default:
			return null; // not processed
		}
	}

	public String getSortOrder(Uri uri) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.DEPLOYED:
		case ContentProviderConstants.LABEL:
		case ContentProviderConstants.VERSION:
		case ContentProviderConstants.SETUP_REQUIRED:
		case ContentProviderConstants.TYPE:
		case ContentProviderConstants.AREA:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
		}
	}

	@Override
	public String getTypeMT(Uri uri) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.DEPLOYED:
		case ContentProviderConstants.LABEL:
		case ContentProviderConstants.VERSION:
		case ContentProviderConstants.SETUP_REQUIRED:
		case ContentProviderConstants.TYPE:
		case ContentProviderConstants.AREA:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	public abstract UriMatcher getAgencyUriMatcher();

	private Cursor getVersion() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "version" });
		matrixCursor.addRow(new Object[] { getAgencyVersion() });
		return matrixCursor;
	}

	public abstract int getAgencyVersion();

	private Cursor getLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "label" });
		matrixCursor.addRow(new Object[] { getContext().getString(getAgencyLabelResId()) });
		return matrixCursor;
	}

	public abstract int getAgencyLabelResId();

	private Cursor isDeployed() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "deployed" });
		matrixCursor.addRow(new Object[] { isAgencyDeployed() ? 1 : 0 });
		return matrixCursor;
	}

	public abstract boolean isAgencyDeployed();

	private Cursor isSetupRequired() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "setuprequired" });
		matrixCursor.addRow(new Object[] { isAgencySetupRequired() ? 1 : 0 });
		return matrixCursor;
	}

	public abstract boolean isAgencySetupRequired();

	private Cursor getType() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "type" });
		matrixCursor.addRow(new Object[] { getAgencyType() });
		return matrixCursor;
	}

	public abstract int getAgencyType();

	private Cursor getArea() {
		final Area area = getAgencyArea(getContext());
		return Area.toCursor(area);
	}

	public abstract Area getAgencyArea(Context context);

}
