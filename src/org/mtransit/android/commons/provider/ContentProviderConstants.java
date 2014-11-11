package org.mtransit.android.commons.provider;

import android.database.Cursor;
import android.database.MatrixCursor;

public final class ContentProviderConstants {

	public static final Cursor EMPTY_CURSOR = new MatrixCursor(new String[] {});

	public static final String SEARCH_SPLIT_ON = "[\\s\\W]";

	// shared URI Matcher constants (> 100) here
	public static final int PING = 100;
	public static final int VERSION = 101;
	public static final int DEPLOYED = 102;
	public static final int LABEL = 103;
	public static final int SHORT_NAME = 109;
	public static final int SETUP_REQUIRED = 104;
	//
	public static final int TYPE = 105;
	public static final int AREA = 106;
	//
	public static final int POI = 107;
	//
	public static final int STATUS = 108;
	//
	public static final int SCHEDULE_TIMESTAMPS = 110;
	//
	public static final int SEARCH_SUGGEST_EMPTY = 111;
	public static final int SEARCH_SUGGEST_QUERY = 112;

}
