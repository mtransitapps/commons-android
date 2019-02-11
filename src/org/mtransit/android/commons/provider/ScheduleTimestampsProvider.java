package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.ScheduleTimestamps;

import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class ScheduleTimestampsProvider extends ContentProviderExtra implements ScheduleTimestampsProviderContract {

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
	public static Cursor queryS(@NonNull ScheduleTimestampsProviderContract provider, @NonNull Uri uri, @Nullable String selection) {
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

	@NonNull
	private static Cursor getScheduleTimestamps(@NonNull ScheduleTimestampsProviderContract provider, @Nullable String selection) {
		ScheduleTimestampsProviderContract.Filter scheduleTimestampsFilter = ScheduleTimestampsProviderContract.Filter.fromJSONString(selection);
		if (scheduleTimestampsFilter == null) {
			MTLog.w(LOG_TAG, "Error while parsing schedule timestamps filter '%s'!", selection);
			return getScheduleTimestampCursor(null);
		}
		ScheduleTimestamps scheduleTimestamps = provider.getScheduleTimestamps(scheduleTimestampsFilter);
		return getScheduleTimestampCursor(scheduleTimestamps);
	}

	@NonNull
	public static Cursor getScheduleTimestampCursor(@Nullable ScheduleTimestamps scheduleTimestamps) {
		if (scheduleTimestamps == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		return scheduleTimestamps.toCursor();
	}
}
