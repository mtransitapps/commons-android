package org.mtransit.android.commons.provider;

import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.MTLog;

public interface ProviderContract extends MTLog.Loggable {

	String PING_PATH = "ping";

	@NonNull
	UriMatcher getURI_MATCHER();

	void ping();

	@NonNull
	SQLiteDatabase getReadDB();

	@NonNull
	SQLiteDatabase getWriteDB();
}
