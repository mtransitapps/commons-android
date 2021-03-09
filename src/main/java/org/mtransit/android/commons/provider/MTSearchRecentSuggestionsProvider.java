package org.mtransit.android.commons.provider;

import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.util.Arrays;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTSearchRecentSuggestionsProvider extends SearchRecentSuggestionsProvider implements MTLog.Loggable {

	@Override
	protected void setupSuggestions(String authority, int mode) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "setupSuggestions(%s,%s)", authority, mode);
		}
		super.setupSuggestions(authority, mode);
	}

	// INHERITED FROM CONTENTPROVIDER

	@Override
	public boolean onCreate() {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "onCreate()");
		}
		return super.onCreate();
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "query(%s, %s, %s, %s, %s)", uri, Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		}
		return super.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public String getType(Uri uri) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "getType(%s)", uri);
		}
		return super.getType(uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "delete(%s,%s,%s)", uri, selection, Arrays.toString(selectionArgs));
		}
		return super.delete(uri, selection, selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "update(%s,%s,%s,%s)", uri, values, selection, Arrays.toString(selectionArgs));
		}
		return super.update(uri, values, selection, selectionArgs);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "insert(%s,%s)", uri, values);
		}
		return super.insert(uri, values);
	}
}
