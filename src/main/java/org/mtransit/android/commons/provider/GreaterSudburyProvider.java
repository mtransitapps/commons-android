package org.mtransit.android.commons.provider;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.gson.annotations.SerializedName;

import org.mtransit.android.commons.KeysIds;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SecureStringUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.agency.AgencyUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.SourceUtils;
import org.mtransit.commons.provider.GreaterSudburyProviderCommons;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

// https://opendata.greatersudbury.ca/datasets/mybus-transit-api
// https://dataportal.greatersudbury.ca/swagger/ui/index#/MyBus
@SuppressLint("Registered")
public class GreaterSudburyProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = GreaterSudburyProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static UriMatcher getURIMATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.greater_sudbury_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String authToken = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTH_TOKEN(@NonNull Context context) {
		if (authToken == null) {
			authToken = context.getResources().getString(R.string.greater_sudbury_auth_token);
		}
		return authToken;
	}

	@Nullable
	private String providedAuthToken = null;

	private static final long MY_BUS_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long MY_BUS_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long MY_BUS_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long MY_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long MY_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return MY_BUS_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return MY_BUS_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return MY_BUS_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return MY_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return MY_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getCachedStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			return null;
		}
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, getAgencyRouteStopTargetUUID(rts));
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom MyBus API tags
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
				}
			}
		}
		return cachedStatus;
	}

	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(
				rts.getAuthority(),
				rts.getRoute().getShortName(),
				rts.isNoPickup(), // "like" trip ID
				rts.getStop().getCode()
		);
	}

	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, boolean noPickup, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, noPickup ? 1 : 0, stopCode);
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return StatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return GreaterSudburyDbHelper.T_MY_BUS_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		this.providedAuthToken = SecureStringUtils.dec(statusFilter.getProvidedEncryptKey(KeysIds.CA_SUDBURY_TRANSIT_AUTH_TOKEN));
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	@Nullable
	private SudburyTransitApiV2 sudburyTransitApi = null;

	@NonNull
	private SudburyTransitApiV2 getSudburyTransitApi(@NonNull Context context) {
		if (this.sudburyTransitApi == null) {
			this.sudburyTransitApi = createSudburyTransitApi(context);
		}
		return this.sudburyTransitApi;
	}

	@NonNull
	private SudburyTransitApiV2 createSudburyTransitApi(@NonNull Context context) {
		final Retrofit retrofit = NetworkUtils.makeNewRetrofitWithGson(
				BASE_HOST_URL,
				context,
				NetworkUtils.makeNewOkHttpClientWithInterceptor(context),
				DATA_FORMAT);
		return retrofit.create(SudburyTransitApiV2.class);
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final String DATA_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";

	private static final String BASE_HOST = "greatersudbury.ca";
	private static final String BASE_HOST_URL = "https://dataportal." + BASE_HOST + "/api/";

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			final Context context = requireContextCompat();
			if (TextUtils.isEmpty(rts.getStop().getCode())) {
				MTLog.w(LOG_TAG, "Can't create real-time status URL (no stop code) for %s", rts);
				return;
			}
			String sourceLabel = SourceUtils.getSourceLabel(BASE_HOST_URL);
			MTLog.i(this, "Loading from '%s' for stop '%s'...", BASE_HOST_URL, rts.getStop().getCode());
			Call<SudburyTransitApiV2.JStopResponse> call = getSudburyTransitApi(context).stops(
					rts.getStop().getCode(),
					this.providedAuthToken != null ? this.providedAuthToken : getAUTH_TOKEN(context)
			);
			Response<SudburyTransitApiV2.JStopResponse> response = call.execute();
			if (response.isSuccessful()) {
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SudburyTransitApiV2.JStopResponse stopResponse = response.body();
				Collection<? extends POIStatus> statuses = parseAgencyJSON(context, stopResponse, rts, sourceLabel, newLastUpdateInMs);
				MTLog.i(this, "Found %d schedule statuses.", statuses.size());
				if (!statuses.isEmpty()) {
					HashSet<String> targetUUIDs = new HashSet<>();
					for (POIStatus status : statuses) {
						targetUUIDs.add(status.getTargetUUID());
					}
					StatusProvider.deleteCachedStatus(this, targetUUIDs);
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
				}
			} else {
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", response.code(),
						response.message());
			}
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
		} catch (SocketException se) {
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	@NonNull
	private Collection<? extends POIStatus> parseAgencyJSON(@NonNull Context context,
															@Nullable SudburyTransitApiV2.JStopResponse jStopResponse,
															@NonNull RouteTripStop rts,
															@Nullable String sourceLabel,
															long newLastUpdateInMs) {
		final String localTimeZoneId = AgencyUtils.getRtsAgencyTimeZone(context);
		try {
			ArrayMap<String, Schedule> result = new ArrayMap<>();
			final int destinationNumber = pickRTSDestination(context, jStopResponse, rts);
			if (jStopResponse != null && jStopResponse.stop != null) {
				if (jStopResponse.stop.calls != null) {
					for (SudburyTransitApiV2.JCall jCall : jStopResponse.stop.calls) {
						if (jCall == null || jCall.passingTime == null || jCall.destination == null) {
							continue;
						}
						if (!rts.getRoute().getShortName().equals(jCall.route)) {
							continue; // cannot guess other routes drop-off only trip stops
						}
						SudburyTransitApiV2.JDestination jDestination = jCall.destination;
						if (jDestination.number == null) {
							continue; // can NOT pick right number
						}
						final boolean destinationNoPickup = rts.isNoPickup() == jDestination.number.equals(destinationNumber);
						Date jPassingTimeDate = jCall.passingTime;
						try {
							long t = TimeUtils.timeToTheTensSecondsMillis(jPassingTimeDate.getTime());
							final String targetUUID = getAgencyRouteStopTargetUUID(
									rts.getAuthority(),
									rts.getRoute().getShortName(),
									destinationNoPickup, // "like" trip ID
									rts.getStop().getCode()
							);
							Schedule.Timestamp timestamp = new Schedule.Timestamp(t, localTimeZoneId);
							if (destinationNoPickup) {
								timestamp.setHeadsign(
										Trip.HEADSIGN_TYPE_NO_PICKUP,
										null
								);
							} else {
								try {
									if (jDestination.name != null) {
										String jDestinationName = jDestination.name;
										if (!TextUtils.isEmpty(jDestinationName)) {
											timestamp.setHeadsign(
													Trip.HEADSIGN_TYPE_STRING,
													GreaterSudburyProviderCommons.cleanTripHeadSign(jDestinationName)
											);
										}
									}
								} catch (Exception e) {
									MTLog.w(this, e, "Error while adding destination name %s!", jDestination);
								}
							}
							timestamp.setRealTime(true); // all (1-2) results are supposed to be real-time
							Schedule schedule = result.get(targetUUID);
							if (schedule == null) {
								schedule = new Schedule(
										null,
										targetUUID,
										newLastUpdateInMs,
										getStatusMaxValidityInMs(),
										newLastUpdateInMs,
										PROVIDER_PRECISION_IN_MS,
										false,
										sourceLabel,
										false
								);
							}
							schedule.addTimestampWithoutSort(timestamp);
							result.put(targetUUID, schedule);
						} catch (Exception e) {
							MTLog.w(this, e, "Error while parsing time %s!", jCall.passingTime);// TODO remove ?
						}
					}
				}
			}
			for (Schedule schedule : result.values()) {
				schedule.sortTimestamps();
			}
			return result.values();
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jStopResponse);
			return Collections.emptyList();
		}
	}

	protected int pickRTSDestination(@NonNull Context context, @Nullable SudburyTransitApiV2.JStopResponse jStopResponse, @NonNull RouteTripStop rts) {
		ArrayList<Integer> destinationNumbers = new ArrayList<>();
		ArrayList<String> destinationNames = new ArrayList<>();
		ArrayList<Long> passingTimes = new ArrayList<>();
		if (jStopResponse != null && jStopResponse.stop != null) {
			if (jStopResponse.stop.calls != null) {
				for (SudburyTransitApiV2.JCall jCall : jStopResponse.stop.calls) {
					if (jCall == null || jCall.destination == null || jCall.passingTime == null) {
						continue;
					}
					if (!rts.getRoute().getShortName().equals(jCall.route)) {
						continue;
					}
					SudburyTransitApiV2.JDestination jDestination = jCall.destination;
					if (jDestination.number == null) {
						continue;
					}
					long passingTime = jCall.passingTime.getTime();
					destinationNumbers.add(jDestination.number);
					destinationNames.add(jDestination.name);
					passingTimes.add(passingTime);
				}
			}
		}
		final int length = min(destinationNumbers.size(), passingTimes.size());
		List<Integer> distinctDestinationNumbers = CollectionUtils.removeDuplicatesNN(destinationNumbers);
		List<String> distinctDestinationNames = CollectionUtils.removeDuplicatesNN(destinationNames);
		if (distinctDestinationNumbers.size() == 2 && distinctDestinationNames.size() == 2) { // each direction has it's own head-sign
			for (int i = 1; i < length; i++) { // need to iterate to match number & head-sign
				if (rts.getTrip().getHeading(context).equals(GreaterSudburyProviderCommons.cleanTripHeadSign(destinationNames.get(i)))) {
					return destinationNumbers.get(i);
				}
			}
		}
		if (rts.isNoPickup()) {
			if (distinctDestinationNames.size() == 1) {
				if (rts.getTrip().getHeading(context).equals(
						GreaterSudburyProviderCommons.cleanTripHeadSign(distinctDestinationNames.get(0)))
				) {
					return destinationNumbers.get(0); // only this direction (drop-off only)
				} else {
					return -1; // only other direction
				}
			}
		}
		if (distinctDestinationNumbers.size() == 1) {
			return distinctDestinationNumbers.get(0);
		}
		// Multiple destinations
		for (int i = 2; i < length; i++) {
			long previousPreviousPTime = passingTimes.get(i - 2);
			long previousPTime = passingTimes.get(i - 1);
			long pTime = passingTimes.get(i);
			long diff1 = previousPTime - previousPreviousPTime;
			long diff2 = pTime - previousPTime;
			long minDiff = min(diff1, diff2);
			long maxDiff = max(diff1, diff2);
			float percent = (float) minDiff / (float) maxDiff;
			if (minDiff > 0L && percent < 0.50f) {
				if (rts.isNoPickup()) {
					if (diff1 > diff2) {
						return destinationNumbers.get(i - 1);
					} else {
						return destinationNumbers.get(i);
					}
				} else {
					if (diff1 < diff2) {
						return destinationNumbers.get(i - 1);
					} else {
						return destinationNumbers.get(i);
					}
				}
			}
		}
		if (length == 2) {
			long previousPTime = passingTimes.get(0);
			long pTime = passingTimes.get(1);
			long diff2 = pTime - previousPTime;
			if (diff2 < TimeUnit.MINUTES.toMillis(1L)) {
				if (rts.isNoPickup()) {
					return destinationNumbers.get(0);
				} else {
					return destinationNumbers.get(1);
				}
			}
		}
		return -1;
	}

	@SuppressWarnings("unused")
	protected interface SudburyTransitApiV2 {
		@GET("v2/stops/{stopCode}")
		Call<JStopResponse> stops(@Path("stopCode") String stopCode, @Query("auth_token") String authToken);

		class JStopResponse {
			@Nullable
			@SerializedName("stop")
			JStop stop;

			@NonNull
			@Override
			public String toString() {
				return JStopResponse.class.getSimpleName() + "{" +
						"stop=" + stop +
						'}';
			}
		}

		class JStop {
			@Nullable
			@SerializedName("number")
			Integer number;
			@Nullable
			@SerializedName("name")
			String name;
			@Nullable
			@SerializedName("calls")
			ArrayList<JCall> calls;

			@NonNull
			@Override
			public String toString() {
				return JStop.class.getSimpleName() + "{" +
						"number=" + number +
						", name='" + name + '\'' +
						", calls=" + calls +
						'}';
			}
		}

		class JCall {
			@Nullable
			@SerializedName("route")
			String route;
			@Nullable
			@SerializedName("passing_time")
			Date passingTime;
			@Nullable
			@SerializedName("destination")
			JDestination destination;

			@NonNull
			@Override
			public String toString() {
				return JCall.class.getSimpleName() + "{" +
						"route='" + route + '\'' +
						", passingTime='" + passingTime + '\'' +
						", destination=" + destination +
						'}';
			}
		}

		class JDestination {
			@Nullable
			@SerializedName("number")
			Integer number;
			@Nullable
			@SerializedName("name")
			String name;

			@NonNull
			@Override
			public String toString() {
				return JDestination.class.getSimpleName() + "{" +
						"number=" + number +
						", name='" + name + '\'' +
						'}';
			}
		}
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	@Nullable
	private GreaterSudburyDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private GreaterSudburyDbHelper getDBHelper(@NonNull Context context) {
		if (dbHelper == null) { // initialize
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Exception e) { // fail if locked, will try again later
				MTLog.w(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return GreaterSudburyDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	public GreaterSudburyDbHelper getNewDbHelper(@NonNull Context context) {
		return new GreaterSudburyDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(requireContextCompat());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(requireContextCompat());
	}

	@NonNull
	private SQLiteOpenHelper getDBHelper() {
		return getDBHelper(requireContextCompat());
	}

	@NonNull
	@Override
	public SQLiteDatabase getReadDB() {
		return getDBHelper().getReadableDatabase();
	}

	@NonNull
	@Override
	public SQLiteDatabase getWriteDB() {
		return getDBHelper().getWritableDatabase();
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		Cursor cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
	}

	@Override
	public int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Nullable
	@Override
	public Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	public static class GreaterSudburyDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = GreaterSudburyDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link GreaterSudburyDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "greatersudbury.db";

		private static final String T_MY_BUS_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_MY_BUS_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_MY_BUS_STATUS).build();

		private static final String T_MY_BUS_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MY_BUS_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link GreaterSudburyDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.greater_sudbury_db_version);
			}
			return dbVersion;
		}

		GreaterSudburyDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_MY_BUS_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_MY_BUS_STATUS_SQL_CREATE);
		}
	}
}
