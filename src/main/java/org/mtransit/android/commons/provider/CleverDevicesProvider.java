package org.mtransit.android.commons.provider;

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

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.mtransit.android.commons.provider.agency.AgencyUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.SourceUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

@SuppressLint("Registered")
public class CleverDevicesProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = CleverDevicesProvider.class.getSimpleName();

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
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
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
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.clever_devices_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String statusUrlAndRSNAndStopCode = null;

	/**
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	@NonNull
	private static String getSTATUS_URL_AND_RSN_AND_STOP_CODE(@NonNull Context context) {
		if (statusUrlAndRSNAndStopCode == null) {
			statusUrlAndRSNAndStopCode = context.getResources().getString(R.string.clever_devices_status_url_and_rsn_and_stop_code);
		}
		return statusUrlAndRSNAndStopCode;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadsignCleanRegex = null;

	/**
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REGEX(@NonNull Context context) {
		if (scheduleHeadsignCleanRegex == null) {
			scheduleHeadsignCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.clever_devices_schedule_head_sign_clean_regex));
		}
		return scheduleHeadsignCleanRegex;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadsignCleanReplacement = null;

	/**
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(@NonNull Context context) {
		if (scheduleHeadsignCleanReplacement == null) {
			scheduleHeadsignCleanReplacement = Arrays
					.asList(context.getResources().getStringArray(R.array.clever_devices_schedule_head_sign_clean_replacement));
		}
		return scheduleHeadsignCleanReplacement;
	}

	@Nullable
	private static Boolean scheduleHeadsignToLowerCase = null;

	/**
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	private static boolean isSCHEDULE_HEADSIGN_TO_LOWER_CASE(@NonNull Context context) {
		if (scheduleHeadsignToLowerCase == null) {
			scheduleHeadsignToLowerCase = context.getResources().getBoolean(R.bool.clever_devices_schedule_head_sign_to_lower_case);
		}
		return scheduleHeadsignToLowerCase;
	}

	private static final long CLEVER_DEVICES_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long CLEVER_DEVICES_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long CLEVER_DEVICES_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long CLEVER_DEVICES_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long CLEVER_DEVICES_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return CLEVER_DEVICES_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return CLEVER_DEVICES_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return CLEVER_DEVICES_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return CLEVER_DEVICES_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return CLEVER_DEVICES_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getCachedStatus() > Can't find new schedule w/o schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, uuid);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom Clever Devices tags
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
				}
			}
		}
		return cachedStatus;
	}

	@NonNull
	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return rts.getUUID();
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
		return CleverDevicesDbHelper.T_CLEVER_DEVICES_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule w/o schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static String getRealTimeStatusUrlString(@NonNull Context context, @NonNull RouteTripStop rts) {
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			MTLog.w(LOG_TAG, "Can't create real-time status URL (no stop code) for %s", rts);
			return null;
		}
		return String.format(getSTATUS_URL_AND_RSN_AND_STOP_CODE(context), rts.getRoute().getShortName(), rts.getStop().getCode());
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			final Context context = requireContextCompat();
			String urlString = getRealTimeStatusUrlString(context, rts);
			if (TextUtils.isEmpty(urlString)) {
				return;
			}
			MTLog.i(this, "Loading from '%s'...", urlString);
			String sourceLabel = SourceUtils.getSourceLabel(urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				CleverDevicesPredictionsDataHandler handler = new CleverDevicesPredictionsDataHandler(this, newLastUpdateInMs, AgencyUtils.getRtsAgencyTimeZone(context), sourceLabel, rts);
				xr.setContentHandler(handler);
				xr.parse(new InputSource(httpUrlConnection.getInputStream()));
				Collection<POIStatus> statuses = handler.getStatuses();
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
				MTLog.i(this, "Loaded %d statuses.", statuses.size());
				for (POIStatus status : statuses) {
					StatusProvider.cacheStatusS(this, status);
				}
				return;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
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
	private CleverDevicesDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private CleverDevicesDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return CleverDevicesDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link CleverDevicesProvider} implementations in same app.
	 */
	@NonNull
	public CleverDevicesDbHelper getNewDbHelper(@NonNull Context context) {
		return new CleverDevicesDbHelper(context.getApplicationContext());
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

	private static class CleverDevicesPredictionsDataHandler extends MTDefaultHandler {

		private static final String LOG_TAG = CleverDevicesProvider.LOG_TAG + ">" + CleverDevicesPredictionsDataHandler.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final String STOP = "stop";
		private static final String NO_PREDICTION_MESSAGE = "noPredictionMessage";
		private static final String PRE = "pre";
		private static final String PT = "pt";
		private static final String PU = "pu";
		private static final String FD = "fd";
		private static final String RN = "rn";
		private static final String RD = "rd";
		private static final String V = "v";
		private static final String ZONE = "zone";

		private static final String MINUTES = "MINUTES";
		private static final String DELAYED = "DELAYED"; // 0 minutes (0 minutes in official app)
		private static final String APPROACHING = "APPROACHING"; // 1-0 minutes

		private String currentLocalName = STOP;

		@NonNull
		private final CleverDevicesProvider provider;
		private final long lastUpdateInMs;
		@NonNull
		private final String timeZoneId;
		@Nullable
		private final String sourceLabel;
		@NonNull
		private final RouteTripStop rts;

		@NonNull
		private final StringBuilder currentPt = new StringBuilder();
		@NonNull
		private final StringBuilder currentPu = new StringBuilder();
		@NonNull
		private final StringBuilder currentFd = new StringBuilder();
		@NonNull
		private final ArrayList<Timestamp> currentTimestamps = new ArrayList<>();

		@NonNull
		private final HashSet<POIStatus> statuses = new HashSet<>();

		CleverDevicesPredictionsDataHandler(@NonNull CleverDevicesProvider provider,
											long lastUpdateInMs,
											@NonNull String timeZoneId,
											@Nullable String sourceLabel,
											@NonNull RouteTripStop rts) {
			this.provider = provider;
			this.lastUpdateInMs = lastUpdateInMs;
			this.timeZoneId = timeZoneId;
			this.sourceLabel = sourceLabel;
			this.rts = rts;
		}

		@NonNull
		public Collection<POIStatus> getStatuses() {
			return this.statuses;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (PRE.equals(this.currentLocalName)) {
				this.currentPt.setLength(0); // reset
				this.currentPu.setLength(0); // reset
				this.currentFd.setLength(0); // reset
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			try {
				String string = new String(ch, start, length);
				if (TextUtils.isEmpty(string)) {
					return;
				}
				if (PT.equals(this.currentLocalName)) {
					this.currentPt.append(string);
				} else if (PU.equals(this.currentLocalName)) {
					this.currentPu.append(string);
				} else if (FD.equals(this.currentLocalName)) { //
					this.currentFd.append(string);
				} else if (STOP.equals(this.currentLocalName)) { // ignore
				} else if (PRE.equals(this.currentLocalName)) { // ignore
				} else if (RN.equals(this.currentLocalName)) { // ignore
				} else if (RD.equals(this.currentLocalName)) { // ignore
				} else if (V.equals(this.currentLocalName)) { // ignore
				} else if (ZONE.equals(this.currentLocalName)) { // ignore
				} else if (NO_PREDICTION_MESSAGE.equals(this.currentLocalName)) { // ignore
				} else {
					MTLog.d(this, "characters() > unexpected characters '%s' for '%s'", string.trim(), this.currentLocalName);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (PRE.equals(localName)) {
				int minutes;
				String pu = this.currentPu.toString().trim();
				switch (pu) {
				case APPROACHING:
				case DELAYED:
					minutes = 0;
					break;
				case MINUTES:
					minutes = Integer.parseInt(this.currentPt.toString().trim());
					break;
				default:
					MTLog.d(this, "endElement() > Unexpected PU: %s (skip)", pu);
					return;
				}
				long t = TimeUtils.timeToTheMinuteMillis(this.lastUpdateInMs) + TimeUnit.MINUTES.toMillis(minutes);
				Schedule.Timestamp timestamp = new Schedule.Timestamp(t, this.timeZoneId);
				if (!TextUtils.isEmpty(this.currentFd)) {
					timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(this.provider.requireContextCompat(), this.currentFd.toString().trim(), rts));
				}
				timestamp.setRealTime(true); // all (1) result(s) are(is) real-time ELSE no result
				if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
					timestamp.setAccessible(Accessibility.UNKNOWN); // no info available on website
				}
				this.currentTimestamps.add(timestamp);
			}
			if (STOP.equals(localName)) {
				if (CollectionUtils.getSize(this.currentTimestamps) == 0) {
					MTLog.d(this, "endElement() > No timestamp for %s", this.rts);
					return;
				}
				Schedule newSchedule = new Schedule(
						null,
						getAgencyRouteStopTargetUUID(this.rts),
						this.lastUpdateInMs,
						this.provider.getStatusMaxValidityInMs(),
						this.lastUpdateInMs,
						PROVIDER_PRECISION_IN_MS,
						false,
						this.sourceLabel,
						false
				);
				newSchedule.setTimestampsAndSort(this.currentTimestamps);
				this.statuses.add(newSchedule);
			}
		}

		private String cleanTripHeadsign(@NonNull Context context, String tripHeadsign, RouteTripStop optRTS) {
			try {
				if (isSCHEDULE_HEADSIGN_TO_LOWER_CASE(context)) {
					tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
				}
				for (int c = 0; c < getSCHEDULE_HEADSIGN_CLEAN_REGEX(context).size(); c++) {
					try {
						tripHeadsign = Pattern.compile(getSCHEDULE_HEADSIGN_CLEAN_REGEX(context).get(c), Pattern.CASE_INSENSITIVE)
								.matcher(tripHeadsign).replaceAll(getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(context).get(c));
					} catch (Exception e) {
						MTLog.w(this, e, "Error while cleaning trip head sign %s for %s cleaning configuration!", tripHeadsign, c);
					}
				}
				tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
				if (optRTS != null) {
					String heading = optRTS.getTrip().getHeading(context);
					tripHeadsign = Pattern.compile("((^|\\W)(" + heading + ")(\\W|$))", Pattern.CASE_INSENSITIVE).matcher(tripHeadsign).replaceAll(" ");
				}
				tripHeadsign = CleanUtils.cleanLabel(Locale.ENGLISH, tripHeadsign);
				return tripHeadsign;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
				return tripHeadsign;
			}
		}
	}

	public static class CleverDevicesDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = CleverDevicesDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link CleverDevicesDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "cleverdevices.db";

		static final String T_CLEVER_DEVICES_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_CLEVER_DEVICES_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_CLEVER_DEVICES_STATUS).build();

		private static final String T_CLEVER_DEVICES_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_CLEVER_DEVICES_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CleverDevicesDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.clever_devices_db_version);
			}
			return dbVersion;
		}

		CleverDevicesDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_CLEVER_DEVICES_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_CLEVER_DEVICES_STATUS_SQL_CREATE);
		}
	}
}
