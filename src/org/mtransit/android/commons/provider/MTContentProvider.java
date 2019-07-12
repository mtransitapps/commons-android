package org.mtransit.android.commons.provider;

import java.util.Arrays;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTContentProvider extends ContentProvider implements MTLog.Loggable {

	@Override
	public boolean onCreate() {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "onCreate()");
		}
		return onCreateMT();
	}

	/**
	 * @see ContentProvider#onCreate()
	 */
	public abstract boolean onCreateMT();

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "query(%s, %s, %s, %s, %s)", uri, Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		}
		return queryMT(uri, projection, selection, selectionArgs, sortOrder);
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
			MTLog.v(this, "getType(%s)", uri);
		}
		return getTypeMT(uri);
	}

	/**
	 * @see ContentProvider#getType(Uri)
	 */
	@Nullable
	public abstract String getTypeMT(@NonNull Uri uri);

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "delete(%s,%s,%s)", uri, selection, Arrays.toString(selectionArgs));
		}
		return deleteMT(uri, selection, selectionArgs);
	}

	/**
	 * @see ContentProvider#delete(Uri, String, String[])
	 */
	public abstract int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs);

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "update(%s,%s,%s,%s)", uri, values, selection, Arrays.toString(selectionArgs));
		}
		return updateMT(uri, values, selection, selectionArgs);
	}

	/**
	 * @see ContentProvider#update(Uri, ContentValues, String, String[])
	 */
	public abstract int updateMT(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs);

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "insert(%s,%s)", uri, values);
		}
		return insertMT(uri, values);
	}

	/**
	 * @see ContentProvider#insert(Uri, ContentValues)
	 */
	@Nullable
	public abstract Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values);

	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "bulkInsert(%s,%s)", uri, values);
		}
		return super.bulkInsert(uri, values);
	}
}
