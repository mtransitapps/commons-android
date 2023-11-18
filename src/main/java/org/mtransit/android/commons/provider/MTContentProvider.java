package org.mtransit.android.commons.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContentProviderCompat;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.commons.CommonsApp;

import java.util.Arrays;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTContentProvider extends ContentProvider implements MTLog.Loggable {

	@MainThread
	@Override
	public boolean onCreate() {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "onCreate()");
		}
		CommonsApp.setup(true);
		return onCreateMT();
	}

	/**
	 * @see ContentProvider#onCreate()
	 */
	@MainThread
	public abstract boolean onCreateMT();

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "query(%s, %s, %s, %s, %s) > ...", uri, Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		}
		final Cursor cursor = queryMT(uri, projection, selection, selectionArgs, sortOrder);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.d(this, "query(%s, %s, %s, %s, %s) > DONE", uri, Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		}
		return cursor;
	}

	/**
	 * @see ContentProvider#query(Uri, String[], String, String[], String)
	 */
	@Nullable
	public abstract Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder);

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "getType(%s) > ...", uri);
		}
		final String type = getTypeMT(uri);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.d(this, "getType(%s) > DONE", uri);
		}
		return type;
	}

	/**
	 * @see ContentProvider#getType(Uri)
	 */
	@Nullable
	public abstract String getTypeMT(@NonNull Uri uri);

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "delete(%s,%s,%s) > ...", uri, selection, Arrays.toString(selectionArgs));
		}
		final int deleted = deleteMT(uri, selection, selectionArgs);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.d(this, "delete(%s,%s,%s) > DONE", uri, selection, Arrays.toString(selectionArgs));
		}
		return deleted;
	}

	/**
	 * @see ContentProvider#delete(Uri, String, String[])
	 */
	public abstract int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs);

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "update(%s,%s,%s,%s) > ...", uri, values, selection, Arrays.toString(selectionArgs));
		}
		final int updated = updateMT(uri, values, selection, selectionArgs);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.d(this, "update(%s,%s,%s,%s) > DONE", uri, values, selection, Arrays.toString(selectionArgs));
		}
		return updated;
	}

	/**
	 * @see ContentProvider#update(Uri, ContentValues, String, String[])
	 */
	public abstract int updateMT(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs);

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "insert(%s,%s) > ...", uri, values);
		}
		final Uri inserted = insertMT(uri, values);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.d(this, "insert(%s,%s) > DONE", uri, values);
		}
		return inserted;
	}

	/**
	 * @see ContentProvider#insert(Uri, ContentValues)
	 */
	@Nullable
	public abstract Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values);

	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "bulkInsert(%s,%s) > ...", uri, values);
		}
		final int inserted = super.bulkInsert(uri, values);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.d(this, "bulkInsert(%s,%s) > DONE", uri, values);
		}
		return inserted;
	}

	@SuppressWarnings("WeakerAccess")
	@NonNull
	public Context requireContextCompat() {
		return ContentProviderCompat.requireContext(this);
	}
}
