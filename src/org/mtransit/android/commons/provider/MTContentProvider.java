package org.mtransit.android.commons.provider;

import java.util.Arrays;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTContentProvider extends ContentProvider implements MTLog.Loggable {

	@Override
	public boolean onCreate() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate()");
		}
		return onCreateMT();
	}

	/**
	 * @see ContentProvider#onCreate()
	 */
	public abstract boolean onCreateMT();

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "query(%s, %s, %s, %s, %s)", uri, Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		}
		return queryMT(uri, projection, selection, selectionArgs, sortOrder);
	}

	/**
	 * @see ContentProvider#query(Uri, String[], String, String[], String)
	 */
	public abstract Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

	@Override
	public String getType(Uri uri) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "getType(%s)", uri);
		}
		return getTypeMT(uri);
	}

	/**
	 * @see ContentProvider#getType(Uri)
	 */
	public abstract String getTypeMT(Uri uri);

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "delete(%s,%s,%s)", uri, selection, Arrays.toString(selectionArgs));
		}
		return deleteMT(uri, selection, selectionArgs);
	}

	/**
	 * @see ContentProvider#delete(Uri, String, String[])
	 */
	public abstract int deleteMT(Uri uri, String selection, String[] selectionArgs);

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "update(%s,%s,%s,%s)", uri, values, selection, Arrays.toString(selectionArgs));
		}
		return updateMT(uri, values, selection, selectionArgs);
	}

	/**
	 * @see ContentProvider#update(Uri, ContentValues, String, String[])
	 */
	public abstract int updateMT(Uri uri, ContentValues values, String selection, String[] selectionArgs);

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "insert(%s,%s)", uri, values);
		}
		return insertMT(uri, values);
	}

	/**
	 * @see ContentProvider#insert(Uri, ContentValues)
	 */
	public abstract Uri insertMT(Uri uri, ContentValues values);

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "bulkInsert(%s,%s)", uri, values);
		}
		return super.bulkInsert(uri, values);
	}

}
