package org.mtransit.android.commons.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class GTFSStatusProvider implements MTLog.Loggable {

	private static final String TAG = GTFSStatusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void append(@NonNull UriMatcher uriMatcher, String authority) {
		StatusProvider.append(uriMatcher, authority);
	}

	@Nullable
	private static String timeZone = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	@NonNull
	public static String getTIME_ZONE(@NonNull Context context) {
		if (timeZone == null) {
			timeZone = context.getResources().getString(R.string.gtfs_rts_timezone);
		}
		return timeZone;
	}

	@Nullable
	private static Boolean scheduleAvailable = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	public static boolean isSCHEDULE_AVAILABLE(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (scheduleAvailable == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					scheduleAvailable = context.getResources().getBoolean(R.bool.next_gtfs_rts_schedule_available);
				} else { // CURRENT = default
					scheduleAvailable = context.getResources().getBoolean(R.bool.current_gtfs_rts_schedule_available);
				}
			} else {
				scheduleAvailable = context.getResources().getBoolean(R.bool.gtfs_rts_schedule_available);
			}
		}
		return scheduleAvailable;
	}

	@Nullable
	private static Boolean frequencyAvailable = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public static boolean isFREQUENCY_AVAILABLE(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (frequencyAvailable == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					frequencyAvailable = context.getResources().getBoolean(R.bool.next_gtfs_rts_frequency_available);
				} else { // CURRENT = default
					frequencyAvailable = context.getResources().getBoolean(R.bool.current_gtfs_rts_frequency_available);
				}
			} else {
				frequencyAvailable = context.getResources().getBoolean(R.bool.gtfs_rts_frequency_available);
			}
		}
		return frequencyAvailable;
	}

	public static void onCurrentNextDataChange() {
		scheduleAvailable = null;
		stopScheduleRawFileFormat = null;
		frequencyAvailable = null;
		routeFrequencyRawFileFormat = null;
	}

	public static final long STATUS_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);

	public static final long STATUS_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(6L);

	public static final long STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(1L);

	public static final long STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.HOURS.toMillis(1L);

	public static final long STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(30L);

	public static long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return STATUS_VALIDITY_IN_MS;
	}

	public static long getStatusMaxValidityInMs() {
		return STATUS_MAX_VALIDITY_IN_MS;
	}

	public static long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	private static final long PROVIDER_READ_FROM_SOURCE_AT_IN_MS = 0; // it doesn't get older than that

	@Nullable
	public static POIStatus getNewStatus(@NonNull GTFSProvider provider, StatusProviderContract.Filter statusFilter) {
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(TAG, "Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		Schedule schedule = new Schedule(statusFilter.getTargetUUID(), scheduleStatusFilter.getTimestampOrDefault(), getStatusMaxValidityInMs(),
				PROVIDER_READ_FROM_SOURCE_AT_IN_MS, PROVIDER_PRECISION_IN_MS, scheduleStatusFilter.getRouteTripStop().isDescentOnly());
		if (isSCHEDULE_AVAILABLE(provider.getContext())) {
			schedule.setTimestampsAndSort(findTimestamps(provider, scheduleStatusFilter));
		}
		if (isFREQUENCY_AVAILABLE(provider.getContext())) {
			schedule.setFrequenciesAndSort(findFrequencies(provider, scheduleStatusFilter));
		}
		return schedule;
	}

	private static final String DATE_FORMAT_PATTERN = "yyyyMMdd";

	@Nullable
	private static ThreadSafeDateFormatter dateFormat;

	@NonNull
	public static ThreadSafeDateFormatter getDateFormat(@NonNull Context context) {
		if (dateFormat == null) {
			dateFormat = new ThreadSafeDateFormatter(DATE_FORMAT_PATTERN);
			dateFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return dateFormat;
	}

	private static final String TIME_FORMAT_PATTERN = "HHmmss";
	@Nullable
	private static ThreadSafeDateFormatter timeFormat;

	@NonNull
	public static ThreadSafeDateFormatter getTimeFormat(@NonNull Context context) {
		if (timeFormat == null) {
			timeFormat = new ThreadSafeDateFormatter(TIME_FORMAT_PATTERN);
			timeFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return timeFormat;
	}

	public static final int TWENTY_FOUR_HOURS = 240000;

	public static final String MIDNIGHT = "000000";

	private static final String ROUTE_FREQUENCY_RAW_FILE_TYPE = "raw";

	@Nullable
	private static String routeFrequencyRawFileFormat = null;

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	@NonNull
	public static String getROUTE_FREQUENCY_RAW_FILE_FORMAT(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (routeFrequencyRawFileFormat == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					routeFrequencyRawFileFormat = "next_gtfs_frequency_route_%s";
				} else { // CURRENT = default
					routeFrequencyRawFileFormat = "current_gtfs_frequency_route_%s";
				}
			} else {
				routeFrequencyRawFileFormat = "gtfs_frequency_route_%s";
			}
		}
		return routeFrequencyRawFileFormat;
	}

	private static final String GTFS_ROUTE_FREQUENCY_FILE_COL_SPLIT_ON = ",";
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_COUNT = 5;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_SERVICE_IDX = 0;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_TRIP_IDX = 1;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_START_TIME_IDX = 2;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_END_TIME_IDX = 3;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_HEADWAY_IDX = 4;

	@NonNull
	private static ArrayList<Schedule.Timestamp> findTimestamps(@NonNull GTFSProvider provider, Schedule.ScheduleStatusFilter filter) {
		ArrayList<Schedule.Timestamp> allTimestamps = new ArrayList<Schedule.Timestamp>();
		RouteTripStop routeTripStop = filter.getRouteTripStop();
		int maxDataRequests = filter.getMaxDataRequestsOrDefault();
		int minUsefulResults = filter.getMinUsefulResultsOrDefault();
		long minDurationCoveredInMs = filter.getMinUsefulDurationCoveredInMsOrDefault();
		long lookBehindInMs = filter.getLookBehindInMsOrDefault();
		long timestamp = filter.getTimestampOrDefault();
		long minTimestampCovered = timestamp + minDurationCoveredInMs;
		Calendar now = TimeUtils.getNewCalendar(timestamp);
		if (lookBehindInMs > PROVIDER_PRECISION_IN_MS) {
			if (lookBehindInMs > 0) {
				now.add(Calendar.MILLISECOND, (int) -lookBehindInMs);
			}
		} else {
			if (PROVIDER_PRECISION_IN_MS > 0) {
				now.add(Calendar.MILLISECOND, (int) -PROVIDER_PRECISION_IN_MS);
			}
		}
		now.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Timestamp> dayTimestamps;
		String dayTime;
		String dayDate;
		int nbTimestamps = 0;
		int dataRequests = 0;
		while (dataRequests < maxDataRequests) {
			Date timeDate = now.getTime();
			dayDate = getDateFormat(provider.getContext()).formatThreadSafe(timeDate);
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				dayTime = String.valueOf(Integer.valueOf(getTimeFormat(provider.getContext()).formatThreadSafe(timeDate)) + TWENTY_FOUR_HOURS); //
			} else if (dataRequests == 1) { // ELSE IF today DO start now
				dayTime = getTimeFormat(provider.getContext()).formatThreadSafe(timeDate);
			} else { // ELSE tomorrow or later DO start at midnight
				dayTime = MIDNIGHT;
			}
			dayTimestamps =
					findScheduleList( //
							provider, //
							routeTripStop.getRoute().getId(), //
							routeTripStop.getTrip().getId(), //
							routeTripStop.getStop().getId(), //
							dayDate, //
							dayTime);
			dataRequests++; // 1 more data request done
			allTimestamps.addAll(dayTimestamps);
			if (lookBehindInMs == 0) {
				nbTimestamps += dayTimestamps.size();
			} else {
				for (Schedule.Timestamp dayTimestamp : dayTimestamps) {
					if (dayTimestamp.t >= timestamp) {
						nbTimestamps++;
					}
				}
			}
			if (nbTimestamps >= minUsefulResults && now.getTimeInMillis() >= minTimestampCovered) {
				break;
			}
			now.add(Calendar.DATE, +1); // NEXT DAY
		}
		return allTimestamps;
	}

	@Nullable
	private static String stopScheduleRawFileFormat = null;

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	@NonNull
	public static String getSTOP_SCHEDULE_RAW_FILE_FORMAT(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (stopScheduleRawFileFormat == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					stopScheduleRawFileFormat = "next_gtfs_schedule_stop_%s";
				} else { // CURRENT = default
					stopScheduleRawFileFormat = "current_gtfs_schedule_stop_%s";
				}
			} else {
				stopScheduleRawFileFormat = "gtfs_schedule_stop_%s";
			}
		}
		return stopScheduleRawFileFormat;
	}

	private static final String STOP_SCHEDULE_RAW_FILE_TYPE = "raw";

	private static final String GTFS_SCHEDULE_STOP_FILE_COL_SPLIT_ON = ",";
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_COUNT = 5;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX = 0;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_TRIP_IDX = 1;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX = 2;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX = 3;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX = 4;

	public static HashSet<Schedule.Timestamp> findScheduleList(GTFSProvider provider, long routeId, long tripId, int stopId, String dateS, String timeS) {
		long timeI = Integer.parseInt(timeS);
		HashSet<Schedule.Timestamp> result = new HashSet<Schedule.Timestamp>();
		HashSet<String> serviceIds = findServices(provider, dateS);
		BufferedReader br = null;
		String line = null;
		String fileName = String.format(getSTOP_SCHEDULE_RAW_FILE_FORMAT(provider.getContext()), stopId);
		try {
			int fileId = provider.getContext().getResources().getIdentifier(fileName, STOP_SCHEDULE_RAW_FILE_TYPE, provider.getContext().getPackageName());
			if (fileId == 0) {
				return result;
			}
			InputStream is = provider.getContext().getResources().openRawResource(fileId);
			br = new BufferedReader(new InputStreamReader(is, FileUtils.UTF_8), 8192);
			String[] lineItems;
			String lineServiceIdWithQuotes;
			String lineServiceId;
			long lineTripId;
			int lineDeparture;
			int lineDepartureDelta;
			Long tTimestampInMs;
			Schedule.Timestamp timestamp;
			String headsignTypeS;
			Integer headsignType;
			String headsignValueWithQuotes;
			while ((line = br.readLine()) != null) {
				try {
					lineItems = line.split(GTFS_SCHEDULE_STOP_FILE_COL_SPLIT_ON);
					if (lineItems.length < GTFS_SCHEDULE_STOP_FILE_COL_COUNT) {
						MTLog.w(TAG, "Cannot parse schedule '%s'!", line);
						continue;
					}
					lineServiceIdWithQuotes = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX];
					lineServiceId = lineServiceIdWithQuotes.substring(1, lineServiceIdWithQuotes.length() - 1);
					if (!serviceIds.contains(lineServiceId)) {
						continue;
					}
					lineTripId = Long.parseLong(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_TRIP_IDX]);
					if (tripId != lineTripId) {
						continue;
					}
					lineDeparture = Integer.parseInt(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX]);
					tTimestampInMs = convertToTimestamp(provider.getContext(), lineDeparture, dateS);
					if (lineDeparture > timeI) {
						if (tTimestampInMs != null) {
							timestamp = new Schedule.Timestamp(tTimestampInMs);
							timestamp.setLocalTimeZone(getTIME_ZONE(provider.getContext()));
							headsignTypeS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX];
							headsignType = TextUtils.isEmpty(headsignTypeS) ? null : Integer.valueOf(headsignTypeS);
							if (headsignType != null && headsignType >= 0) {
								headsignValueWithQuotes = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX];
								if (headsignValueWithQuotes.length() > 2) {
									timestamp.setHeadsign(headsignType, headsignValueWithQuotes.substring(1, headsignValueWithQuotes.length() - 1));
								}
							}
							result.add(timestamp);
						}
					}
					int nbExtra = (lineItems.length - GTFS_SCHEDULE_STOP_FILE_COL_COUNT) / 3;
					for (int i = 1; i <= nbExtra; i++) {
						lineDepartureDelta = Integer.parseInt(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX + i * 3]);
						lineDeparture += lineDepartureDelta;
						tTimestampInMs = convertToTimestamp(provider.getContext(), lineDeparture, dateS);
						if (lineDeparture > timeI) {
							if (tTimestampInMs != null) {
								timestamp = new Schedule.Timestamp(tTimestampInMs);
								timestamp.setLocalTimeZone(getTIME_ZONE(provider.getContext()));
								headsignTypeS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX + i * 3];
								headsignType = TextUtils.isEmpty(headsignTypeS) ? null : Integer.valueOf(headsignTypeS);
								if (headsignType != null && headsignType >= 0) {
									headsignValueWithQuotes = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX + i * 3];
									if (headsignValueWithQuotes.length() > 2) {
										timestamp.setHeadsign(headsignType, headsignValueWithQuotes.substring(1, headsignValueWithQuotes.length() - 1));
									}
								}
								result.add(timestamp);
							}
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Cannot parse schedule '%s' (fileName: %s)!", line, fileName);
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "ERROR while reading stop time from file! (fileName: %s, line: %s)", fileName, line);
		} finally {
			FileUtils.closeQuietly(br);
		}
		return result;
	}

	@NonNull
	private static ArrayList<Schedule.Frequency> findFrequencies(@NonNull GTFSProvider provider, @NonNull Schedule.ScheduleStatusFilter filter) {
		ArrayList<Schedule.Frequency> allFrequencies = new ArrayList<Schedule.Frequency>();
		RouteTripStop routeTripStop = filter.getRouteTripStop();
		int maxDataRequests = filter.getMaxDataRequestsOrDefault();
		long minDurationCoveredInMs = filter.getMinUsefulDurationCoveredInMsOrDefault();
		long timestamp = filter.getTimestampOrDefault();
		long minTimestampCovered = timestamp + minDurationCoveredInMs;
		Calendar now = TimeUtils.getNewCalendar(timestamp);
		now.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Frequency> dayFrequencies;
		String dayTime;
		String dayDate;
		int dataRequests = 0;
		while (dataRequests < maxDataRequests) {
			Date timeDate = now.getTime();
			dayDate = getDateFormat(provider.getContext()).formatThreadSafe(timeDate);
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				dayTime = String.valueOf(Integer.valueOf(getTimeFormat(provider.getContext()).formatThreadSafe(timeDate)) + TWENTY_FOUR_HOURS);
			} else if (dataRequests == 1) { // ELSE IF today DO start now
				dayTime = getTimeFormat(provider.getContext()).formatThreadSafe(timeDate);
			} else { // ELSE tomorrow or later DO start at midnight
				dayTime = MIDNIGHT;
			}
			dayFrequencies = findFrequencyList(provider, routeTripStop.getRoute().getId(), routeTripStop.getTrip().getId(), dayDate, dayTime);
			dataRequests++; // 1 more data request done
			for (Schedule.Frequency dayFrequency : dayFrequencies) {
				if (timestamp <= dayFrequency.endTimeInMs) {
					allFrequencies.add(dayFrequency);
				}
			}
			if (now.getTimeInMillis() >= minTimestampCovered) {
				break;
			}
			now.add(Calendar.DATE, +1); // NEXT DAY
		}
		return allFrequencies;
	}

	@NonNull
	private static HashSet<Schedule.Frequency> findFrequencyList(@NonNull GTFSProvider provider, long routeId, long tripId, String dateS, String timeS) {
		long timeI = Integer.parseInt(timeS);
		HashSet<Schedule.Frequency> result = new HashSet<Schedule.Frequency>();
		HashSet<String> serviceIds = findServices(provider, dateS);
		BufferedReader br = null;
		String line = null;
		String fileName = String.format(getROUTE_FREQUENCY_RAW_FILE_FORMAT(provider.getContext()), routeId);
		int fileId;
		InputStream is;
		String[] lineItems;
		String lineServiceIdWithQuotes;
		String lineServiceId;
		long lineTripId;
		int endTime;
		int startTime;
		Long tStartTimeInMs;
		Long tEndTimeInMs;
		Integer tHeadway;
		try {
			fileId = provider.getContext().getResources().getIdentifier(fileName, ROUTE_FREQUENCY_RAW_FILE_TYPE, provider.getContext().getPackageName());
			if (fileId == 0) {
				return result;
			}
			is = provider.getContext().getResources().openRawResource(fileId);
			br = new BufferedReader(new InputStreamReader(is, FileUtils.UTF_8), 8192);
			while ((line = br.readLine()) != null) {
				try {
					lineItems = line.split(GTFS_ROUTE_FREQUENCY_FILE_COL_SPLIT_ON);
					if (lineItems.length != GTFS_ROUTE_FREQUENCY_FILE_COL_COUNT) {
						MTLog.w(TAG, "Cannot parse frequency '%s'!", line);
						continue;
					}
					lineServiceIdWithQuotes = lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_SERVICE_IDX];
					lineServiceId = lineServiceIdWithQuotes.substring(1, lineServiceIdWithQuotes.length() - 1);
					if (!serviceIds.contains(lineServiceId)) {
						continue;
					}
					lineTripId = Long.parseLong(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_TRIP_IDX]);
					if (tripId != lineTripId) {
						continue;
					}
					endTime = Integer.parseInt(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_END_TIME_IDX]);
					if (timeI <= endTime) {
						startTime = Integer.parseInt(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_START_TIME_IDX]);
						tStartTimeInMs = convertToTimestamp(provider.getContext(), startTime, dateS);
						tEndTimeInMs = convertToTimestamp(provider.getContext(), endTime, dateS);
						tHeadway = Integer.valueOf(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_HEADWAY_IDX]);
						if (tStartTimeInMs != null && tEndTimeInMs != null && tHeadway != null) {
							result.add(new Schedule.Frequency(tStartTimeInMs, tEndTimeInMs, tHeadway));
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Cannot parse frequency '%s' (fileName: %s)!", line, fileName);
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "ERROR while reading route frequency from file! (fileName: %s, line: %s)", fileName, line);
		} finally {
			FileUtils.closeQuietly(br);
		}
		return result;
	}

	private static final String TIME_FORMATTER = "%06d";

	@Nullable
	private static Long convertToTimestamp(Context context, int timeInt, String dateS) {
		try {
			Date parsedDate = getToTimestampFormat(context).parseThreadSafe(dateS + String.format(Locale.ENGLISH, TIME_FORMATTER, timeInt));
			return parsedDate.getTime();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while parsing time %s %s!", dateS, timeInt);
			return null;
		}
	}

	private static final String TO_TIMESTAMP_FORMAT_PATTERN = "yyyyMMdd" + "HHmmss";

	@Nullable
	private static ThreadSafeDateFormatter toTimestampFormat;

	@NonNull
	public static ThreadSafeDateFormatter getToTimestampFormat(Context context) {
		if (toTimestampFormat == null) {
			toTimestampFormat = new ThreadSafeDateFormatter(TO_TIMESTAMP_FORMAT_PATTERN);
			toTimestampFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return toTimestampFormat;
	}

	private static final String[] PROJECTION_SERVICE_DATES = new String[] { ServiceDateColumns.T_SERVICE_DATES_K_SERVICE_ID };

	public static HashSet<String> findServices(GTFSProvider provider, String dateS) {
		HashSet<String> serviceIds = new HashSet<String>();
		Cursor cursor = null;
		try {
			String where = SqlUtils.getWhereEquals(ServiceDateColumns.T_SERVICE_DATES_K_DATE, dateS);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSProviderDbHelper.T_SERVICE_DATES);
			cursor = qb.query(provider.getDBHelper().getReadableDatabase(), PROJECTION_SERVICE_DATES, where, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						String serviceId = cursor.getString(0);
						if (!TextUtils.isEmpty(serviceId)) {
							serviceIds.add(serviceId);
						}
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return serviceIds;
	}

	public static void cacheStatusS(@NonNull GTFSProvider provider, POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(provider, newStatusToCache);
	}

	public static POIStatus getCachedStatus(@NonNull GTFSProvider provider, StatusProviderContract.Filter statusFilter) {
		return StatusProvider.getCachedStatusS(provider, statusFilter.getTargetUUID());
	}

	public static boolean purgeUselessCachedStatuses(@NonNull GTFSProvider provider) {
		return StatusProvider.purgeUselessCachedStatuses(provider);
	}

	public static boolean deleteCachedStatus(@NonNull GTFSProvider provider, int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(provider, cachedStatusId);
	}

	public static String getStatusDbTableName(@NonNull GTFSProvider provider) {
		return GTFSProviderDbHelper.T_ROUTE_TRIP_STOP_STATUS;
	}

	public static Cursor queryS(@NonNull GTFSProvider provider, Uri uri, String selection) {
		return StatusProvider.queryS(provider, uri, selection);
	}

	public static String getSortOrderS(@NonNull GTFSProvider provider, Uri uri) {
		return StatusProvider.getSortOrderS(provider, uri);
	}

	public static String getTypeS(@NonNull GTFSProvider provider, Uri uri) {
		return StatusProvider.getTypeS(provider, uri);
	}

	public static int getStatusType(@NonNull GTFSProvider provider) {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}
}
