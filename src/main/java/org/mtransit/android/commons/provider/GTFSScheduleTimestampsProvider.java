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
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GTFSScheduleTimestampsProvider implements MTLog.Loggable {

	private static final String LOG_TAG = GTFSScheduleTimestampsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		ScheduleTimestampsProvider.append(uriMatcher, authority);
	}

	@NonNull
	static ScheduleTimestamps getScheduleTimestamps(@NonNull GTFSProvider provider, @NonNull ScheduleTimestampsProviderContract.Filter filter) {
		ArrayList<Schedule.Timestamp> allTimestamps = new ArrayList<>();
		RouteTripStop rts = filter.getRouteTripStop();
		long startsAtInMs = filter.getStartsAtInMs();
		long endsAtInMs = filter.getEndsAtInMs();
		//noinspection ConstantConditions // TODO requireContext()
		final ThreadSafeDateFormatter dateFormat = GTFSStatusProvider.getDateFormat(provider.getContext());
		final ThreadSafeDateFormatter timeFormat = GTFSStatusProvider.getTimeFormat(provider.getContext());
		final TimeZone timeZone = TimeZone.getTimeZone(GTFSStatusProvider.getTIME_ZONE(provider.getContext()));
		Calendar startsAt = TimeUtils.getNewCalendar(timeZone, startsAtInMs);
		startsAt.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Timestamp> dayTimestamps;
		String dayTime;
		String dayDate;
		int dataRequests = 0;
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_DEPARTURE_IN_SEC(provider.getContext()));
		while (startsAt.getTimeInMillis() <= endsAtInMs) {
			long timeInMs = startsAt.getTimeInMillis();
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				timeInMs += GTFSStatusProvider.TWENTY_FOUR_HOURS_IN_MS;
			} else { // ELSE tomorrow or later DO start at midnight
				// DO NOTHING
			}
			long diffWithRealityInMs = 0L;
			while (timeInMs - diffWithRealityInMs > lastDepartureInMs) {
				diffWithRealityInMs += GTFSStatusProvider.ONE_WEEK_IN_MS;
			}
			timeInMs -= diffWithRealityInMs;
			final Date timeDate = new Date(timeInMs);
			dayDate = dateFormat.formatThreadSafe(timeDate);
			dayTime = timeFormat.formatThreadSafe(timeDate);
			if (dataRequests == 0) { // IF yesterday DO override computed date & time with GTFS format for 24+
				dayDate = dateFormat.formatThreadSafe(startsAt.getTimeInMillis() - diffWithRealityInMs);
				dayTime = String.valueOf(Integer.parseInt(dayTime) + GTFSStatusProvider.TWENTY_FOUR_HOURS);
			} else { // ELSE IF tomorrow or later DO
				dayTime = GTFSStatusProvider.MIDNIGHT;
			}
			dayTimestamps = GTFSStatusProvider.findScheduleList(
					provider,
					rts.getRoute().getId(),
					rts.getTrip().getId(),
					rts.getStop().getId(),
					dayDate,
					dayTime,
					diffWithRealityInMs
			);
			if (diffWithRealityInMs > 0L // already looking at OLD schedule
					&& dayTimestamps.isEmpty()) {
				dayDate = dateFormat.formatThreadSafe(new Date(timeInMs - GTFSStatusProvider.ONE_WEEK_IN_MS)); // try 1 week before once
				dayTimestamps = GTFSStatusProvider.findScheduleList(
						provider,
						rts.getRoute().getId(),
						rts.getTrip().getId(),
						rts.getStop().getId(),
						dayDate,
						dayTime,
						diffWithRealityInMs + GTFSStatusProvider.ONE_WEEK_IN_MS
				);
			}
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
	public static Cursor queryS(@NonNull GTFSProvider provider, @NonNull Uri uri, @Nullable String selection) {
		return ScheduleTimestampsProvider.queryS(provider, uri, selection);
	}
}
