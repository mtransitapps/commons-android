package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ScheduleTimestampsFilter;

import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public abstract class ScheduleTimestampsProvider extends MTContentProvider implements ScheduleTimestampsProviderContract {

	private static final String TAG = StatusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String SCHEDULE_TIMESTAMPS_CONTENT_DIRECTORY = "schedule";

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "ping", ContentProviderConstants.PING);
		uriMatcher.addURI(authority, SCHEDULE_TIMESTAMPS_CONTENT_DIRECTORY, ContentProviderConstants.SCHEDULE_TIMESTAMPS);
	}

	public static Cursor queryS(ScheduleTimestampsProviderContract provider, Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.SCHEDULE_TIMESTAMPS:
			return getScheduleTimestamps(provider, selection);
		default:
			return null; // not processed
		}
	}

	private static Cursor getScheduleTimestamps(ScheduleTimestampsProviderContract provider, String selection) {
		ScheduleTimestampsFilter scheduleTimestampsFilter = ScheduleTimestampsFilter.fromJSONString(selection);
		if (scheduleTimestampsFilter == null) {
			MTLog.w(TAG, "Error while parsing schedule timestamps filter '%s'!", selection);
			return getScheduleTimestampCursor(null);
		}
		ScheduleTimestamps scheduleTimestamps = provider.getScheduleTimestamps(scheduleTimestampsFilter);
		return getScheduleTimestampCursor(scheduleTimestamps);
	}

	public static Cursor getScheduleTimestampCursor(ScheduleTimestamps scheduleTimestamps) {
		if (scheduleTimestamps == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		return scheduleTimestamps.toCursor();
	}

	public static class ScheduleTimeStampsColumns {
		public static final String T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID = "target";
		public static final String T_SCHEDULE_TIMESTAMPS_K_EXTRAS = "extras";
		public static final String T_SCHEDULE_TIMESTAMPS_K_STARTS_AT = "startsAt";
		public static final String T_SCHEDULE_TIMESTAMPS_K_ENDS_AT = "endsAt";
	}
}
