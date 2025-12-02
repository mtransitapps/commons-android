package org.mtransit.android.commons.provider.common;

import android.content.ContentValues;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContentProviderCompat;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.util.Arrays;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTSearchRecentSuggestionsProvider extends SearchRecentSuggestionsProvider implements MTLog.Loggable {

	@Override
	protected void setupSuggestions(@NonNull String authority, int mode) {
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

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "query(%s, %s, %s, %s, %s)", uri, Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		}
		return super.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "getType(%s)", uri);
		}
		return super.getType(uri);
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "delete(%s,%s,%s)", uri, selection, Arrays.toString(selectionArgs));
		}
		return super.delete(uri, selection, selectionArgs);
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "update(%s,%s,%s,%s)", uri, values, selection, Arrays.toString(selectionArgs));
		}
		return super.update(uri, values, selection, selectionArgs);
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "insert(%s,%s)", uri, values);
		}
		return super.insert(uri, values);
	}

	@NonNull
	public Context requireContextCompat() {
		return ContentProviderCompat.requireContext(this);
	}
}
