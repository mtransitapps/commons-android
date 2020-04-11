package org.mtransit.android.commons.provider;

import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleTimestamps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class GTFSScheduleTimestampsProvider implements MTLog.Loggable {

	private static final String LOG_TAG = GTFSScheduleTimestampsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		ScheduleTimestampsProvider.append(uriMatcher, authority);
	}

	private static final long ONE_WEEK_IN_MS = TimeUnit.DAYS.toMillis(7L);

	@NonNull
	static ScheduleTimestamps getScheduleTimestamps(@NonNull GTFSProvider provider, @NonNull ScheduleTimestampsProviderContract.Filter filter) {
		ArrayList<Schedule.Timestamp> allTimestamps = new ArrayList<>();
		RouteTripStop rts = filter.getRouteTripStop();
		long startsAtInMs = filter.getStartsAtInMs();
		long endsAtInMs = filter.getEndsAtInMs();
		Calendar startsAt = TimeUtils.getNewCalendar(startsAtInMs);
		startsAt.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Timestamp> dayTimestamps;
		String dayTime;
		String dayDate;
		int dataRequests = 0;
		//noinspection ConstantConditions // TODO requireContext()
		final ThreadSafeDateFormatter dateFormat = GTFSStatusProvider.getDateFormat(provider.getContext());
		final ThreadSafeDateFormatter timeFormat = GTFSStatusProvider.getTimeFormat(provider.getContext());
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_LAST_DEPARTURE_IN_SEC(provider.getContext()));
		while (startsAt.getTimeInMillis() <= endsAtInMs) {
			long timeInMs = startsAt.getTimeInMillis();
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				timeInMs += TimeUnit.HOURS.toMillis(24L);
			} else { // ELSE tomorrow or later DO start at midnight
				Calendar midnight = TimeUtils.getNewCalendar(timeInMs);
				midnight.set(Calendar.HOUR_OF_DAY, 0);
				midnight.set(Calendar.MINUTE, 0);
				midnight.set(Calendar.SECOND, 0);
				midnight.set(Calendar.MILLISECOND, 0);
				timeInMs = midnight.getTimeInMillis();
			}
			long diffWithRealityInMs = 0L;
			while (timeInMs - diffWithRealityInMs > lastDepartureInMs) {
				diffWithRealityInMs += ONE_WEEK_IN_MS;
			}
			timeInMs -= diffWithRealityInMs;
			final Date timeDate = new Date(timeInMs);
			dayDate = dateFormat.formatThreadSafe(timeDate);
			dayTime = timeFormat.formatThreadSafe(timeDate);
			dayTimestamps = GTFSStatusProvider.findScheduleList(provider,
					rts.getRoute().getId(), rts.getTrip().getId(), rts.getStop().getId(),
					dayDate, dayTime,
					diffWithRealityInMs);
			dataRequests++; // 1 more data request done
			for (Schedule.Timestamp t : dayTimestamps) {
				if (t.t >= startsAtInMs && t.t < endsAtInMs) {
					allTimestamps.add(t);
				}
			}
			startsAt.add(Calendar.DATE, +1); // NEXT DAY
		}
		ScheduleTimestamps scheduleTimestamps = new ScheduleTimestamps(rts.getUUID(), startsAtInMs, endsAtInMs);
		scheduleTimestamps.setTimestampsAndSort(allTimestamps);
		return scheduleTimestamps;
	}

	@Nullable
	public static Cursor queryS(GTFSProvider provider, Uri uri, String selection) {
		return ScheduleTimestampsProvider.queryS(provider, uri, selection);
	}
}
