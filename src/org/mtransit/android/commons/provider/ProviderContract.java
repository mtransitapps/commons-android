package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.MTLog;

import android.content.UriMatcher;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

public interface ProviderContract extends MTLog.Loggable {

	String PING_PATH = "ping";

	@NonNull
	UriMatcher getURI_MATCHER();

	void ping();

	@NonNull
	SQLiteOpenHelper getDBHelper();
}
