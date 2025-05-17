package org.mtransit.android.commons.provider;

import android.content.Context;
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
import org.mtransit.android.commons.provider.agency.AgencyUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class GTFSScheduleTimestampsProvider implements MTLog.Loggable {

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
		final RouteTripStop rts = filter.getRouteTripStop();
		final long startsAtInMs = filter.getStartsAtInMs();
		final long endsAtInMs = filter.getEndsAtInMs();
		final Context context = provider.requireContextCompat();
		final ThreadSafeDateFormatter dateFormat = GTFSStatusProvider.getDateFormat(context);
		final ThreadSafeDateFormatter timeFormat = GTFSStatusProvider.getTimeFormat(context);
		final TimeZone timeZone = TimeZone.getTimeZone(AgencyUtils.getRtsAgencyTimeZone(context));
		final Calendar startsAt = TimeUtils.getNewCalendar(timeZone, startsAtInMs);
		startsAt.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Timestamp> dayTimestamps;
		String lookupDayTime;
		String lookupDayDate;
		int dataRequests = 0;
		final Integer lastServiceDate = GTFSStatusProvider.findLastServiceDate(provider);
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_DEPARTURE_IN_SEC(context));
		while (startsAt.getTimeInMillis() <= endsAtInMs) {
			final Calendar lookupStartAt = TimeUtils.getNewCalendar(timeZone, startsAt.getTimeInMillis());
			if (lastServiceDate != null) {
				try {
					while (Integer.parseInt(dateFormat.formatThreadSafe(lookupStartAt)) > lastServiceDate) {
						lookupStartAt.add(Calendar.DATE, -7); // look 1 week behind
					}
				} catch (Exception e) {
					MTLog.w(LOG_TAG, e, "Error while parsing date!");
				}
			}
			while (lookupStartAt.getTimeInMillis() > lastDepartureInMs) { // WHILE lookup time is after last departure DO
				lookupStartAt.add(Calendar.DATE, -7); // look 1 week behind
			}
			lookupDayDate = dateFormat.formatThreadSafe(lookupStartAt);
			lookupDayTime = timeFormat.formatThreadSafe(lookupStartAt);
			if (dataRequests == 0) { // IF yesterday DO override computed date & time with GTFS format for 24+
				lookupDayTime = String.valueOf(Integer.parseInt(lookupDayTime) + GTFSStatusProvider.TWENTY_FOUR_HOURS);
			} else { // ELSE IF tomorrow or later DO
				lookupDayTime = GTFSStatusProvider.MIDNIGHT;
			}
			dayTimestamps = GTFSStatusProvider.findScheduleList(
					provider,
					rts.getRoute().getId(),
					rts.getTrip().getId(),
					rts.getStop().getId(),
					lookupDayDate,
					lookupDayTime,
					startsAt.getTimeInMillis() - lookupStartAt.getTimeInMillis()
			);
			if (startsAt.getTimeInMillis() > lookupStartAt.getTimeInMillis() // already looking at OLD schedule
					&& dayTimestamps.isEmpty()) {
				lookupStartAt.add(Calendar.DATE, -7); // look 1 week behind
				lookupDayDate = dateFormat.formatThreadSafe(lookupStartAt); // try 1 week before once
				dayTimestamps = GTFSStatusProvider.findScheduleList(
						provider,
						rts.getRoute().getId(),
						rts.getTrip().getId(),
						rts.getStop().getId(),
						lookupDayDate,
						lookupDayTime,
						startsAt.getTimeInMillis() - lookupStartAt.getTimeInMillis()
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
		scheduleTimestamps.setSourceLabel(GTFSProvider.getSOURCE_LABEL(provider.requireContextCompat()));
		scheduleTimestamps.setTimestampsAndSort(allTimestamps);
		return scheduleTimestamps;
	}

	@Nullable
	public static Cursor queryS(@NonNull GTFSProvider provider, @NonNull Uri uri, @Nullable String selection) {
		return ScheduleTimestampsProvider.queryS(provider, uri, selection);
	}
}
