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
import org.mtransit.commons.FeatureFlags;

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

public class GTFSStatusProvider implements MTLog.Loggable {

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
	private static String timeZone = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	@NonNull
	static String getTIME_ZONE(@NonNull Context context) {
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
	private static boolean isSCHEDULE_AVAILABLE(@NonNull Context context) {
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
	private static boolean isFREQUENCY_AVAILABLE(@NonNull Context context) {
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
		Schedule schedule = new Schedule(statusFilter.getTargetUUID(), scheduleStatusFilter.getTimestampOrDefault(), getStatusMaxValidityInMs(),
				PROVIDER_READ_FROM_SOURCE_AT_IN_MS, PROVIDER_PRECISION_IN_MS, scheduleStatusFilter.getRouteTripStop().isNoPickup());
		//noinspection ConstantConditions // TODO requireContext()
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
	static ThreadSafeDateFormatter getDateFormat(@NonNull Context context) {
		if (dateFormat == null) {
			dateFormat = new ThreadSafeDateFormatter(DATE_FORMAT_PATTERN, Locale.ENGLISH);
			dateFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
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
			timeFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return timeFormat;
	}

	static final int TWENTY_FOUR_HOURS = 24_00_00;

	static final String MIDNIGHT = "000000";

	static final long TWENTY_FOUR_HOURS_IN_MS = TimeUnit.HOURS.toMillis(24L);

	static final long ONE_WEEK_IN_MS = TimeUnit.DAYS.toMillis(7L);

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

	private static final String GTFS_ROUTE_FREQUENCY_FILE_COL_SPLIT_ON = ",";
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_COUNT = 5;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_SERVICE_IDX = 0;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_TRIP_IDX = 1;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_START_TIME_IDX = 2;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_END_TIME_IDX = 3;
	private static final int GTFS_ROUTE_FREQUENCY_FILE_COL_HEADWAY_IDX = 4;

	@NonNull
	private static ArrayList<Schedule.Timestamp> findTimestamps(@NonNull GTFSProvider provider, Schedule.ScheduleStatusFilter filter) {
		ArrayList<Schedule.Timestamp> allTimestamps = new ArrayList<>();
		final RouteTripStop rts = filter.getRouteTripStop();
		int maxDataRequests = filter.getMaxDataRequestsOrDefault();
		int minUsefulResults = filter.getMinUsefulResultsOrDefault();
		long minDurationCoveredInMs = filter.getMinUsefulDurationCoveredInMsOrDefault();
		long lookBehindInMs = filter.getLookBehindInMsOrDefault();
		long timestamp = filter.getTimestampOrDefault();
		long minTimestampCoveredIntMs = timestamp + minDurationCoveredInMs;
		//noinspection ConstantConditions // TODO requireContext()
		final ThreadSafeDateFormatter dateFormat = getDateFormat(provider.getContext());
		final ThreadSafeDateFormatter timeFormat = getTimeFormat(provider.getContext());
		final TimeZone timeZone = TimeZone.getTimeZone(getTIME_ZONE(provider.getContext()));
		Calendar now = TimeUtils.getNewCalendar(timeZone, timestamp);
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
		HashSet<Schedule.Timestamp> dayTimestamps;
		String dayTime;
		String dayDate;
		int nbTimestamps = 0;
		int dataRequests = 0;
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_DEPARTURE_IN_SEC(provider.getContext()));
		while (dataRequests < maxDataRequests) {
			long timeInMs = now.getTimeInMillis();
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				timeInMs += TWENTY_FOUR_HOURS_IN_MS;
			} else if (dataRequests == 1) {
				// DO NOTHING
			} else {
				// DO NOTHING
			}
			long diffWithRealityInMs = 0L;
			while (timeInMs - diffWithRealityInMs > lastDepartureInMs) {
				diffWithRealityInMs += ONE_WEEK_IN_MS;
			}
			timeInMs -= diffWithRealityInMs;
			final Date timeDate = new Date(timeInMs);
			dayDate = dateFormat.formatThreadSafe(timeDate);
			dayTime = timeFormat.formatThreadSafe(timeDate);
			if (dataRequests == 0) { // IF yesterday DO override computed date & time with GTFS format for 24+
				dayDate = dateFormat.formatThreadSafe(now.getTimeInMillis() - diffWithRealityInMs);
				dayTime = String.valueOf(Integer.parseInt(dayTime) + TWENTY_FOUR_HOURS);
			} else if (dataRequests == 1) { // ELSE IF today DO
				// DO NOTHING
			} else { // ELSE IF tomorrow or later DO
				dayTime = MIDNIGHT;
			}
			dayTimestamps = findScheduleList(
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
				dayDate = dateFormat.formatThreadSafe(new Date(timeInMs - ONE_WEEK_IN_MS)); // try 1 week before once
				dayTimestamps = findScheduleList(
						provider,
						rts.getRoute().getId(),
						rts.getTrip().getId(),
						rts.getStop().getId(),
						dayDate,
						dayTime,
						diffWithRealityInMs + ONE_WEEK_IN_MS
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

	private static final String GTFS_SCHEDULE_STOP_FILE_COL_SPLIT_ON = ",";
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_COUNT = FeatureFlags.F_ACCESSIBILITY_PRODUCER ? 6 : 5;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA = FeatureFlags.F_ACCESSIBILITY_PRODUCER ? 4 : 3;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_SERVICE_IDX = 0;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_TRIP_IDX = 1;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX = 2;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX = 3;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX = 4;
	private static final int GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX = 5;

	@NonNull
	static HashSet<Schedule.Timestamp> findScheduleList(@NonNull GTFSProvider provider,
														@SuppressWarnings("unused") long routeId,
														long tripId,
														int stopId,
														String dateS, String timeS,
														long diffWithRealityInMs) {
		long timeI = Integer.parseInt(timeS);
		HashSet<Schedule.Timestamp> result = new HashSet<>();
		HashSet<String> serviceIds = findServices(provider, dateS);
		BufferedReader br = null;
		String line = null;
		//noinspection ConstantConditions // TODO requireContext()
		String fileName = String.format(getSTOP_SCHEDULE_RAW_FILE_FORMAT(provider.getContext()), stopId);
		try {
			@SuppressLint("DiscouragedApi")
			int fileId = provider.getContext().getResources().getIdentifier(fileName, STOP_SCHEDULE_RAW_FILE_TYPE, provider.getContext().getPackageName());
			if (fileId == 0) {
				return result;
			}
			InputStream is = provider.getContext().getResources().openRawResource(fileId);
			br = new BufferedReader(new InputStreamReader(is, FileUtils.getUTF8()), 8192);
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
			String accessibleS;
			Integer accessible;
			while ((line = br.readLine()) != null) {
				try {
					lineItems = line.split(GTFS_SCHEDULE_STOP_FILE_COL_SPLIT_ON);
					if (lineItems.length < GTFS_SCHEDULE_STOP_FILE_COL_COUNT) {
						MTLog.w(LOG_TAG, "Cannot parse schedule '%s'!", line);
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
							timestamp = new Schedule.Timestamp(tTimestampInMs + diffWithRealityInMs);
							timestamp.setLocalTimeZone(getTIME_ZONE(provider.getContext()));
							headsignTypeS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX];
							headsignType = TextUtils.isEmpty(headsignTypeS) ? null : Integer.valueOf(headsignTypeS);
							if (headsignType != null && headsignType >= 0) {
								headsignValueWithQuotes = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX];
								if (headsignValueWithQuotes.length() > 2) {
									timestamp.setHeadsign(headsignType, headsignValueWithQuotes.substring(1, headsignValueWithQuotes.length() - 1));
								} else {
									timestamp.setHeadsign(headsignType, null);
								}
							}
							timestamp.setOldSchedule(diffWithRealityInMs > 0L);
							timestamp.setRealTime(false); // static
							if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
								accessibleS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX];
								accessible = TextUtils.isEmpty(accessibleS) ? null : Integer.valueOf(accessibleS);
								if (accessible != null && accessible >= 0) {
									timestamp.setAccessible(accessible);
								}
							}
							result.add(timestamp);
						}
					}
					final int nbExtra = (lineItems.length - GTFS_SCHEDULE_STOP_FILE_COL_COUNT) / GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA;
					for (int i = 1; i <= nbExtra; i++) {
						final int extraIdx = i * GTFS_SCHEDULE_STOP_FILE_COL_COUNT_EXTRA;
						lineDepartureDelta = Integer.parseInt(lineItems[GTFS_SCHEDULE_STOP_FILE_COL_DEPARTURE_IDX + extraIdx]);
						lineDeparture += lineDepartureDelta;
						tTimestampInMs = convertToTimestamp(provider.getContext(), lineDeparture, dateS);
						if (lineDeparture > timeI) {
							if (tTimestampInMs != null) {
								timestamp = new Schedule.Timestamp(tTimestampInMs + diffWithRealityInMs);
								timestamp.setLocalTimeZone(getTIME_ZONE(provider.getContext()));
								headsignTypeS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_TYPE_IDX + extraIdx];
								headsignType = TextUtils.isEmpty(headsignTypeS) ? null : Integer.valueOf(headsignTypeS);
								if (headsignType != null && headsignType >= 0) {
									headsignValueWithQuotes = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_HEADSIGN_VALUE_IDX + extraIdx];
									if (headsignValueWithQuotes.length() > 2) {
										timestamp.setHeadsign(headsignType, headsignValueWithQuotes.substring(1, headsignValueWithQuotes.length() - 1));
									} else {
										timestamp.setHeadsign(headsignType, null);
									}
								}
								timestamp.setOldSchedule(diffWithRealityInMs > 0L);
								timestamp.setRealTime(false); // static
								if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
									accessibleS = lineItems[GTFS_SCHEDULE_STOP_FILE_COL_ACCESSIBLE_IDX + extraIdx];
									accessible = TextUtils.isEmpty(accessibleS) ? null : Integer.valueOf(accessibleS);
									if (accessible != null && accessible >= 0) {
										timestamp.setAccessible(accessible);
									}
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
	private static ArrayList<Schedule.Frequency> findFrequencies(@NonNull GTFSProvider provider, @NonNull Schedule.ScheduleStatusFilter filter) {
		ArrayList<Schedule.Frequency> allFrequencies = new ArrayList<>();
		RouteTripStop rts = filter.getRouteTripStop();
		int maxDataRequests = filter.getMaxDataRequestsOrDefault();
		long minDurationCoveredInMs = filter.getMinUsefulDurationCoveredInMsOrDefault();
		long timestamp = filter.getTimestampOrDefault();
		long minTimestampCovered = timestamp + minDurationCoveredInMs;
		//noinspection ConstantConditions // TODO requireContext()
		final ThreadSafeDateFormatter dateFormat = getDateFormat(provider.getContext());
		final ThreadSafeDateFormatter timeFormat = getTimeFormat(provider.getContext());
		final TimeZone timeZone = TimeZone.getTimeZone(getTIME_ZONE(provider.getContext()));
		Calendar now = TimeUtils.getNewCalendar(timeZone, timestamp);
		now.add(Calendar.DATE, -1); // starting yesterday
		HashSet<Schedule.Frequency> dayFrequencies;
		String dayTime;
		String dayDate;
		int dataRequests = 0;
		final long lastDepartureInMs = TimeUnit.SECONDS.toMillis(GTFSCurrentNextProvider.getLAST_DEPARTURE_IN_SEC(provider.getContext()));
		while (dataRequests < maxDataRequests) {
			long timeInMs = now.getTimeInMillis();
			if (dataRequests == 0) { // IF yesterday DO look for trips started yesterday
				timeInMs += TWENTY_FOUR_HOURS_IN_MS;
			} else if (dataRequests == 1) {
				// DO NOTHING
			} else {
				// DO NOTHING
			}
			long diffWithRealityInMs = 0L;
			while (timeInMs - diffWithRealityInMs > lastDepartureInMs) {
				diffWithRealityInMs += ONE_WEEK_IN_MS;
			}
			timeInMs -= diffWithRealityInMs;
			final Date timeDate = new Date(timeInMs);
			dayDate = dateFormat.formatThreadSafe(timeDate);
			dayTime = timeFormat.formatThreadSafe(timeDate);
			if (dataRequests == 0) { // IF yesterday DO override computed date & time with GTFS format for 24+
				dayDate = dateFormat.formatThreadSafe(now.getTimeInMillis() - diffWithRealityInMs);
				dayTime = String.valueOf(Integer.parseInt(dayTime) + TWENTY_FOUR_HOURS);
			} else if (dataRequests == 1) { // ELSE IF today DO
				// DO NOTHING
			} else { // ELSE IF tomorrow or later DO
				dayTime = MIDNIGHT;
			}
			dayFrequencies = findFrequencyList(
					provider,
					rts.getRoute().getId(),
					rts.getTrip().getId(),
					dayDate,
					dayTime,
					diffWithRealityInMs
			);
			if (dayFrequencies.isEmpty()
					&& diffWithRealityInMs > 0L // already looking at OLD schedule
					&& MIDNIGHT.equals(dayTime) // not a partial schedule
			) {
				dayDate = dateFormat.formatThreadSafe(new Date(timeInMs - ONE_WEEK_IN_MS)); // try 1 week before once
				dayFrequencies = findFrequencyList(
						provider,
						rts.getRoute().getId(),
						rts.getTrip().getId(),
						dayDate,
						dayTime,
						diffWithRealityInMs + ONE_WEEK_IN_MS
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
																 long routeId, long tripId,
																 String dateS, String timeS,
																 long diffWithRealityInMs) {
		long timeI = Integer.parseInt(timeS);
		HashSet<Schedule.Frequency> result = new HashSet<>();
		HashSet<String> serviceIds = findServices(provider, dateS);
		BufferedReader br = null;
		String line = null;
		//noinspection ConstantConditions // TODO requireContext()
		String fileName = String.format(getROUTE_FREQUENCY_RAW_FILE_FORMAT(provider.getContext()), routeId);
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
			// @SuppressLint("DiscouragedApi")
			@SuppressLint("DiscouragedApi")
			int fileId = provider.getContext().getResources().getIdentifier(fileName, ROUTE_FREQUENCY_RAW_FILE_TYPE, provider.getContext().getPackageName());
			if (fileId == 0) {
				return result;
			}
			is = provider.getContext().getResources().openRawResource(fileId);
			br = new BufferedReader(new InputStreamReader(is, FileUtils.getUTF8()), 8192);
			while ((line = br.readLine()) != null) {
				try {
					lineItems = line.split(GTFS_ROUTE_FREQUENCY_FILE_COL_SPLIT_ON);
					if (lineItems.length != GTFS_ROUTE_FREQUENCY_FILE_COL_COUNT) {
						MTLog.w(LOG_TAG, "Cannot parse frequency '%s'!", line);
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
			Date parsedDate = getToTimestampFormat(context).parseThreadSafe(
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
			toTimestampFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return toTimestampFormat;
	}

	@NonNull
	private static final String[] PROJECTION_SERVICE_DATES = new String[]{ServiceDateColumns.T_SERVICE_DATES_K_SERVICE_ID};

	@NonNull
	private static HashSet<String> findServices(@NonNull GTFSProvider provider, @NonNull String dateS) {
		HashSet<String> serviceIds = new HashSet<>();
		Cursor cursor = null;
		try {
			String where = SqlUtils.getWhereEquals(ServiceDateColumns.T_SERVICE_DATES_K_DATE, dateS);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(GTFSProviderDbHelper.T_SERVICE_DATES);
			cursor = qb.query(provider.getReadDB(), PROJECTION_SERVICE_DATES, where, null, null, null, null, null);
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
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return serviceIds;
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
		return GTFSProviderDbHelper.T_ROUTE_TRIP_STOP_STATUS;
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
