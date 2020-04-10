package org.mtransit.android.commons.provider;

import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.ScheduleTimestamps;

public abstract class ScheduleTimestampsProvider extends MTContentProvider implements ScheduleTimestampsProviderContract {

	private static final String LOG_TAG = StatusProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, ScheduleTimestampsProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, ScheduleTimestampsProviderContract.SCHEDULE_TIMESTAMPS_PATH, ContentProviderConstants.SCHEDULE_TIMESTAMPS);
	}

	@Nullable
	public static Cursor queryS(ScheduleTimestampsProviderContract provider, Uri uri, String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
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
		ScheduleTimestampsProviderContract.Filter scheduleTimestampsFilter = ScheduleTimestampsProviderContract.Filter.fromJSONString(selection);
		if (scheduleTimestampsFilter == null) {
			MTLog.w(LOG_TAG, "Error while parsing schedule timestamps filter '%s'!", selection);
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
}
