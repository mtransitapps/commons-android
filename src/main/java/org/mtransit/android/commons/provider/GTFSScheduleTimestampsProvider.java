package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleTimestamps;

import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

	@NonNull
	static ScheduleTimestamps getScheduleTimestamps(GTFSProvider provider, ScheduleTimestampsProviderContract.Filter filter) {
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
		while (startsAt.getTimeInMillis() <= endsAtInMs) {
			Date timeDate = startsAt.getTime();
			//noinspection ConstantConditions // TODO requireContext()
			dayDate = GTFSStatusProvider.getDateFormat(provider.getContext()).formatThreadSafe(timeDate);
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				dayTime = String.valueOf(Integer.parseInt(GTFSStatusProvider.getTimeFormat(provider.getContext()).formatThreadSafe(timeDate))
						+ GTFSStatusProvider.TWENTY_FOUR_HOURS);
			} else { // ELSE tomorrow or later DO start at midnight
				dayTime = GTFSStatusProvider.MIDNIGHT;
			}
			dayTimestamps = GTFSStatusProvider.findScheduleList(provider, rts.getRoute().getId(), rts.getTrip().getId(), rts.getStop().getId(), dayDate,
					dayTime);
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
