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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.android.gms.common.util.Hex;
import com.google.transit.realtime.GtfsRealtime;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("Registered")
public class GTFSRealTimeProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String LOG_TAG = GTFSRealTimeProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String MT_HASH_SECRET_AND_DATE = "MtHashSecretAndDate";

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS = GTFSRealTimeDbHelper.PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS;

	@NonNull
	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
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
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.gtfs_real_time_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String agencyId = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_ID(@NonNull Context context) {
		if (agencyId == null) {
			agencyId = context.getResources().getString(R.string.gtfs_real_time_agency_id);
		}
		return agencyId;
	}

	@Nullable
	private static String agencyBoldWords = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_BOLD_WORDS(@NonNull Context context) {
		if (agencyBoldWords == null) {
			agencyBoldWords = context.getResources().getString(R.string.gtfs_real_time_agency_bold_words);
		}
		return agencyBoldWords;
	}

	@Nullable
	private static java.util.List<String> agencyExtraLanguages = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getAGENCY_EXTRA_LANGUAGES(@NonNull Context context) {
		if (agencyExtraLanguages == null) {
			agencyExtraLanguages = Arrays.asList(context.getResources().getStringArray(R.array.gtfs_real_time_agency_extra_languages));
		}
		return agencyExtraLanguages;
	}

	@Nullable
	private static java.util.List<String> agencyExtraBoldWords = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getAGENCY_EXTRA_BOLD_WORDS(@NonNull Context context) {
		if (agencyExtraBoldWords == null) {
			agencyExtraBoldWords = Arrays.asList(context.getResources().getStringArray(R.array.gtfs_real_time_agency_extra_bold_words));
		}
		return agencyExtraBoldWords;
	}

	@Nullable
	private static Boolean agencyStopIdIsStopCode = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static boolean isAGENCY_STOP_ID_IS_STOP_CODE(@NonNull Context context) {
		if (agencyStopIdIsStopCode == null) {
			agencyStopIdIsStopCode = context.getResources().getBoolean(R.bool.gtfs_real_time_stop_id_is_stop_code);
		}
		return agencyStopIdIsStopCode;
	}

	@Nullable
	private static String agencyUrlToken = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_URL_TOKEN(@NonNull Context context) {
		if (agencyUrlToken == null) {
			agencyUrlToken = context.getResources().getString(R.string.gtfs_real_time_agency_url_token);
		}
		return agencyUrlToken;
	}

	@Nullable
	private static String agencyUrlSecret = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_URL_SECRET(@NonNull Context context) {
		if (agencyUrlSecret == null) {
			agencyUrlSecret = context.getResources().getString(R.string.gtfs_real_time_agency_url_secret);
		}
		return agencyUrlSecret;
	}

	@Nullable
	private static Boolean useURLHashSecretAndDate = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static boolean isUSE_URL_HASH_SECRET_AND_DATE(@NonNull Context context) {
		if (useURLHashSecretAndDate == null) {
			useURLHashSecretAndDate = context.getResources().getBoolean(R.bool.gtfs_real_time_url_use_hash_secret_and_date);
		}
		return useURLHashSecretAndDate;
	}

	@Nullable
	private static String agencyServiceAlertsUrl = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	@SuppressLint("StringFormatInvalid") // empty string: set in module app
	private static String getAGENCY_SERVICE_ALERTS_URL(@NonNull Context context,
													   @NonNull String token,
													   @SuppressWarnings("SameParameterValue") @NonNull String hash) {
		if (agencyServiceAlertsUrl == null) {
			agencyServiceAlertsUrl = context.getResources().getString(R.string.gtfs_real_time_agency_service_alerts_url, token, hash);
		}
		return agencyServiceAlertsUrl;
	}

	@Nullable
	private static String agencyTimeRegex = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_TIME_REGEX(@NonNull Context context) {
		if (agencyTimeRegex == null) {
			agencyTimeRegex = context.getResources().getString(R.string.gtfs_real_time_agency_time_regex);
		}
		return agencyTimeRegex;
	}

	@Nullable
	private static String agencyTimeHourFormat = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_TIME_HOUR_FORMAT(@NonNull Context context) {
		if (agencyTimeHourFormat == null) {
			agencyTimeHourFormat = context.getResources().getString(R.string.gtfs_real_time_agency_time_hour_format);
		}
		return agencyTimeHourFormat;
	}

	@Nullable
	private static String agencyTimeMinuteFormat = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_TIME_MINUTE_FORMAT(@NonNull Context context) {
		if (agencyTimeMinuteFormat == null) {
			agencyTimeMinuteFormat = context.getResources().getString(R.string.gtfs_real_time_agency_time_minute_format);
		}
		return agencyTimeMinuteFormat;
	}

	@Nullable
	private static String agencyTimeAmPmFormat = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_AM_PM_FORMAT(@NonNull Context context) {
		if (agencyTimeAmPmFormat == null) {
			agencyTimeAmPmFormat = context.getResources().getString(R.string.gtfs_real_time_agency_time_ampm_format);
		}
		return agencyTimeAmPmFormat;
	}

	@Nullable
	private static String agencyTimeZone = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_ZONE(@NonNull Context context) {
		if (agencyTimeZone == null) {
			agencyTimeZone = context.getResources().getString(R.string.gtfs_real_time_agency_time_zone);
		}
		return agencyTimeZone;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@NonNull
	@Override
	public String getServiceUpdateDbTableName() {
		return GTFSRealTimeDbHelper.T_GTFS_REAL_TIME_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(@NonNull ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		HashSet<String> targetUUIDs = getTargetUUIDs(rts);
		for (String targetUUID : targetUUIDs) {
			Collection<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUID);
			serviceUpdates.addAll(cachedServiceUpdates);
		}
		enhanceRTServiceUpdateForStop(serviceUpdates, rts);
		return serviceUpdates;
	}

	@NonNull
	private HashSet<String> getTargetUUIDs(@NonNull RouteTripStop rts) {
		HashSet<String> targetUUIDs = new HashSet<>();
		targetUUIDs.add(getAgencyTargetUUID(getAgencyTag()));
		targetUUIDs.add(getAgencyRouteTypeTargetUUID(getAgencyTag(), rts.getDataSourceTypeId()));
		targetUUIDs.add(getAgencyRouteTagTargetUUID(getAgencyTag(), getRouteId(rts)));
		targetUUIDs.add(getAgencyStopTagTargetUUID(getAgencyTag(), getStopId(rts)));
		targetUUIDs.add(getAgencyRouteStopTagTargetUUID(getAgencyTag(), getRouteId(rts), getStopId(rts)));
		return targetUUIDs;
	}

	@NonNull
	public String getAgencyTag() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAGENCY_ID(getContext());
	}

	@NonNull
	public String getRouteId(@NonNull RouteTripStop rts) {
		// TODO ? ability to use route short name (String vs int for route ID)
		return String.valueOf(rts.getRoute().getId());
	}

	@NonNull
	public String getStopId(@NonNull RouteTripStop rts) {
		if (getContext() != null
				&& isAGENCY_STOP_ID_IS_STOP_CODE(getContext())) {
			return rts.getStop().getCode();
		}
		return String.valueOf(rts.getStop().getId());
	}

	@NonNull
	protected static String getAgencyStopTagTargetUUID(@NonNull String agencyTag, @NonNull String stopTag) {
		return POI.POIUtils.getUUID(agencyTag, stopTag);
	}

	@NonNull
	protected static String getAgencyRouteTagTargetUUID(@NonNull String agencyTag, @NonNull String routeTag) {
		return POI.POIUtils.getUUID(agencyTag, routeTag);
	}

	@NonNull
	protected static String getAgencyRouteStopTagTargetUUID(@NonNull String agencyTag, @NonNull String routeTag, @NonNull String stopTag) {
		return POI.POIUtils.getUUID(agencyTag, routeTag, stopTag);
	}

	@NonNull
	protected static String getAgencyRouteTypeTargetUUID(@NonNull String agencyTag, int routeType) {
		return POI.POIUtils.getUUID(agencyTag, routeType);
	}

	@NonNull
	protected static String getAgencyTargetUUID(@NonNull String agencyTag) {
		return POI.POIUtils.getUUID(agencyTag);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(serviceUpdateFilter.isInFocusOrDefault());
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(serviceUpdateFilter);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(rts.getAuthority());
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(Collection<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
					serviceUpdate.setTextHTML(enhanceHtmlDateTime(serviceUpdate.getTextHTML()));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	@NonNull
	public ServiceUpdate getServiceUpdateNone(@NonNull String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, getServiceUpdateLanguage());
	}

	private static final String AGENCY_SOURCE_ID = "gtfs_real_time_service_alerts";

	private static final String AGENCY_SOURCE_LABEL = "GTFS-RealTime";

	private void updateAgencyServiceUpdateDataIfRequired(boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW();
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	@Nullable
	private static String agencyAlertsUrl = null;

	@NonNull
	private static String getAgencyServiceAlertsUrlString(@NonNull Context context) {
		if (agencyAlertsUrl == null) {
			agencyAlertsUrl = getAGENCY_SERVICE_ALERTS_URL(context,
					getAGENCY_URL_TOKEN(context), // 1st (some agency config have only 1 "%s"
					MT_HASH_SECRET_AND_DATE
			);
		}
		return agencyAlertsUrl;
	}

	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

	private static final ThreadSafeDateFormatter HASH_DATE_FORMATTER;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("yyyyMMdd'T'HHmm'Z'", Locale.ENGLISH);
		dateFormatter.setTimeZone(UTC_TZ);
		HASH_DATE_FORMATTER = dateFormatter;
	}

	@Nullable
	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW() {
		try {
			Context context = getContext();
			if (context == null) {
				return null;
			}
			String urlString = getAgencyServiceAlertsUrlString(context);
			if (isUSE_URL_HASH_SECRET_AND_DATE(context)) {
				final String hash = getHashSecretAndDate(context);
				if (hash != null) {
					urlString = urlString.replaceAll(MT_HASH_SECRET_AND_DATE, hash.trim());
				}
			}
			URL url = new URL(urlString);
			MTLog.i(this, "Loading from '%s'...", url.getHost());
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();
				try {
					GtfsRealtime.FeedMessage gFeedMessage = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
					for (GtfsRealtime.FeedEntity gFeedEntity : gFeedMessage.getEntityList()) {
						if (gFeedEntity.hasAlert()) {
							GtfsRealtime.Alert gAlert = gFeedEntity.getAlert();
							HashSet<ServiceUpdate> alerts = processAlerts(context, newLastUpdateInMs, gAlert);
							if (alerts != null && !alerts.isEmpty()) {
								serviceUpdates.addAll(alerts);
							}
						}
					}
				} catch (Exception e) {
					MTLog.w(this, e, "loadDataFromWWW() > error while parsing GTFS Real Time data!");
				}
				MTLog.i(this, "Found %d service updates.", serviceUpdates.size());
				return serviceUpdates;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				return null;
			}
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
			return null;
		} catch (SocketException se) {
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) { // Unknown error
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	@Nullable
	private String getHashSecretAndDate(@NonNull Context context) {
		try {
			final String secret = getAGENCY_URL_SECRET(context);
			final String date = HASH_DATE_FORMATTER.formatThreadSafe(new Date());
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			final String saltedSecret = secret + date;
			md.update(saltedSecret.getBytes());
			final byte[] digest = md.digest();
			return Hex.bytesToStringUppercase(digest);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to encode hash secret & password!");
			return null;
		}
	}

	@Nullable
	private HashSet<ServiceUpdate> processAlerts(@NonNull Context context, long newLastUpdateInMs, GtfsRealtime.Alert gAlert) {
		if (gAlert == null) {
			return null;
		}
		java.util.List<GtfsRealtime.EntitySelector> gEntitySelectors = gAlert.getInformedEntityList();
		if (CollectionUtils.getSize(gEntitySelectors) == 0) {
			MTLog.w(this, "processAlerts() > no entity selectors!");
			return null;
		}
		if (!isInActivePeriod(gAlert)) {
			MTLog.d(this, "processAlerts() > SKIP (not in active period): %s.", gAlert.getActivePeriodList());
			return null;
		}
		GtfsRealtime.Alert.Cause gCause = gAlert.getCause();
		GtfsRealtime.Alert.Effect gEffect = gAlert.getEffect();
		HashSet<String> targetUUIDs = new HashSet<>();
		ArrayMap<String, Integer> targetUUIDSeverities = new ArrayMap<>();
		String providerAgencyId = getAGENCY_ID(context);
		String agencyTag = getAgencyTag();
		for (GtfsRealtime.EntitySelector gEntitySelector : gEntitySelectors) {
			if (gEntitySelector.hasAgencyId() && !providerAgencyId.equals(gEntitySelector.getAgencyId())) {
				MTLog.w(this, "processAlerts() > Alert targets another agency: %s", gEntitySelector.getAgencyId());
				continue;
			}
			String targetUUID = parseTargetUUID(agencyTag, gEntitySelector);
			if (targetUUID == null || targetUUID.isEmpty()) {
				continue;
			}
			targetUUIDs.add(targetUUID);
			int severity = parseSeverity(gEntitySelector, gCause, gEffect);
			targetUUIDSeverities.put(targetUUID, severity);
		}
		if (CollectionUtils.getSize(targetUUIDs) == 0) {
			MTLog.w(this, "processAlerts() > no target UUIDs!");
			return null;
		}
		ArrayMap<String, String> headerTexts = parseTranslations(gAlert.getHeaderText());
		ArrayMap<String, String> descriptionTexts = parseTranslations(gAlert.getDescriptionText());
		ArrayMap<String, String> urlTexts = parseTranslations(gAlert.getUrl());
		HashSet<String> languages = new HashSet<>();
		languages.addAll(headerTexts.keySet());
		languages.addAll(descriptionTexts.keySet());
		languages.addAll(urlTexts.keySet());
		HashSet<ServiceUpdate> serviceUpdates = new HashSet<>();
		long serviceUpdateMaxValidityInMs = getServiceUpdateMaxValidityInMs();
		for (String targetUUID : targetUUIDs) {
			Integer severity = targetUUIDSeverities.get(targetUUID);
			for (String language : languages) {
				ServiceUpdate newServiceUpdate =
						generateNewServiceUpdate( //
								context,
								newLastUpdateInMs, //
								headerTexts, //
								descriptionTexts, //
								urlTexts, //
								serviceUpdateMaxValidityInMs, //
								targetUUID, //
								severity == null ? ServiceUpdate.SEVERITY_INFO_UNKNOWN : severity, //
								language
						);
				serviceUpdates.add(newServiceUpdate);
			}
		}
		return serviceUpdates;
	}

	private boolean isInActivePeriod(@NonNull GtfsRealtime.Alert gAlert) {
		if (gAlert.getActivePeriodCount() <= 0) {
			return true; // optional (If missing, the alert will be shown as long as it appears in the feed.)
		}
		long nowInSec = TimeUtils.currentTimeSec();
		for (int ap = 0; ap < gAlert.getActivePeriodCount(); ap++) {
			GtfsRealtime.TimeRange activePeriod = gAlert.getActivePeriod(ap);
			if (activePeriod == null) {
				continue;
			}
			boolean afterStart = false;
			boolean beforeEnd = false;
			if (activePeriod.hasStart()) {
				if (activePeriod.getStart() <= nowInSec) {
					afterStart = true;
				}
			} else {
				afterStart = true;
			}
			if (activePeriod.hasEnd()) {
				if (nowInSec <= activePeriod.getEnd()) {
					beforeEnd = true;
				}
			} else {
				beforeEnd = true;
			}
			if (afterStart && beforeEnd) {
				return true; // If multiple ranges are given, the alert will be shown during all of them.
			}
		}
		return false; // if active period provided, must be respected
	}

	@Nullable
	private static Pattern boldWords = null;

	@Nullable
	private static Pattern getBoldWords(@NonNull Context context) {
		if (boldWords == null) {
			try {
				if (!TextUtils.isEmpty(getAGENCY_BOLD_WORDS(context))) {
					boldWords = Pattern.compile(getAGENCY_BOLD_WORDS(context), Pattern.CASE_INSENSITIVE);
				}
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while compiling bold words pattern!");
				boldWords = null;
			}
		}
		return boldWords;
	}

	@NonNull
	private static final ArrayMap<String, Pattern> extraBoldWords = new ArrayMap<>();

	private static Pattern getExtraBoldWords(@NonNull Context context, String language) {
		if (!extraBoldWords.containsKey(language)) {
			try {
				int index = getAGENCY_EXTRA_LANGUAGES(context).indexOf(language);
				if (index >= 0) {
					if (index < getAGENCY_EXTRA_BOLD_WORDS(context).size()) {
						String regex = getAGENCY_EXTRA_BOLD_WORDS(context).get(index);
						if (!TextUtils.isEmpty(regex)) {
							extraBoldWords.put(language, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
						}
					}
				}
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while compiling extra bold words pattern for language '%s'!", language);
				extraBoldWords.remove(language);
			}
		}
		return extraBoldWords.get(language);
	}

	@NonNull
	private ServiceUpdate generateNewServiceUpdate(@NonNull Context context, long newLastUpdateInMs, ArrayMap<String, String> headerTexts, ArrayMap<String, String> descriptionTexts,
												   ArrayMap<String, String> urlTexts, long serviceUpdateMaxValidityInMs, String targetUUID, int severity, String language) {
		StringBuilder textSb = new StringBuilder();
		StringBuilder textHTMLSb = new StringBuilder();
		String header = headerTexts.get(language);
		if (header != null && !header.isEmpty()) {
			textSb.append(header);
			String headerHTML = HtmlUtils.toHTML(header);
			headerHTML = HtmlUtils.applyBold(headerHTML);
			textHTMLSb.append(headerHTML);
		}
		String description = descriptionTexts.get(language);
		if (description != null && !description.isEmpty()) {
			if (textSb.length() > 0) {
				textSb.append(": ");
			}
			textSb.append(description);
			if (textHTMLSb.length() > 0) {
				textHTMLSb.append(HtmlUtils.BR);
			}
			String descriptionHTML = HtmlUtils.toHTML(description);
			descriptionHTML = HtmlUtils.fixTextViewBR(descriptionHTML);
			textHTMLSb.append(descriptionHTML);
		}
		String url = urlTexts.get(language);
		if (!TextUtils.isEmpty(url)) {
			if (textHTMLSb.length() > 0) {
				textHTMLSb.append(HtmlUtils.BR);
			}
			textHTMLSb.append(url);
		}
		Pattern boldWords;
		if (Locale.ENGLISH.getLanguage().equals(language)) {
			boldWords = getBoldWords(context);
		} else {
			boldWords = getExtraBoldWords(context, language);
		}
		return new ServiceUpdate(null, targetUUID, newLastUpdateInMs, serviceUpdateMaxValidityInMs, //
				textSb.toString(), //
				enhanceHtml(textHTMLSb.toString(), boldWords),  //
				severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, language);
	}

	private String enhanceHtml(String originalHtml, @Nullable Pattern boldWords) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlBold(html, boldWords);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML (using original)!");
			return originalHtml;
		}
	}

	private static final String CLEAN_BOLD_REPLACEMENT = HtmlUtils.applyBold("$1");

	private String enhanceHtmlBold(String html, @Nullable Pattern regex) {
		if (regex == null || TextUtils.isEmpty(html)) {
			return html;
		}
		try {
			return regex.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while making text bold!");
			return html;
		}
	}

	@Nullable
	private static Pattern agencyCleanTime = null;

	@Nullable
	private static Pattern getAgencyCleanTime(@NonNull Context context) {
		if (TextUtils.isEmpty(getAGENCY_TIME_REGEX(context))) {
			return null;
		}
		if (agencyCleanTime == null) {
			agencyCleanTime = Pattern.compile(getAGENCY_TIME_REGEX(context));
		}
		return agencyCleanTime;
	}

	@Nullable
	private static ThreadSafeDateFormatter timeParser = null;

	private static final String COLON = ":";

	@Nullable
	private ThreadSafeDateFormatter getTimeParser(@NonNull Context context) {
		if (timeParser == null) {
			try {
				if (TextUtils.isEmpty(getAGENCY_TIME_HOUR_FORMAT(context)) || TextUtils.isEmpty(getAGENCY_TIME_MINUTE_FORMAT(context))) {
					return null;
				}
				String pattern = getAGENCY_TIME_HOUR_FORMAT(context) + COLON + getAGENCY_TIME_MINUTE_FORMAT(context);
				if (!TextUtils.isEmpty(getAGENCY_TIME_AM_PM_FORMAT(context))) {
					pattern += StringUtils.SPACE_STRING + getAGENCY_TIME_AM_PM_FORMAT(context);
				}
				timeParser = new ThreadSafeDateFormatter(pattern, Locale.ENGLISH);
				if (!TextUtils.isEmpty(getAGENCY_TIME_ZONE(context))) {
					timeParser.setTimeZone(TimeZone.getTimeZone(getAGENCY_TIME_ZONE(context)));
				}
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while initializing time formatter!");
				timeParser = null;
			}
		}
		return timeParser;
	}

	private String enhanceHtmlDateTime(String html) throws ParseException {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		if (getContext() == null) {
			return html;
		}
		Pattern agencyCleanTime = getAgencyCleanTime(getContext());
		ThreadSafeDateFormatter timeParser = getTimeParser(getContext());
		if (agencyCleanTime == null || timeParser == null) {
			return html;
		}
		Matcher timeMatcher = agencyCleanTime.matcher(html);
		while (timeMatcher.find()) {
			String time = timeMatcher.group(0);
			if (time == null) {
				return html;
			}
			String hours = timeMatcher.group(1);
			String minutes = timeMatcher.group(2);
			String amPm = null;
			if (timeMatcher.groupCount() > 2) {
				amPm = StringUtils.trim(timeMatcher.group(3));
			}
			String timeToParse = hours + COLON + minutes;
			if (!TextUtils.isEmpty(amPm)) {
				timeToParse += StringUtils.SPACE_STRING + amPm;
			}
			Date timeD = timeParser.parseThreadSafe(timeToParse);
			if (timeD == null) {
				return html;
			}
			String fTime = TimeUtils.formatTime(false, getContext(), timeD);
			html = html.replace(time, HtmlUtils.applyBold(fTime));
		}
		return html;
	}

	@NonNull
	private ArrayMap<String, String> parseTranslations(@NonNull GtfsRealtime.TranslatedString gTranslatedString) {
		ArrayMap<String, String> translations = new ArrayMap<>();
		java.util.List<GtfsRealtime.TranslatedString.Translation> gTranslations = gTranslatedString.getTranslationList();
		if (CollectionUtils.getSize(gTranslations) > 0) {
			int translationsCount = gTranslations.size();
			for (GtfsRealtime.TranslatedString.Translation gTranslation : gTranslations) {
				String language = parseLanguage(translationsCount, gTranslation.getLanguage());
				final String translationText = gTranslation.getText();
				if (translationText == null || translationText.trim().isEmpty()) {
					continue; // SKIP empty text
				}
				if (translations.containsKey(language)) {
					MTLog.w(this, "Language '%s' translation '%s' already provided with '%s'!", language, translationText, translations.get(language));
				}
				translations.put(language, translationText.trim());
			}
		}
		return translations;
	}

	@Nullable
	private String parseTargetUUID(String agencyTag, @NonNull GtfsRealtime.EntitySelector gEntitySelector) {
		if (gEntitySelector.hasRouteId()) {
			if (gEntitySelector.hasStopId()) {
				return getAgencyRouteStopTagTargetUUID(agencyTag, gEntitySelector.getRouteId(), gEntitySelector.getStopId());
			}
			return getAgencyRouteTagTargetUUID(agencyTag, gEntitySelector.getRouteId());
		} else if (gEntitySelector.hasStopId()) {
			return getAgencyStopTagTargetUUID(agencyTag, gEntitySelector.getStopId());
		} else if (gEntitySelector.hasRouteType()) {
			return getAgencyRouteTypeTargetUUID(agencyTag, gEntitySelector.getRouteType());
		} else if (gEntitySelector.hasAgencyId()) {
			return getAgencyTargetUUID(agencyTag);
		} else if (gEntitySelector.hasTrip()) {
			MTLog.w(this, "parseTargetUUID() > unsupported TRIP entity selector: %s (IGNORED)", gEntitySelector.getTrip());
			return null;
		}
		MTLog.w(this, "parseTargetUUID() > unexpected entity selector: %s (IGNORED)", gEntitySelector);
		return null;
	}

	private int parseSeverity(@NonNull GtfsRealtime.EntitySelector gEntitySelector,
							  @SuppressWarnings("unused") GtfsRealtime.Alert.Cause gCause,
							  @SuppressWarnings("unused") GtfsRealtime.Alert.Effect gEffect) {
		if (gEntitySelector.hasStopId()) {
			return ServiceUpdate.SEVERITY_WARNING_POI;
		} else if (gEntitySelector.hasRouteId()) {
			return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
		} else if (gEntitySelector.hasAgencyId()) {
			return ServiceUpdate.SEVERITY_INFO_AGENCY;
		}
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	private String parseLanguage(int translationsCount, String gLanguage) {
		if (TextUtils.isEmpty(gLanguage) || translationsCount == 1) {
			return Locale.ENGLISH.getLanguage();
		}
		String providedLanguage = SupportFactory.get().localeForLanguageTag(gLanguage).getLanguage();
		if (getContext() != null
				&& getAGENCY_EXTRA_LANGUAGES(getContext()).contains(providedLanguage)) {
			return providedLanguage;
		} else {
			return Locale.ENGLISH.getLanguage();
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getWriteDB().delete(getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency service update data!");
		}
		return affectedRows;
	}

	@Nullable
	private static String serviceUpdateLanguage = null;

	@NonNull
	@Override
	public String getServiceUpdateLanguage() {
		if (serviceUpdateLanguage == null) {
			String newServiceUpdateLanguage = Locale.ENGLISH.getLanguage();
			if (LocaleUtils.isFR() && getContext() != null) {
				if (getAGENCY_EXTRA_LANGUAGES(getContext()).contains(Locale.FRENCH.getLanguage())) {
					newServiceUpdateLanguage = Locale.FRENCH.getLanguage();
				}
			}
			serviceUpdateLanguage = newServiceUpdateLanguage;
		}
		return serviceUpdateLanguage;
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull String targetUUID, @NonNull String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
	}

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
	private GTFSRealTimeDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private GTFSRealTimeDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		//noinspection ConstantConditions // TODO requireContext()
		return GTFSRealTimeDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	public GTFSRealTimeDbHelper getNewDbHelper(@NonNull Context context) {
		return new GTFSRealTimeDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions // TODO requireContext()
		return getURIMATCHER(getContext());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITY_URI(getContext());
	}

	@NonNull
	private SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
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
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = ServiceUpdateProvider.getTypeS(this, uri);
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

	public static class GTFSRealTimeDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = GTFSRealTimeDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "gtfsrealtime.db";

		/**
		 * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS = "pGTFSRealTimeServiceAlertsLastUpdate";

		static final String T_GTFS_REAL_TIME_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_GTFS_REAL_TIME_SERVICE_UPDATE).build();

		private static final String T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_GTFS_REAL_TIME_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link GTFSRealTimeDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.gtfs_real_time_db_version);
			}
			return dbVersion;
		}

		private final Context context;

		GTFSRealTimeDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, 0L, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
