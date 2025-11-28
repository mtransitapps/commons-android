package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.agency.AgencyUtils;
import org.mtransit.android.commons.provider.gtfs.GTFSStringsUtils;
import org.mtransit.android.commons.provider.gtfs.GTFSTripIdsUtils;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.sql.SQLUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class GTFSStatusProvider implements MTLog.Loggable {

	private static final String LOG_TAG = GTFSStatusProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		StatusProvider.append(uriMatcher, authority);
	}

	@Nullable
	private static Boolean scheduleAvailable = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	private static boolean isSCHEDULE_AVAILABLE(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (scheduleAvailable == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					scheduleAvailable = context.getResources().getBoolean(R.bool.next_gtfs_rts_schedule_available); // do not change to avoid breaking compat w/ old modules
				} else { // CURRENT = default
					scheduleAvailable = context.getResources().getBoolean(R.bool.current_gtfs_rts_schedule_available); // do not change to avoid breaking compat w/ old modules
				}
			} else {
				scheduleAvailable = context.getResources().getBoolean(R.bool.gtfs_rts_schedule_available); // do not change to avoid breaking compat w/ old modules
			}
		}
		return scheduleAvailable;
	}

	@Nullable
	private static Boolean frequencyAvailable = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	private static boolean isFREQUENCY_AVAILABLE(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (frequencyAvailable == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					frequencyAvailable = context.getResources().getBoolean(R.bool.next_gtfs_rts_frequency_available); // do not change to avoid breaking compat w/ old modules
				} else { // CURRENT = default
					frequencyAvailable = context.getResources().getBoolean(R.bool.current_gtfs_rts_frequency_available); // do not change to avoid breaking compat w/ old modules
				}
			} else {
				frequencyAvailable = context.getResources().getBoolean(R.bool.gtfs_rts_frequency_available); // do not change to avoid breaking compat w/ old modules
			}
		}
		return frequencyAvailable;
	}

	static void onCurrentNextDataChange() {
		scheduleAvailable = null;
		stopScheduleRawFileFormat = null;
		frequencyAvailable = null;
		routeFrequencyRawFileFormat = null;
	}

	private static final long STATUS_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);

	private static final long STATUS_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(6L);

	private static final long STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(1L);

	private static final long STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.HOURS.toMillis(1L);

	private static final long STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(30L);

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
	public static POIStatus getNewStatus(@NonNull GTFSProvider provider, @NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(LOG_TAG, "Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		Schedule schedule = new Schedule(
				null,
				statusFilter.getTargetUUID(),
				scheduleStatusFilter.getTimestampOrDefault(),
				getStatusMaxValidityInMs(),
				PROVIDER_READ_FROM_SOURCE_AT_IN_MS,
				PROVIDER_PRECISION_IN_MS,
				scheduleStatusFilter.getRouteDirectionStop().isNoPickup(),
				GTFSProvider.getSOURCE_LABEL(provider.requireContextCompat()),
				false
		);
		if (isSCHEDULE_AVAILABLE(provider.requireContextCompat())) {
			schedule.setTimestampsAndSort(findTimestamps(provider, scheduleStatusFilter));
		}
		if (isFREQUENCY_AVAILABLE(provider.requireContextCompat())) {
			schedule.setFrequenciesAndSort(findFrequencies(provider, scheduleStatusFilter));
		}
		return schedule;
	}

	private static final String DATE_FORMAT_PATTERN = "yyyyMMdd";

	@Nullable
	private static ThreadSafeDateFormatter dateFormat;

	@NonNull
	static ThreadSafeDateFormatter getDateFormat(@NonNull Context context) {
		if (dateFormat == null) {
			dateFormat = new ThreadSafeDateFormatter(DATE_FORMAT_PATTERN, Locale.ENGLISH);
			dateFormat.setTimeZone(TimeZone.getTimeZone(AgencyUtils.getRDSAgencyTimeZone(context)));
		}
		return dateFormat;
	}

	private static final String TIME_FORMAT_PATTERN = "HHmmss";
	@Nullable
	private static ThreadSafeDateFormatter timeFormat;

	@NonNull
	static ThreadSafeDateFormatter getTimeFormat(@NonNull Context context) {
		if (timeFormat == null) {
			timeFormat = new ThreadSafeDateFormatter(TIME_FORMAT_PATTERN, Locale.ENGLISH);
			timeFormat.setTimeZone(TimeZone.getTimeZone(AgencyUtils.getRDSAgencyTimeZone(context)));
		}
		return timeFormat;
	}

	static final int TWENTY_FOUR_HOURS = 24_00_00;

	static final String MIDNIGHT = "000000";

	private static final String ROUTE_FREQUENCY_RAW_FILE_TYPE = "raw";

	@Nullable
	private static String routeFrequencyRawFileFormat = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	@NonNull
	private static String getROUTE_FREQUENCY_RAW_FILE_FORMAT(@NonNull Context context) {
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

	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_SERVICE_IDX = 0;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_DIRECTION_IDX = 1;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_START_TIME_IDX = 2;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_END_TIME_IDX = 3;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_HEADWAY_IDX = 4;
	// ->
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_COUNT = 5;

	@NonNull
	private static ArrayList<Schedule.Timestamp> findTimestamps(@NonNull GTFSProvider provider, Schedule.ScheduleStatusFilter filter) {
		ArrayList<Schedule.Timestamp> allTimestamps = new ArrayList<>();
		final RouteDirectionStop rds = filter.getRouteDirectionStop();
		final int maxDataRequests = filter.getMaxDataRequestsOrDefault();
		final int minUsefulResults = filter.getMinUsefulResultsOrDefault();
		final long minDurationCoveredInMs = filter.getMinUsefulDurationCoveredInMsOrDefault();
		final long lookBehindInMs = filter.getLookBehindInMsOrDefault();
		final long timestamp = filter.getTimestampOrDefault();
		final long minTimestampCoveredIntMs = timestamp + minDurationCoveredInMs;
		final Context context = provider.requireContextCompat();
		final ThreadSafeDateFormatter dateFormat = getDateFormat(context);
		final ThreadSafeDateFormatter timeFormat = getTimeFormat(context);
		final TimeZone timeZone = TimeZone.getTimeZone(AgencyUtils.getRDSAgencyTimeZone(context));
		final Calendar now = TimeUtils.getNewCalendar(timeZone, timestamp);
		if (lookBehindInMs > PROVIDER_PRECISION_IN_MS) {
			if (lookBehindInMs > 0L) {
				now.add(Calendar.MILLISECOND, (int) -lookBehindInMs);
			}
		} else {
			if (PROVIDER_PRECISION_IN_MS > 0L) {
				now.add(Calendar.MILLISECOND, (int) -PROVIDER_PRECISION_IN_MS);
			}
		}
		now.add(Calendar.DATE, -1); // starting yesterday
		Set<Schedule.Timestamp> dayTimestamps;
		String lookupDayTime;
		String lookupDayDate;
		int nbTimestamps = 0;
		int dataRequests = 0;
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_DEPARTURE_IN_SEC(context));
		while (dataRequests < maxDataRequests) {
			final Calendar lookupStartAt = TimeUtils.getNewCalendar(timeZone, now.getTimeInMillis());
			while (lookupStartAt.getTimeInMillis() > lastDepartureInMs) { // WHILE lookup time is after last departure DO
				lookupStartAt.add(Calendar.DATE, -7); // look 1 week behind
			}
			lookupDayDate = dateFormat.formatThreadSafe(lookupStartAt);
			lookupDayTime = timeFormat.formatThreadSafe(lookupStartAt);
			if (dataRequests == 0) { // IF yesterday DO override computed date & time with GTFS format for 24+
				lookupDayTime = String.valueOf(Integer.parseInt(lookupDayTime) + TWENTY_FOUR_HOURS);
			} else if (dataRequests == 1) { // ELSE IF today DO
				// DO NOTHING (keep now time)
			} else { // ELSE IF tomorrow or later DO
				lookupDayTime = MIDNIGHT;
			}
			dayTimestamps = findScheduleList(
					provider,
					rds.getRoute().getId(),
					rds.getDirection().getId(),
					rds.getStop().getId(),
					lookupDayDate,
					lookupDayTime,
					now.getTimeInMillis() - lookupStartAt.getTimeInMillis()
			);
			if (now.getTimeInMillis() > lookupStartAt.getTimeInMillis() // already looking at OLD schedule
					&& dayTimestamps.isEmpty()) {
				lookupDayDate = dateFormat.formatThreadSafe(lookupStartAt); // try 1 week before once
				dayTimestamps = findScheduleList(
						provider,
						rds.getRoute().getId(),
						rds.getDirection().getId(),
						rds.getStop().getId(),
						lookupDayDate,
						lookupDayTime,
						now.getTimeInMillis() - lookupStartAt.getTimeInMillis()
				);
			}
			dataRequests++; // 1 more data request done
			allTimestamps.addAll(dayTimestamps);
			if (lookBehindInMs == 0L) {
				nbTimestamps += dayTimestamps.size();
			} else {
				for (Schedule.Timestamp dayTimestamp : dayTimestamps) {
					if (dayTimestamp.t >= timestamp) {
						nbTimestamps++;
					}
				}
			}
			if (nbTimestamps >= minUsefulResults && now.getTimeInMillis() >= minTimestampCoveredIntMs) {
				break;
			}
			now.add(Calendar.DATE, +1); // NEXT DAY
		}
		if (FeatureFlags.F_EXPORT_STRINGS || FeatureFlags.F_EXPORT_SCHEDULE_STRINGS) {
			allTimestamps = GTFSStringsUtils.updateStrings(allTimestamps, provider);
		}
		if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
			allTimestamps = GTFSTripIdsUtils.updateTripIds(allTimestamps, provider);
		}
		return allTimestamps;
	}

	@Nullable
	private static String stopScheduleRawFileFormat = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	@NonNull
	private static String getSTOP_SCHEDULE_RAW_FILE_FORMAT(@NonNull Context context) {
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

	private static final int GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_DIRECTION_IDX;
	//
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_ARRIVAL_DIFF_IDX;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX;
	// ->
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_COUNT;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA;

	static {
		GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX = 0;
		GTFS_SCHEDULE_STOP_FILE_COL_DIRECTION_IDX = 1;
		//
		GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX = 2;
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			if (FeatureFlags.F_EXPORT_ARRIVAL_W_TRIP_ID) {
				GTFS_SCHEDULE_STOP_FILE_COL_ARRIVAL_DIFF_IDX = 3;
				GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX = 4;
				GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX = 5;
				GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX = 6;
				GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX = 7;
			} else {
				GTFS_SCHEDULE_STOP_FILE_COL_ARRIVAL_DIFF_IDX = -1;
				GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX = 3;
				GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX = 4;
				GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX = 5;
				GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX = 6;
			}
		} else {
			GTFS_SCHEDULE_STOP_FILE_COL_ARRIVAL_DIFF_IDX = -1;
			GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX = -1;
			GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX = 3;
			GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX = 4;
			GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX = 5;
		}
		// ->
		GTFS_SCHEDULE_STOP_FILE_COL_COUNT = GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX + 1;
		GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA = GTFS_SCHEDULE_STOP_FILE_COL_COUNT - 2;
	}

	@NonNull
	static Set<Schedule.Timestamp> findScheduleList(
			@NonNull GTFSProvider provider,
			@SuppressWarnings("unused") long routeId,
			long directionId,
			int stopId,
			String dateS, String timeS,
			long diffWithRealityInMs
	) {
		final int timeI = FeatureFlags.F_SCHEDULE_IN_MINUTES ? Integer.parseInt(timeS) / 100 : Integer.parseInt(timeS);
		Set<Schedule.Timestamp> result = new HashSet<>();
		final Set<Pair<String, Integer>> serviceIdOrIntAndExceptionTypes = findServicesAndExceptionTypes(provider, dateS);
		final Set<String> serviceIdOrInts = filterServiceIdOrInts(serviceIdOrIntAndExceptionTypes, diffWithRealityInMs > 0L);
		BufferedReader br = null;
		String line = null;
		final Context context = provider.requireContextCompat();
		final String localTimeZoneId = AgencyUtils.getRDSAgencyTimeZone(context);
		String fileName = String.format(getSTOP_SCHEDULE_RAW_FILE_FORMAT(context), stopId);
		try {
			@SuppressLint("DiscouragedApi")
			int fileId = context.getResources().getIdentifier(fileName, STOP_SCHEDULE_RAW_FILE_TYPE, context.getPackageName());
			if (fileId == 0) {
				return result;
			}
			InputStream is = context.getResources().openRawResource(fileId);
			br = new BufferedReader(new InputStreamReader(is, FileUtils.getUTF8()), 8192);
			String[] lineItems;
			String lineServiceIdOrInt;
			long lineDirectionId;
			int lineDeparture;
			int lineDepartureDelta;
			String arrivalDiffS;
			int arrivalDiff;
			Long tTimestampInMs;
			Long arrivalTimestampMs;
			Schedule.Timestamp timestamp;
			String tripIdOrInt;
			String headsignTypeS;
			Integer headsignType;
			String accessibleS;
			Integer accessible;
			while ((line = br.readLine()) != null) {
				try {
					lineItems = line.split(SQLUtils.COLUMN_SEPARATOR);
					if (lineItems.length < GTFS_SCHEDULE_STOP_FILE_COL_COUNT) {
						MTLog.w(LOG_TAG, "Cannot parse schedule '%s'!", line);
						continue;
					}
					if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
						lineServiceIdOrInt = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX];
					} else {
						lineServiceIdOrInt = SQLUtils.unquotes(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX]);
					}
					if (!serviceIdOrInts.contains(lineServiceIdOrInt)) {
						continue;
					}
					lineDirectionId = Long.parseLong(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_DIRECTION_IDX]);
					if (directionId != lineDirectionId) {
						continue;
					}
					lineDeparture = 0; // 1st departure contains full time "HHMMSS"
					final int nbExtra = (lineItems.length - GTFS_SCHEDULE_STOP_FILE_COL_COUNT) / GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA;
					for (int i = 0; i <= nbExtra; i++) {
						final int extraIdx = i * GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA;
						lineDepartureDelta = Integer.parseInt(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX + extraIdx]);
						lineDeparture += lineDepartureDelta;
						tTimestampInMs = convertToTimestamp(context, lineDeparture, dateS);
						if (lineDeparture > timeI) {
							if (tTimestampInMs != null) {
								timestamp = new Schedule.Timestamp(tTimestampInMs + diffWithRealityInMs, localTimeZoneId);
								if (FeatureFlags.F_EXPORT_TRIP_ID) {
									if (FeatureFlags.F_EXPORT_ARRIVAL_W_TRIP_ID && GTFS_SCHEDULE_STOP_FILE_COL_ARRIVAL_DIFF_IDX >= 0) {
										arrivalDiffS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_ARRIVAL_DIFF_IDX + extraIdx];
										if (!TextUtils.isEmpty(arrivalDiffS) && CharUtils.isDigitsOnly(arrivalDiffS)) {
											arrivalDiff = Integer.parseInt(arrivalDiffS);
											if (arrivalDiff > 0) {
												arrivalTimestampMs = convertToTimestamp(context, lineDeparture - arrivalDiff, dateS);
												if (arrivalTimestampMs != null) {
													timestamp.setArrivalTimestamp(arrivalTimestampMs);
												}
											}
										}
									}
									if (GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX >= 0) {
										if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
											tripIdOrInt = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX + extraIdx];
										} else {
											tripIdOrInt = SQLUtils.unquotes(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_TRIP_ID_IDX + extraIdx]);
										}
										if (!TextUtils.isEmpty(tripIdOrInt)) {
											timestamp.setTripId(tripIdOrInt);
										}
									}
								}
								headsignTypeS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX + extraIdx];
								headsignType = TextUtils.isEmpty(headsignTypeS) ? null : Integer.valueOf(headsignTypeS);
								if (headsignType != null && headsignType >= 0) {
									timestamp.setHeadsign(
											headsignType,
											SqlUtils.unquoteUnescapeStringOrNull(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX + extraIdx])
									);
								}
								timestamp.setOldSchedule(diffWithRealityInMs > 0L);
								timestamp.setRealTime(false); // static
								accessibleS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX + extraIdx];
								accessible = TextUtils.isEmpty(accessibleS) ? null : Integer.valueOf(accessibleS);
								if (accessible != null && accessible >= 0) {
									timestamp.setAccessible(accessible);
								}
								result.add(timestamp);
							}
						}
					}
				} catch (Exception e) {
					MTLog.w(LOG_TAG, e, "Cannot parse schedule '%s' (fileName: %s)!", line, fileName);
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "ERROR while reading stop time from file! (fileName: %s, line: %s)", fileName, line);
		} finally {
			FileUtils.closeQuietly(br);
		}
		return result;
	}

	@NonNull
	protected static Set<String> filterServiceIdOrInts(@NonNull Set<Pair<String, Integer>> serviceIdOrIntAndExceptionTypes, boolean usingAnotherDate) {
		final HashSet<String> serviceIdOrInts = new HashSet<>();
		final HashSet<String> serviceIdOrIntsToRemove = new HashSet<>();
		final HashSet<String> serviceIdOrIntsToAdd = new HashSet<>();
		for (Pair<String, Integer> serviceIdOrIntAndExceptionType : serviceIdOrIntAndExceptionTypes) {
			final String serviceIdOrInt = serviceIdOrIntAndExceptionType.first;
			final Integer exceptionType = serviceIdOrIntAndExceptionType.second;
			if (exceptionType == null) {
				MTLog.w(LOG_TAG, "SKIP invalid exception type for service ID '%s'!", serviceIdOrInt);
				continue;
			}
			switch (exceptionType) {
			case GTFSCommons.EXCEPTION_TYPE_DEFAULT:
				serviceIdOrInts.add(serviceIdOrInt);
				break;
			case GTFSCommons.EXCEPTION_TYPE_ADDED:
				if (usingAnotherDate) {
					serviceIdOrIntsToAdd.add(serviceIdOrInt); // maybe all services add ADDED (no calendar.txt provided)
				} else {
					serviceIdOrInts.add(serviceIdOrInt);
				}
				break;
			case GTFSCommons.EXCEPTION_TYPE_REMOVED:
				serviceIdOrIntsToRemove.add(serviceIdOrInt);
				break;
			default:
				MTLog.w(LOG_TAG, "Unexpected service ID exception type '%s' for '%s'!", exceptionType, serviceIdOrInt);
				break;
			}
		}
		if (usingAnotherDate) {
			if (serviceIdOrInts.isEmpty()) {
				serviceIdOrInts.addAll(serviceIdOrIntsToAdd); // maybe all services add ADDED (no calendar.txt provided)
			}
		} else {
			serviceIdOrInts.removeAll(serviceIdOrIntsToRemove);
		}
		return serviceIdOrInts;
	}

	@NonNull
	private static ArrayList<Schedule.Frequency> findFrequencies(@NonNull GTFSProvider provider, @NonNull Schedule.ScheduleStatusFilter filter) {
		final ArrayList<Schedule.Frequency> allFrequencies = new ArrayList<>();
		final RouteDirectionStop rds = filter.getRouteDirectionStop();
		final int maxDataRequests = filter.getMaxDataRequestsOrDefault();
		final long minDurationCoveredInMs = filter.getMinUsefulDurationCoveredInMsOrDefault();
		final long timestamp = filter.getTimestampOrDefault();
		final long minTimestampCovered = timestamp + minDurationCoveredInMs;
		final Context context = provider.requireContextCompat();
		final ThreadSafeDateFormatter dateFormat = getDateFormat(context);
		final ThreadSafeDateFormatter timeFormat = getTimeFormat(context);
		final TimeZone timeZone = TimeZone.getTimeZone(AgencyUtils.getRDSAgencyTimeZone(context));
		final Calendar now = TimeUtils.getNewCalendar(timeZone, timestamp);
		now.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Frequency> dayFrequencies;
		String lookupDayTime;
		String lookupDayDate;
		int dataRequests = 0;
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_DEPARTURE_IN_SEC(context));
		while (dataRequests < maxDataRequests) {
			final Calendar lookupTime = TimeUtils.getNewCalendar(timeZone, now.getTimeInMillis());
			while (lookupTime.getTimeInMillis() > lastDepartureInMs) { // WHILE lookup time is after last departure DO
				lookupTime.add(Calendar.DATE, -7); // look 1 week behind
			}
			lookupDayDate = dateFormat.formatThreadSafe(lookupTime);
			lookupDayTime = timeFormat.formatThreadSafe(lookupTime);
			if (dataRequests == 0) { // IF yesterday DO override computed date & time with GTFS format for 24+
				lookupDayTime = String.valueOf(Integer.parseInt(lookupDayTime) + TWENTY_FOUR_HOURS);
			} else if (dataRequests == 1) { // ELSE IF today DO
				// DO NOTHING (keep now time)
			} else { // ELSE IF tomorrow or later DO
				lookupDayTime = MIDNIGHT;
			}
			dayFrequencies = findFrequencyList(
					provider,
					rds.getRoute().getId(),
					rds.getDirection().getId(),
					lookupDayDate,
					lookupDayTime,
					now.getTimeInMillis() - lookupTime.getTimeInMillis()
			);
			if (dayFrequencies.isEmpty()
					&& now.getTimeInMillis() > lookupTime.getTimeInMillis() // already looking at OLD schedule
					&& MIDNIGHT.equals(lookupDayTime) // not a partial schedule
			) {
				lookupTime.add(Calendar.DATE, -7); // look 1 week behind
				lookupDayDate = dateFormat.formatThreadSafe(lookupTime); // try 1 week before once
				dayFrequencies = findFrequencyList(
						provider,
						rds.getRoute().getId(),
						rds.getDirection().getId(),
						lookupDayDate,
						lookupDayTime,
						now.getTimeInMillis() - lookupTime.getTimeInMillis()
				);
			}
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
	private static HashSet<Schedule.Frequency> findFrequencyList(@NonNull GTFSProvider provider,
																 long routeId, long directionId,
																 String dateS, String timeS,
																 long diffWithRealityInMs) {
		long timeI = Integer.parseInt(timeS);
		final HashSet<Schedule.Frequency> result = new HashSet<>();
		final Set<Pair<String, Integer>> serviceIdOrIntAndExceptionTypes = findServicesAndExceptionTypes(provider, dateS);
		final Set<String> serviceIdOrInts = filterServiceIdOrInts(serviceIdOrIntAndExceptionTypes, diffWithRealityInMs > 0L);
		BufferedReader br = null;
		String line = null;
		final Context context = provider.requireContextCompat();
		String fileName = String.format(getROUTE_FREQUENCY_RAW_FILE_FORMAT(context), routeId);
		InputStream is;
		String[] lineItems;
		String lineServiceIdOrInt;
		long lineDirectionId;
		int endTime;
		int startTime;
		Long tStartTimeInMs;
		Long tEndTimeInMs;
		Integer tHeadway;
		try {
			@SuppressLint("DiscouragedApi")
			int fileId = context.getResources().getIdentifier(fileName, ROUTE_FREQUENCY_RAW_FILE_TYPE, context.getPackageName());
			if (fileId == 0) {
				return result;
			}
			is = context.getResources().openRawResource(fileId);
			br = new BufferedReader(new InputStreamReader(is, FileUtils.getUTF8()), 8192);
			while ((line = br.readLine()) != null) {
				try {
					lineItems = line.split(SQLUtils.COLUMN_SEPARATOR);
					if (lineItems.length != GTFS_ROUTE_FREQUENCY_FILE_COL_COUNT) {
						MTLog.w(LOG_TAG, "Cannot parse frequency '%s'!", line);
						continue;
					}
					if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
						lineServiceIdOrInt = lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_SERVICE_IDX];
					} else {
						lineServiceIdOrInt = SQLUtils.unquotes(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_SERVICE_IDX]);
					}
					if (!serviceIdOrInts.contains(lineServiceIdOrInt)) {
						continue;
					}
					lineDirectionId = Long.parseLong(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_DIRECTION_IDX]);
					if (directionId != lineDirectionId) {
						continue;
					}
					endTime = Integer.parseInt(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_END_TIME_IDX]);
					if (timeI <= endTime) {
						startTime = Integer.parseInt(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_START_TIME_IDX]);
						tStartTimeInMs = convertToTimestamp(context, startTime, dateS);
						tEndTimeInMs = convertToTimestamp(context, endTime, dateS);
						tHeadway = Integer.valueOf(lineItems[GTFS_ROUTE_FREQUENCY_FILE_COL_HEADWAY_IDX]);
						//noinspection ConstantConditions
						if (tStartTimeInMs != null && tEndTimeInMs != null && tHeadway != null) {
							result.add(new Schedule.Frequency(
									tStartTimeInMs + diffWithRealityInMs,
									tEndTimeInMs + diffWithRealityInMs,
									tHeadway,
									diffWithRealityInMs > 0L
							));
						}
					}
				} catch (Exception e) {
					MTLog.w(LOG_TAG, e, "Cannot parse frequency '%s' (fileName: %s)!", line, fileName);
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "ERROR while reading route frequency from file! (fileName: %s, line: %s)", fileName, line);
		} finally {
			FileUtils.closeQuietly(br);
		}
		return result;
	}

	private static final String TIME_FORMATTER = "%06d";

	@Nullable
	private static Long convertToTimestamp(Context context, int timeInt, String dateS) {
		try {
			if (FeatureFlags.F_SCHEDULE_IN_MINUTES) {
				timeInt *= 100; // HHMM -> HHMMSS
			}
			final Date parsedDate = getToTimestampFormat(context).parseThreadSafe(
					dateS + String.format(Locale.ENGLISH, TIME_FORMATTER, timeInt)
			);
			return parsedDate == null ? null : parsedDate.getTime();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while parsing time %s %s!", dateS, timeInt);
			return null;
		}
	}

	private static final String TO_TIMESTAMP_FORMAT_PATTERN = "yyyyMMdd" + "HHmmss";

	@Nullable
	private static ThreadSafeDateFormatter toTimestampFormat;

	@NonNull
	private static ThreadSafeDateFormatter getToTimestampFormat(Context context) {
		if (toTimestampFormat == null) {
			toTimestampFormat = new ThreadSafeDateFormatter(TO_TIMESTAMP_FORMAT_PATTERN, Locale.ENGLISH);
			toTimestampFormat.setTimeZone(TimeZone.getTimeZone(AgencyUtils.getRDSAgencyTimeZone(context)));
		}
		return toTimestampFormat;
	}

	@Nullable
	public static Integer findLastServiceDate(@NonNull GTFSProvider provider) {
		Integer lastServiceDate = null;
		Cursor cursor = null;
		try {
			// same as ? DatabaseUtils.longForQuery(provider.getReadDB(), "SELECT MAX(" + GTFSCommons.T_SERVICE_DATES_K_DATE + ") FROM " + GTFSProviderDbHelper.T_SERVICE_DATES, null);
			final String[] projection = new String[]{
					SqlUtils.getMaxValue(GTFSCommons.T_SERVICE_DATES_K_DATE)
			};
			final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSProviderDbHelper.T_SERVICE_DATES);
			cursor = qb.query(provider.getReadDB(), projection, null, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					lastServiceDate = cursor.getInt(0);
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while reading last service date!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return lastServiceDate;
	}

	@NonNull
	private static final String[] PROJECTION_SERVICE_DATES =
			FeatureFlags.F_EXPORT_SERVICE_ID_INTS ? new String[]{
					GTFSCommons.T_SERVICE_DATES_K_SERVICE_ID_INT,
					GTFSCommons.T_SERVICE_DATES_K_EXCEPTION_TYPE
			} : new String[]{
					GTFSCommons.T_SERVICE_DATES_K_SERVICE_ID,
					GTFSCommons.T_SERVICE_DATES_K_EXCEPTION_TYPE
			};

	@NonNull
	private static HashSet<Pair<String, Integer>> findServicesAndExceptionTypes(@NonNull GTFSProvider provider, @NonNull String dateS) {
		final HashSet<Pair<String, Integer>> serviceIdOrIntAndExceptionTypes = new HashSet<>();
		Cursor cursor = null;
		try {
			final String selection = SqlUtils.getWhereEquals(GTFSProviderDbHelper.T_SERVICE_DATES_K_DATE, dateS);
			final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSProviderDbHelper.T_SERVICE_DATES);
			cursor = qb.query(provider.getReadDB(), PROJECTION_SERVICE_DATES, selection, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						final String serviceIdOrInt;
						if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
							serviceIdOrInt = CursorExtKt.getString(cursor, GTFSProviderDbHelper.T_SERVICE_DATES_K_SERVICE_ID_INT);
						} else {
							serviceIdOrInt = CursorExtKt.getString(cursor, GTFSProviderDbHelper.T_SERVICE_DATES_K_SERVICE_ID);
						}
						final int exceptionType = CursorExtKt.optIntNN(cursor, GTFSProviderDbHelper.T_SERVICE_DATES_K_EXCEPTION_TYPE, GTFSCommons.EXCEPTION_TYPE_DEFAULT);
						if (!TextUtils.isEmpty(serviceIdOrInt)) {
							serviceIdOrIntAndExceptionTypes.add(new Pair<>(serviceIdOrInt, exceptionType));
						}
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return serviceIdOrIntAndExceptionTypes;
	}

	public static void cacheStatusS(@NonNull GTFSProvider provider, @NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(provider, newStatusToCache);
	}

	@Nullable
	public static POIStatus getCachedStatus(@NonNull GTFSProvider provider, @NonNull StatusProviderContract.Filter statusFilter) {
		return StatusProvider.getCachedStatusS(provider, statusFilter.getTargetUUID());
	}

	public static boolean purgeUselessCachedStatuses(@NonNull GTFSProvider provider) {
		return StatusProvider.purgeUselessCachedStatuses(provider);
	}

	public static boolean deleteCachedStatus(@NonNull GTFSProvider provider, int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(provider, cachedStatusId);
	}

	@NonNull
	public static String getStatusDbTableName(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return GTFSProviderDbHelper.T_ROUTE_DIRECTION_STOP_STATUS;
	}

	@Nullable
	public static Cursor queryS(@NonNull GTFSProvider provider, @NonNull Uri uri, @Nullable String selection) {
		return StatusProvider.queryS(provider, uri, selection);
	}

	static String getSortOrderS(@NonNull GTFSProvider provider, Uri uri) {
		return StatusProvider.getSortOrderS(provider, uri);
	}

	@Nullable
	public static String getTypeS(@NonNull GTFSProvider provider, @NonNull Uri uri) {
		return StatusProvider.getTypeS(provider, uri);
	}

	public static int getStatusType(@SuppressWarnings("unused") @NonNull GTFSProvider provider) {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}
}
