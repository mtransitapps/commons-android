package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.MTLog;

import android.content.UriMatcher;
import android.database.sqlite.SQLiteOpenHelper;

public interface ProviderContract extends MTLog.Loggable {

	public UriMatcher getURIMATCHER();

	public void ping();

	public SQLiteOpenHelper getDBHelper();

}
