package org.mtransit.android.commons.provider;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

@SuppressLint("Registered")
public class OCTranspoProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = OCTranspoProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.oc_transpo_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public static String getTARGET_AUTHORITY(Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.oc_transpo_status_for_poi_authority);
		}
		return targetAuthority;
	}

	private static String appId = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public static String getAPP_ID(Context context) {
		if (appId == null) {
			appId = context.getResources().getString(R.string.oc_transpo_app_id);
		}
		return appId;
	}

	private static String apiKey = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public static String getAPI_KEY(Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.oc_transpo_api_key);
		}
		return apiKey;
	}

	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		String targetUUID = rts.getUUID();
		return StatusProvider.getCachedStatusS(this, targetUUID);
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return StatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@Override
	public String getStatusDbTableName() {
		return OCTranspoDbHelper.T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Override
	public POIStatus getNewStatus(StatusProviderContract.Filter statusFilter) {
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (!rts.isDescentOnly()) {
			loadPredictionsFromWWW(rts);
		}
		return getCachedStatus(statusFilter);
	}

	private static final String GET_NEXT_TRIPS_FOR_STOP_URL = "https://api.octranspo1.com/v1.2/GetNextTripsForStop";

	private static final String URL_POST_PARAM_APP_ID = "appID";
	private static final String URL_POST_PARAM_APP_KEY = "apiKey";
	private static final String URL_POST_PARAM_ROUTE_NUMBER = "routeNo";
	private static final String URL_POST_PARAM_STOP_NUMBER = "stopNo";

	private static String getPostParameters(Context context, RouteTripStop rts) {
		return new StringBuilder() //
				.append(URL_POST_PARAM_APP_ID).append(HtmlUtils.URL_PARAM_EQ).append(getAPP_ID(context)) //
				.append(HtmlUtils.URL_PARAM_AND) //
				.append(URL_POST_PARAM_APP_KEY).append(HtmlUtils.URL_PARAM_EQ).append(getAPI_KEY(context)) //
				.append(HtmlUtils.URL_PARAM_AND) //
				.append(URL_POST_PARAM_ROUTE_NUMBER).append(HtmlUtils.URL_PARAM_EQ).append(rts.getRoute().getId()) //
				.append(HtmlUtils.URL_PARAM_AND) //
				.append(URL_POST_PARAM_STOP_NUMBER).append(HtmlUtils.URL_PARAM_EQ).append(rts.getStop().getId()) //
				.toString();
	}

	private void loadPredictionsFromWWW(RouteTripStop rts) {
		try {
			String urlString = GET_NEXT_TRIPS_FOR_STOP_URL;
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			urlc.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlc;
			try {
				httpsUrlConnection.setDoOutput(true);
				httpsUrlConnection.setChunkedStreamingMode(0);
				OutputStream os = httpsUrlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, FileUtils.UTF_8));
				writer.write(getPostParameters(getContext(), rts));
				writer.flush();
				writer.close();
				os.close();
				httpsUrlConnection.connect();
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				OCTranspoGetNextTripsForStopDataHandler handler = new OCTranspoGetNextTripsForStopDataHandler(this, newLastUpdateInMs, rts);
				xr.setContentHandler(handler);
				xr.parse(new InputSource(httpsUrlConnection.getInputStream()));
				Collection<POIStatus> statuses = handler.getStatuses();
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(rts.getUUID()));
				for (POIStatus status : statuses) {
					StatusProvider.cacheStatusS(this, status);
				}
				return;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while posting query!");
			} finally {
				httpsUrlConnection.disconnect();
			}
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
		} catch (SocketException se) {
			MTLog.w(TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		PackageManagerUtils.removeModuleLauncherIcon(getContext());
	}

	private static OCTranspoDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private OCTranspoDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return OCTranspoDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public OCTranspoDbHelper getNewDbHelper(Context context) {
		return new OCTranspoDbHelper(context.getApplicationContext());
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(getContext());
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
	}

	@Override
	public int deleteMT(Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insertMT(Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	private static class OCTranspoGetNextTripsForStopDataHandler extends MTDefaultHandler {

		private static final String TAG = OCTranspoProvider.TAG + ">" + OCTranspoGetNextTripsForStopDataHandler.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final String SOAP_ENVELOPE = "soap:Envelope";
		private static final String ROUTE_DIRECTION = "RouteDirection";
		private static final String ROUTE_LABEL = "RouteLabel";
		private static final String REQUEST_PROCESSING_TIME = "RequestProcessingTime";
		private static final String ADJUSTED_SCHEDULE_TIME = "AdjustedScheduleTime"; // minutes until departure

		private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

		private String currentLocalName = SOAP_ENVELOPE;
		private OCTranspoProvider provider;
		private long lastUpdateInMs;
		private RouteTripStop rts;

		private HashSet<POIStatus> statuses = new HashSet<POIStatus>();
		private String currentRouteLabel;
		private String currentRequestProcessingTime;
		private HashSet<String> currentAdjustedScheduleTimes = new HashSet<String>();

		private static final String DATE_FORMAT_PATTERN = "yyyyMMddHHmmss";
		private static final String TIME_ZONE = "America/Montreal";
		private static ThreadSafeDateFormatter dateFormat;

		public static ThreadSafeDateFormatter getDateFormat(Context context) {
			if (dateFormat == null) {
				dateFormat = new ThreadSafeDateFormatter(DATE_FORMAT_PATTERN);
				dateFormat.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
			}
			return dateFormat;
		}

		public OCTranspoGetNextTripsForStopDataHandler(OCTranspoProvider provider, long lastUpdateInMs, RouteTripStop rts) {
			this.provider = provider;
			this.lastUpdateInMs = lastUpdateInMs;
			this.rts = rts;
		}

		public Collection<POIStatus> getStatuses() {
			return this.statuses;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (ROUTE_DIRECTION.equals(this.currentLocalName)) {
				this.currentRouteLabel = null; // reset
				this.currentRequestProcessingTime = null; // reset
				this.currentAdjustedScheduleTimes.clear();
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
				if (ROUTE_LABEL.equals(this.currentLocalName)) {
					this.currentRouteLabel = string;
				} else if (REQUEST_PROCESSING_TIME.equals(this.currentLocalName)) {
					this.currentRequestProcessingTime = string;
				} else if (ADJUSTED_SCHEDULE_TIME.equals(this.currentLocalName)) {
					this.currentAdjustedScheduleTimes.add(string);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (ROUTE_DIRECTION.equals(localName)) {
				if (!this.rts.getTrip().getHeadsignValue().equals(this.currentRouteLabel)) {
					return;
				}
				try {
					Schedule schedule = new Schedule(this.rts.getUUID(), this.lastUpdateInMs, this.provider.getStatusMaxValidityInMs(), this.lastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, rts.isDescentOnly());
					long requestProcessingTimeInMs = getDateFormat(this.provider.getContext()).parseThreadSafe(this.currentRequestProcessingTime).getTime();
					requestProcessingTimeInMs = TimeUtils.timeToTheMinuteMillis(requestProcessingTimeInMs);
					for (String adjustedScheduleTime : this.currentAdjustedScheduleTimes) {
						long t = requestProcessingTimeInMs + TimeUnit.MINUTES.toMillis(Long.parseLong(adjustedScheduleTime));
						schedule.addTimestampWithoutSort(new Schedule.Timestamp(t));
					}
					this.statuses.add(schedule);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while adding new schedule!");
				}
			}
		}
	}

	public static class OCTranspoDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = OCTranspoDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link OCTranspoDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "octranspo.db";

		public static final String T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(
				T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS).build();

		private static final String T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_DROP = SqlUtils
				.getSQLDropIfExistsQuery(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link OCTranspoDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.oc_transpo_db_version);
			}
			return dbVersion;
		}

		public OCTranspoDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_CREATE);
		}
	}
}
