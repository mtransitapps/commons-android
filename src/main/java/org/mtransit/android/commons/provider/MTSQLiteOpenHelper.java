package org.mtransit.android.commons.provider;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTSQLiteOpenHelper extends SQLiteOpenHelper implements MTLog.Loggable {

	public MTSQLiteOpenHelper(@Nullable Context context, @Nullable String name, @Nullable CursorFactory factory, int version) {
		super(context, name, factory, version);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "%s(%s, %s)", getLogTag(), name, version);
		}
	}

	public MTSQLiteOpenHelper(@Nullable Context context, @Nullable String name, @Nullable CursorFactory factory, int version, @Nullable DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "%s(%s, %s)", getLogTag(), name, version);
		}
	}

	@Override
	public void onCreate(@NonNull SQLiteDatabase db) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "onCreate()");
		}
		onCreateMT(db);
	}

	/**
	 * @see SQLiteOpenHelper#onCreate(SQLiteDatabase)
	 */
	public abstract void onCreateMT(@NonNull SQLiteDatabase db);

	@Override
	public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "onUpgrade(%s, %s)", oldVersion, newVersion);
		}
		onUpgradeMT(db, oldVersion, newVersion);
	}

	/**
	 * @see SQLiteOpenHelper#onUpgrade(SQLiteDatabase, int, int)
	 */
	public abstract void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

	@Override
	public void onOpen(@NonNull SQLiteDatabase db) {
		if (Constants.LOG_PROVIDER_LIFECYCLE) {
			MTLog.v(this, "onOpen()");
		}
		super.onOpen(db);
	}
}
