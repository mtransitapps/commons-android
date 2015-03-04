package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.MTLog;

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteOpenHelper;

public interface ProviderContract extends MTLog.Loggable {

	public static final String PING_PATH = "ping";

	public UriMatcher getURI_MATCHER();

	public void ping();

	public SQLiteOpenHelper getDBHelper();

	public Context getContentProviderContext();

}
