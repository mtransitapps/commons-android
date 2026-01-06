package org.mtransit.android.commons.provider;

import static org.mtransit.android.commons.data.ServiceUpdateKtxKt.makeServiceUpdateNoneList;

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

import com.google.android.gms.common.util.Hex;
import com.google.transit.realtime.GtfsRealtime;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.KeysIds;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SecureStringUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirection;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.ServiceUpdateKtxKt;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.provider.agency.AgencyUtils;
import org.mtransit.android.commons.provider.common.MTContentProvider;
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.gtfs.GTFSRDSProvider;
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage;
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt;
import org.mtransit.android.commons.provider.gtfs.alert.GTFSRTAlertsManager;
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateCleaner;
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProvider;
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract;
import org.mtransit.android.commons.provider.vehiclelocations.GTFSRealTimeVehiclePositionsProvider;
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationDbHelper;
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProvider;
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProviderContract;
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.SourceUtils;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
public class GTFSRealTimeProvider extends MTContentProvider implements
		ServiceUpdateProviderContract,
		VehicleLocationProviderContract {

	private static final String LOG_TAG = GTFSRealTimeProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final String MT_HASH_SECRET_AND_DATE = "MtHashSecretAndDate";

	@NonNull
	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		VehicleLocationProvider.append(URI_MATCHER, authority);
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
	private static String rdsAgencyId = null;

	/**
	 * Override if multiple {@link GTFSRDSProvider} implementations in same app.
	 */
	@NonNull
	private static String getRDS_AGENCY_ID(@NonNull Context context) {
		if (rdsAgencyId == null) {
			rdsAgencyId = context.getResources().getString(R.string.gtfs_rts_agency_id); // do not change to avoid breaking compat w/ old modules
		}
		return rdsAgencyId;
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
	private static String agencyUrlToken = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	public static String getAGENCY_URL_TOKEN(@NonNull Context context) {
		if (agencyUrlToken == null) {
			agencyUrlToken = context.getResources().getString(R.string.gtfs_real_time_agency_url_token);
		}
		return agencyUrlToken;
	}

	@Nullable
	public String providedAgencyUrlToken = null;

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
	private String providedAgencyUrlSecret = null;

	@Nullable
	private static Boolean useURLHashSecretAndDate = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	public static boolean isUSE_URL_HASH_SECRET_AND_DATE(@NonNull Context context) {
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
	private static String agencyServiceAlertsUrlCached = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_SERVICE_ALERTS_URL_CACHED(@NonNull Context context) {
		if (agencyServiceAlertsUrlCached == null) {
			agencyServiceAlertsUrlCached = context.getResources().getString(R.string.gtfs_real_time_agency_service_alerts_url_cached);
		}
		return agencyServiceAlertsUrlCached;
	}

	@Nullable
	private static String agencyVehiclesUrl = null;

	@NonNull
	public static String getAgencyVehiclePositionsUrlString(@NonNull Context context, @NonNull String token) {
		if (agencyVehiclesUrl == null) {
			agencyVehiclesUrl = getAGENCY_VEHICLE_POSITIONS_URL(context,
					token, // 1st (some agency config have only 1 "%s")
					MT_HASH_SECRET_AND_DATE
			);
		}
		return agencyVehiclesUrl;
	}

	@Nullable
	private static String agencyVehiclePositionsUrl = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	@SuppressLint("StringFormatInvalid") // empty string: set in module app
	private static String getAGENCY_VEHICLE_POSITIONS_URL(
			@NonNull Context context,
			@NonNull String token,
			@SuppressWarnings("SameParameterValue") @NonNull String hash
	) {
		if (agencyVehiclePositionsUrl == null) {
			agencyVehiclePositionsUrl = context.getResources().getString(R.string.gtfs_real_time_agency_vehicle_positions_url, token, hash);
		}
		return agencyVehiclePositionsUrl;
	}

	@Nullable
	private static String agencyVehiclePositionsUrlCached = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	public static String getAGENCY_VEHICLE_POSITIONS_URL_CACHED(@NonNull Context context) {
		if (agencyVehiclePositionsUrlCached == null) {
			agencyVehiclePositionsUrlCached = context.getResources().getString(R.string.gtfs_real_time_agency_vehicle_positions_url_cached);
		}
		return agencyVehiclePositionsUrlCached;
	}

	@Nullable
	private static String serviceIdCleanupRegex = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getSERVICE_ID_CLEANUP_REGEX(@NonNull Context context) {
		if (serviceIdCleanupRegex == null) {
			serviceIdCleanupRegex = context.getResources().getString(R.string.gtfs_rts_service_id_cleanup_regex); // do not change to avoid breaking compat w/ old modules
		}
		return serviceIdCleanupRegex;
	}

	@Nullable
	private static String routeIdCleanupRegex = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getROUTE_ID_CLEANUP_REGEX(@NonNull Context context) {
		if (routeIdCleanupRegex == null) {
			routeIdCleanupRegex = context.getResources().getString(R.string.gtfs_rts_route_id_cleanup_regex); // do not change to avoid breaking compat w/ old modules
		}
		return routeIdCleanupRegex;
	}

	@Nullable
	private static String tripIdCleanupRegex = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getTRIP_ID_CLEANUP_REGEX(@NonNull Context context) {
		if (tripIdCleanupRegex == null) {
			tripIdCleanupRegex = context.getResources().getString(R.string.gtfs_rts_trip_id_cleanup_regex);
		}
		return tripIdCleanupRegex;
	}

	@Nullable
	private static String stopIdCleanupRegex = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	@NonNull
	private static String getSTOP_ID_CLEANUP_REGEX(@NonNull Context context) {
		if (stopIdCleanupRegex == null) {
			stopIdCleanupRegex = context.getResources().getString(R.string.gtfs_rts_stop_id_cleanup_regex); // do not change to avoid breaking compat w/ old modules
		}
		return stopIdCleanupRegex;
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

	@Override
	public long getMinDurationBetweenVehicleLocationRefreshInMs(boolean inFocus) {
		return GTFSRealTimeVehiclePositionsProvider.getMinDurationBetweenRefreshInMs(inFocus);
	}

	@Override
	public long getVehicleLocationMaxValidityInMs() {
		return GTFSRealTimeVehiclePositionsProvider.getMaxValidityInMs();
	}

	@Override
	public long getVehicleLocationValidityInMs(boolean inFocus) {
		return GTFSRealTimeVehiclePositionsProvider.getValidityInMs(inFocus);
	}

	@Override
	public void cacheVehicleLocations(@NonNull List<VehicleLocation> newVehicleLocations) {
		VehicleLocationProvider.cacheVehicleLocationsS(this, newVehicleLocations);
	}

	@Override
	public @Nullable List<VehicleLocation> getCachedVehicleLocations(@NonNull VehicleLocationProviderContract.Filter vehicleLocationFilter) {
		return GTFSRealTimeVehiclePositionsProvider.getCached(this, vehicleLocationFilter);
	}

	@Override
	public @Nullable List<VehicleLocation> getNewVehicleLocations(@NonNull VehicleLocationProviderContract.Filter vehicleLocationFilter) {
		this.providedAgencyUrlToken = SecureStringUtils.dec(vehicleLocationFilter.getProvidedEncryptKey(KeysIds.GTFS_REAL_TIME_URL_TOKEN));
		this.providedAgencyUrlSecret = SecureStringUtils.dec(vehicleLocationFilter.getProvidedEncryptKey(KeysIds.GTFS_REAL_TIME_URL_SECRET));
		return GTFSRealTimeVehiclePositionsProvider.getNew(this, vehicleLocationFilter);
	}

	@Override
	public boolean deleteCachedVehicleLocation(int vehicleLocationId) {
		return VehicleLocationProvider.deleteCachedVehicleLocation(this, vehicleLocationId);
	}

	public boolean deleteAllCachedVehicleLocations() {
		return VehicleLocationProvider.deleteAllCachedVehicleLocations(this);
	}

	@Override
	public boolean purgeUselessCachedVehicleLocations() {
		return VehicleLocationProvider.purgeUselessCachedVehicleLocations(this);
	}

	@Override
	public @NonNull String getVehicleLocationDbTableName() {
		return GTFSRealTimeDbHelper.T_GTFS_REAL_TIME_VEHICLE_LOCATION;
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
		if ((serviceUpdateFilter.getPoi() instanceof RouteDirectionStop)) {
			return getCachedServiceUpdates((RouteDirectionStop) serviceUpdateFilter.getPoi());
		} else if ((serviceUpdateFilter.getRouteDirection() != null)) {
			return getCachedServiceUpdates(serviceUpdateFilter.getRouteDirection());
		} else if ((serviceUpdateFilter.getRoute() != null)) {
			return getCachedServiceUpdates(serviceUpdateFilter.getRoute());
		} else {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RDS or no route)");
			return null;
		}
	}

	@NonNull
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull RouteDirectionStop rds) {
		final Context context = requireContextCompat();
		//noinspection UnnecessaryLocalVariable
		final ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(context, getProviderTargetUUIDs(context, rds));
		// if (org.mtransit.commons.Constants.DEBUG) {
		// MTLog.d(this, "getCachedServiceUpdates() > %s service updates for %s.", cachedServiceUpdates.size(), rds.getUUID());
		// for (ServiceUpdate serviceUpdate : cachedServiceUpdates) {
		// MTLog.d(this, "getCachedServiceUpdates() > - %s", serviceUpdate);
		// }
		// }
		return cachedServiceUpdates;
	}

	@NonNull
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull RouteDirection rd) {
		final Context context = requireContextCompat();
		//noinspection UnnecessaryLocalVariable
		final ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(context, getProviderTargetUUIDs(context, rd));
		// if (org.mtransit.commons.Constants.DEBUG) {
		// MTLog.d(this, "getCachedServiceUpdates() > %s service updates for %s.", cachedServiceUpdates.size(), rd.getUUID());
		// for (ServiceUpdate serviceUpdate : cachedServiceUpdates) {
		// MTLog.d(this, "getCachedServiceUpdates() > - %s", serviceUpdate);
		// }
		// }
		return cachedServiceUpdates;
	}

	@NonNull
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull Route route) {
		final Context context = requireContextCompat();
		//noinspection UnnecessaryLocalVariable
		final ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(context, getProviderTargetUUIDs(context, route));
		// if (org.mtransit.commons.Constants.DEBUG) {
		// MTLog.d(this, "getCachedServiceUpdates() > %s service updates for %s.", cachedServiceUpdates.size(), route.getUUID());
		// for (ServiceUpdate serviceUpdate : cachedServiceUpdates) {
		// MTLog.d(this, "getCachedServiceUpdates() > - %s", serviceUpdate);
		// }
		// }
		return cachedServiceUpdates;
	}

	@NonNull
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull Context context,
															 @NonNull Map<String, String> targetUUIDs) {
		final ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();
		CollectionUtils.addAllN(serviceUpdates, ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUIDs.keySet()));
		enhanceServiceUpdate(context, serviceUpdates, targetUUIDs);
		return serviceUpdates;
	}

	@NonNull
	private Map<String, String> getProviderTargetUUIDs(@NonNull Context context, @NonNull RouteDirectionStop rds) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getAgencyTagTargetUUID(getAgencyTag(context)), rds.getAuthority());
		CollectionUtils.putNotNull(targetUUIDs, getAgencyRouteTypeTagTargetUUID(getAgencyTag(context), getRouteTypeTag(rds)), rds.getAuthority());
		targetUUIDs.put(getAgencyRouteTagTargetUUID(getAgencyTag(context), getRouteTag(rds)), rds.getRoute().getUUID());
		CollectionUtils.putNotNull(targetUUIDs,
				getAgencyRouteDirectionTagTargetUUID(getAgencyTag(context), getRouteTag(rds), getDirectionTag(rds)), rds.getRouteDirectionUUID());
		CollectionUtils.putNotNull(targetUUIDs,
				getAgencyRouteDirectionStopTagTargetUUID(getAgencyTag(context), getRouteTag(rds), getDirectionTag(rds), getStopTag(rds)), rds.getUUID());
		targetUUIDs.put(getAgencyStopTagTargetUUID(getAgencyTag(context), getStopTag(rds)), rds.getUUID());
		targetUUIDs.put(getAgencyRouteStopTagTargetUUID(getAgencyTag(context), getRouteTag(rds), getStopTag(rds)), rds.getUUID());
		return targetUUIDs;
	}

	@NonNull
	private Map<String, String> getProviderTargetUUIDs(@NonNull Context context, @NonNull RouteDirection rd) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getAgencyTagTargetUUID(getAgencyTag(context)), rd.getAuthority());
		CollectionUtils.putNotNull(targetUUIDs, getAgencyRouteTypeTagTargetUUID(getAgencyTag(context), getRouteTypeTag(rd)), rd.getAuthority());
		targetUUIDs.put(getAgencyRouteTagTargetUUID(getAgencyTag(context), getRouteTag(rd)), rd.getRoute().getUUID());
		CollectionUtils.putNotNull(targetUUIDs,
				getAgencyRouteDirectionTagTargetUUID(getAgencyTag(context), getRouteTag(rd), getDirectionTag(rd)), rd.getUUID());
		return targetUUIDs;
	}

	@NonNull
	private Map<String, String> getProviderTargetUUIDs(@NonNull Context context, @NonNull Route route) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getAgencyTagTargetUUID(getAgencyTag(context)), route.getAuthority());
		targetUUIDs.put(getAgencyRouteTagTargetUUID(getAgencyTag(context), getRouteTag(route)), route.getUUID());
		return targetUUIDs;
	}

	@NonNull
	public String getAgencyTag(@NonNull Context context) {
		return getRDS_AGENCY_ID(context);
	}

	@NonNull
	private String getRouteTag(@NonNull RouteDirectionStop rds) {
		return getRouteTag(rds.getRoute());
	}

	@NonNull
	private String getRouteTag(@NonNull RouteDirection rd) {
		return getRouteTag(rd.getRoute());
	}

	@NonNull
	public String getRouteTag(@NonNull Route route) {
		return String.valueOf(route.getOriginalIdHash());
	}

	private int getRouteTypeTag(@NonNull RouteDirectionStop rds) {
		Integer originalRouteType = getRouteTypeTag(rds.getRoute());
		return originalRouteType != null ? originalRouteType : rds.getDataSourceTypeId();
	}

	@Nullable
	private Integer getRouteTypeTag(@NonNull RouteDirection routeDirection) {
		return getRouteTypeTag(routeDirection.getRoute());
	}

	@Nullable
	private Integer getRouteTypeTag(@NonNull Route route) {
		return route.getType();
	}

	@Nullable
	private Integer getDirectionTag(@NonNull RouteDirectionStop rds) {
		return getDirectionTag(rds.getDirection());
	}

	@Nullable
	private Integer getDirectionTag(@NonNull RouteDirection rd) {
		return getDirectionTag(rd.getDirection());
	}

	@Nullable
	public Integer getDirectionTag(@NonNull Direction direction) {
		return direction.getOriginalDirectionIdOrNull();
	}

	@NonNull
	private String getStopTag(@NonNull RouteDirectionStop rds) {
		return getStopTag(rds.getStop());
	}

	@NonNull
	private String getStopTag(@NonNull Stop stop) {
		return String.valueOf(stop.getOriginalIdHash());
	}

	@NonNull
	protected static String getAgencyStopTagTargetUUID(@NonNull String agencyTag, @NonNull String stopTag) {
		return POI.POIUtils.getUUID(agencyTag, "si" + stopTag);
	}

	@NonNull
	public static String getAgencyRouteTagTargetUUID(@NonNull String agencyTag, @NonNull String routeTag) {
		return POI.POIUtils.getUUID(agencyTag, "ri" + routeTag);
	}

	@NonNull
	protected static String getAgencyRouteStopTagTargetUUID(@NonNull String agencyTag, @NonNull String routeTag, @NonNull String stopTag) {
		return POI.POIUtils.getUUID(agencyTag, "ri" + routeTag, "si" + stopTag);
	}

	@Nullable
	protected static String getAgencyRouteTypeTagTargetUUID(@NonNull String agencyTag, @Nullable Integer routeType) {
		if (routeType == null) return null;
		return POI.POIUtils.getUUID(agencyTag, "t" + routeType);
	}

	@Nullable
	public static String getAgencyRouteDirectionTagTargetUUID(@NonNull String agencyTag, @NonNull String routeTag, @Nullable Integer directionTag) {
		if (directionTag == null) return null;
		return POI.POIUtils.getUUID(agencyTag, "ri" + routeTag, "d" + directionTag);
	}

	@Nullable
	protected static String getAgencyRouteDirectionStopTagTargetUUID(@NonNull String agencyTag, @NonNull String routeTag, @Nullable Integer directionTag, @NonNull String stopTag) {
		if (directionTag == null) return null;
		return POI.POIUtils.getUUID(agencyTag, "ri" + routeTag, "d" + directionTag, "si" + stopTag);
	}

	@NonNull
	public static String getAgencyTagTargetUUID(@NonNull String agencyTag) {
		return POI.POIUtils.getUUID(agencyTag);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		this.providedAgencyUrlToken = SecureStringUtils.dec(serviceUpdateFilter.getProvidedEncryptKey(KeysIds.GTFS_REAL_TIME_URL_TOKEN));
		this.providedAgencyUrlSecret = SecureStringUtils.dec(serviceUpdateFilter.getProvidedEncryptKey(KeysIds.GTFS_REAL_TIME_URL_SECRET));
		if ((serviceUpdateFilter.getPoi() instanceof RouteDirectionStop)) {
			return getNewServiceUpdates((RouteDirectionStop) serviceUpdateFilter.getPoi(), serviceUpdateFilter.isInFocusOrDefault());
		} else if ((serviceUpdateFilter.getRouteDirection() != null)) {
			return getNewServiceUpdates(serviceUpdateFilter.getRouteDirection(), serviceUpdateFilter.isInFocusOrDefault());
		} else if ((serviceUpdateFilter.getRoute() != null)) {
			return getNewServiceUpdates(serviceUpdateFilter.getRoute(), serviceUpdateFilter.isInFocusOrDefault());
		} else {
			MTLog.w(this, "getNewServiceUpdates() > no service update (poi null or not RDS or no route)");
			return null;
		}
	}

	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull RouteDirectionStop rds, boolean inFocus) {
		final Context context = requireContextCompat();
		updateAgencyServiceUpdateDataIfRequired(context, inFocus);
		final String authority = rds.getAuthority();
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(rds);
		return getServiceUpdatesOrNone(context, authority, cachedServiceUpdates);
	}

	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull RouteDirection rd, boolean inFocus) {
		final Context context = requireContextCompat();
		updateAgencyServiceUpdateDataIfRequired(context, inFocus);
		final String authority = rd.getAuthority();
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(rd);
		return getServiceUpdatesOrNone(context, authority, cachedServiceUpdates);
	}

	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull Route route, boolean inFocus) {
		final Context context = requireContextCompat();
		updateAgencyServiceUpdateDataIfRequired(context, inFocus);
		final String authority = route.getAuthority();
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(route);
		return getServiceUpdatesOrNone(context, authority, cachedServiceUpdates);
	}

	private ArrayList<ServiceUpdate> getServiceUpdatesOrNone(Context context, String authority, ArrayList<ServiceUpdate> cachedServiceUpdates) {
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			final String agencyProviderTargetUUID = getAgencyTagTargetUUID(authority);
			cachedServiceUpdates = makeServiceUpdateNoneList(this, agencyProviderTargetUUID, AGENCY_SOURCE_ID);
			enhanceServiceUpdate(context, cachedServiceUpdates, Collections.emptyMap()); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	private void enhanceServiceUpdate(@NonNull Context context,
									  Collection<ServiceUpdate> serviceUpdates,
									  @NonNull Map<String, String> targetUUIDs // different UUID from provider target UUID
	) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					ServiceUpdateKtxKt.syncTargetUUID(serviceUpdate, targetUUIDs);
					serviceUpdate.setTextHTML(enhanceHtmlDateTime(context, serviceUpdate.getTextHTML()));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update for stop!");
		}
	}

	private static final String AGENCY_SOURCE_ID = "gtfs_real_time_service_alerts";

	private void updateAgencyServiceUpdateDataIfRequired(@NonNull Context context, boolean inFocus) {
		final long lastUpdateInMs = GtfsRealTimeStorage.getServiceUpdateLastUpdateMs(context, 0L);
		final Integer lastUpdateCode = getServiceUpdateLastUpdateCode();
		if (lastUpdateCode != null && lastUpdateCode != HttpURLConnection.HTTP_OK) {
			inFocus = true; // force earlier retry if last fetch returned HTTP error
		}
		final long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		final long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(context, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(@NonNull Context context, long lastUpdateInMs, boolean inFocus) {
		if (GtfsRealTimeStorage.getServiceUpdateLastUpdateMs(context, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		//noinspection RedundantIfStatement
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(context, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(@NonNull Context context, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		final ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(context);
		if (newServiceUpdates != null) { // empty is OK
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
		} // else keep whatever we have until max validity reached
	}

	@Nullable
	private static String agencyAlertsUrl = null;

	@NonNull
	private static String getAgencyServiceAlertsUrlString(@NonNull Context context, @NonNull String token) {
		if (agencyAlertsUrl == null) {
			agencyAlertsUrl = getAGENCY_SERVICE_ALERTS_URL(context,
					token, // 1st (some agency config have only 1 "%s")
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

	private OkHttpClient okHttpClient = null;

	@NonNull
	public OkHttpClient getOkHttpClient(@NonNull Context context) {
		if (this.okHttpClient == null) {
			this.okHttpClient = NetworkUtils.makeNewOkHttpClientWithInterceptor(context);
		}
		return this.okHttpClient;
	}

	@Nullable
	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(@NonNull Context context) {
		try {
			final URL url;
			String urlCachedString = getAGENCY_SERVICE_ALERTS_URL_CACHED(context);
			if (urlCachedString.isBlank()) {
				String token = getAGENCY_URL_TOKEN(context); // use local token 1st for new/updated API URL & tokens
				if (token.isBlank()) {
					token = this.providedAgencyUrlToken;
				}
				if (token == null) {
					token = ""; // compat w/ API w/o token
				}
				String urlString = getAgencyServiceAlertsUrlString(context, token);
				if (isUSE_URL_HASH_SECRET_AND_DATE(context)) {
					final String hash = getHashSecretAndDate(context);
					if (hash != null) {
						urlString = urlString.replaceAll(MT_HASH_SECRET_AND_DATE, hash.trim());
					}
				}
				url = new URL(urlString);
				MTLog.i(this, "Loading from '%s'...", url.getHost());
				MTLog.d(this, "Using token '%s' (length: %d)", !token.isEmpty() ? "***" : "(none)", token.length());
			} else {
				url = new URL(urlCachedString);
				MTLog.i(this, "Loading from cached API (length: %d) '***'...", urlCachedString.length());
			}
			final String sourceLabel = SourceUtils.getSourceLabel( // always use source from official API
					getAgencyServiceAlertsUrlString(context, "T")
			);
			final Request urlRequest = new Request.Builder().url(url).build();
			try (Response response = getOkHttpClient(context).newCall(urlRequest).execute()) {
				setServiceUpdateLastUpdateCode(response.code());
				GtfsRealTimeStorage.saveServiceUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis());
				switch (response.code()) {
				case HttpURLConnection.HTTP_OK:
					final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
					final ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();
					try {
						GtfsRealtime.FeedMessage gFeedMessage = GtfsRealtime.FeedMessage.parseFrom(response.body().bytes());
						List<Pair<GtfsRealtime.Alert, String>> alertsWithIdPair = GtfsRealtimeExt.toAlertsWithIdPair(gFeedMessage.getEntityList());
						for (Pair<GtfsRealtime.Alert, String> gAlertAndId : GtfsRealtimeExt.sortAlertsPair(alertsWithIdPair, newLastUpdateInMs)) {
							final GtfsRealtime.Alert gAlert = gAlertAndId.getFirst();
							final String feedEntityId = gAlertAndId.getSecond();
							if (Constants.DEBUG) {
								MTLog.d(this, "loadAgencyServiceUpdateDataFromWWW() > GTFS alert[%s]: %s.", feedEntityId, GtfsRealtimeExt.toStringExt(gAlert));
							}
							HashSet<ServiceUpdate> alertsServiceUpdates = processAlerts(context, sourceLabel, feedEntityId, newLastUpdateInMs, gAlert);
							if (alertsServiceUpdates != null && !alertsServiceUpdates.isEmpty()) {
								serviceUpdates.addAll(alertsServiceUpdates);
							}
						}
					} catch (Exception e) {
						MTLog.w(this, e, "loadAgencyServiceUpdateDataFromWWW() > error while parsing GTFS Real Time data!");
					}
					MTLog.i(this, "Found %d service updates.", serviceUpdates.size());
					if (Constants.DEBUG) {
						for (ServiceUpdate serviceUpdate : serviceUpdates) {
							MTLog.d(this, "loadAgencyServiceUpdateDataFromWWW() > service update: %s.", serviceUpdate);
						}
					}
					return serviceUpdates;
				default:
					MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", response.code(),
							response.message());
					return null;
				}
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
	public String getHashSecretAndDate(@NonNull Context context) {
		try {
			final String secret = this.providedAgencyUrlSecret != null ? this.providedAgencyUrlSecret : getAGENCY_URL_SECRET(context);
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
	private HashSet<ServiceUpdate> processAlerts(@NonNull Context context, @NonNull String sourceLabel, @Nullable String feedEntityId, long newLastUpdateInMs, GtfsRealtime.Alert gAlert) {
		if (gAlert == null) {
			return null;
		}
		java.util.List<GtfsRealtime.EntitySelector> gInformedEntityList = gAlert.getInformedEntityList();
		if (CollectionUtils.getSize(gInformedEntityList) == 0) {
			MTLog.w(this, "processAlerts() > SKIP (no informed entity selectors!) (%s)", GtfsRealtimeExt.toStringExt(gAlert));
			return null;
		}
		if (!GtfsRealtimeExt.isActive(gAlert)) {
			MTLog.d(this, "processAlerts() > SKIP (not in active period): %s.", GtfsRealtimeExt.toStringExt(gAlert));
			return null;
		}
		// TODO use? GtfsRealtime.Alert.Cause gCause = gAlert.getCause();
		GtfsRealtime.Alert.Effect gEffect = gAlert.getEffect();
		HashSet<String> targetUUIDs = new HashSet<>();
		ArrayMap<String, Integer> targetUUIDSeverities = new ArrayMap<>();
		final String providerAgencyId = getRDS_AGENCY_ID(context);
		final String agencyTag = getAgencyTag(context);
		for (GtfsRealtime.EntitySelector gInformedEntity : gInformedEntityList) {
			if (gInformedEntity.hasAgencyId()
					&& !providerAgencyId.isEmpty()
					&& !providerAgencyId.equals(gInformedEntity.getAgencyId())) {
				MTLog.w(this, "processAlerts() > Alert targets another agency: %s", gInformedEntity.getAgencyId());
				continue;
			}
			final String targetUUID = parseProviderTargetUUID(context, agencyTag, gInformedEntity);
			if (targetUUID == null || targetUUID.isEmpty()) {
				continue;
			}
			targetUUIDs.add(targetUUID);
			final int severity = GTFSRTAlertsManager.parseSeverity(gInformedEntity, gEffect);
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
		setServiceUpdateLanguages(languages);
		HashSet<ServiceUpdate> serviceUpdates = new HashSet<>();
		long serviceUpdateMaxValidityInMs = getServiceUpdateMaxValidityInMs();
		for (String targetUUID : targetUUIDs) {
			Integer severity = targetUUIDSeverities.get(targetUUID);
			for (String language : languages) {
				ServiceUpdate newServiceUpdate =
						generateNewServiceUpdate(
								context,
								sourceLabel,
								feedEntityId,
								newLastUpdateInMs,
								headerTexts,
								descriptionTexts,
								urlTexts,
								serviceUpdateMaxValidityInMs,
								targetUUID,
								severity == null ? ServiceUpdate.SEVERITY_INFO_UNKNOWN : severity,
								language
						);
				serviceUpdates.add(newServiceUpdate);
			}
		}
		return serviceUpdates;
	}

	@Nullable
	private static Cleaner boldWords = null;

	@Nullable
	private static Cleaner getBoldWords(@NonNull Context context) {
		if (boldWords == null) {
			try {
				if (!getAGENCY_BOLD_WORDS(context).isEmpty()) {
					boldWords = new Cleaner(getAGENCY_BOLD_WORDS(context), true);
				}
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while compiling bold words regex!");
				boldWords = null;
			}
		}
		return boldWords;
	}

	@NonNull
	private static final ArrayMap<String, Cleaner> extraBoldWords = new ArrayMap<>();

	@Nullable
	private static Cleaner getExtraBoldWords(@NonNull Context context, String language) {
		if (!extraBoldWords.containsKey(language)) {
			try {
				final int index = getAGENCY_EXTRA_LANGUAGES(context).indexOf(language);
				if (index >= 0) {
					if (index < getAGENCY_EXTRA_BOLD_WORDS(context).size()) {
						final String regex = getAGENCY_EXTRA_BOLD_WORDS(context).get(index);
						if (!regex.isEmpty()) {
							extraBoldWords.put(language, new Cleaner(regex, true));
						}
					}
				}
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while compiling extra bold words regex for language '%s'!", language);
				extraBoldWords.remove(language);
			}
		}
		return extraBoldWords.get(language);
	}

	@Nullable
	private static Cleaner getBoldWords(@NonNull Context context, String language) {
		if (DEFAULT_LANGUAGE.equals(language)) {
			return getBoldWords(context); // EN = default
		}
		return getExtraBoldWords(context, language); // FR...
	}

	@NonNull
	private ServiceUpdate generateNewServiceUpdate(
			@NonNull Context context,
			@NonNull String sourceLabel,
			@Nullable String feedEntityId,
			long newLastUpdateInMs,
			ArrayMap<String, String> headerTexts,
			ArrayMap<String, String> descriptionTexts,
			ArrayMap<String, String> urlTexts,
			long serviceUpdateMaxValidityInMs,
			String targetUUID,
			int severity,
			String language
	) {
		final String header = headerTexts.get(language);
		final String description = descriptionTexts.get(language);
		final String url = urlTexts.get(language);
		final Cleaner boldWords = getBoldWords(context, language);
		final String replacement = ServiceUpdateCleaner.getReplacement(severity);
		String textHtml = description;
		if (textHtml != null) {
			textHtml = HtmlUtils.toHTML(description);
			textHtml = HtmlUtils.fixTextViewBR(textHtml);
			textHtml = ServiceUpdateCleaner.clean(textHtml, replacement, language);
			textHtml = enhanceHtml(textHtml, boldWords, replacement);
		}
		return new ServiceUpdate(
				null,
				targetUUID,
				newLastUpdateInMs,
				serviceUpdateMaxValidityInMs,
				ServiceUpdateCleaner.makeText(header, description),
				ServiceUpdateCleaner.makeTextHTML(header, textHtml, url),
				severity,
				AGENCY_SOURCE_ID,
				sourceLabel,
				feedEntityId,
				language
		);
	}

	@Nullable
	private String enhanceHtml(@Nullable String originalHtml, @Nullable Cleaner boldWords, @Nullable String replacement) {
		if (originalHtml == null || originalHtml.isEmpty()) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlBold(html, boldWords, replacement);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML (using original)!");
			return originalHtml;
		}
	}

	private String enhanceHtmlBold(String html, @Nullable Cleaner regex, @Nullable String replacement) {
		if (regex == null || html.isEmpty() || replacement == null) {
			return html;
		}
		try {
			return regex.clean(html, replacement);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while making text bold!");
			return html;
		}
	}

	@Nullable
	private static Cleaner agencyCleanTime = null;

	@Nullable
	private static Cleaner getAgencyCleanTime(@NonNull Context context) {
		if (TextUtils.isEmpty(getAGENCY_TIME_REGEX(context))) {
			return null;
		}
		if (agencyCleanTime == null) {
			agencyCleanTime = Cleaner.compile(getAGENCY_TIME_REGEX(context));
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
				String formatter = getAGENCY_TIME_HOUR_FORMAT(context) + COLON + getAGENCY_TIME_MINUTE_FORMAT(context);
				if (!TextUtils.isEmpty(getAGENCY_TIME_AM_PM_FORMAT(context))) {
					formatter += StringUtils.SPACE_STRING + getAGENCY_TIME_AM_PM_FORMAT(context);
				}
				timeParser = new ThreadSafeDateFormatter(formatter, Locale.ENGLISH);
				String agencyTimeZone = getAGENCY_TIME_ZONE(context);
				if (TextUtils.isEmpty(agencyTimeZone)) {
					agencyTimeZone = AgencyUtils.getRDSAgencyTimeZone(context);
				}
				if (!TextUtils.isEmpty(agencyTimeZone)) {
					timeParser.setTimeZone(TimeZone.getTimeZone(agencyTimeZone));
				}
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while initializing time formatter!");
				timeParser = null;
			}
		}
		return timeParser;
	}

	private String enhanceHtmlDateTime(@NonNull Context context, @Nullable String html) throws ParseException {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		final Cleaner agencyCleanTime = getAgencyCleanTime(context);
		ThreadSafeDateFormatter timeParser = getTimeParser(context);
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
			String fTime = TimeUtils.formatTime(false, context, timeD);
			html = html.replace(time, HtmlUtils.applyBold(fTime));
		}
		return html;
	}

	@NonNull
	private ArrayMap<String, String> parseTranslations(@NonNull GtfsRealtime.TranslatedString gTranslatedString) {
		ArrayMap<String, String> translations = new ArrayMap<>();
		java.util.List<GtfsRealtime.TranslatedString.Translation> gTranslations = GtfsRealtimeExt.filterUseless(gTranslatedString.getTranslationList());
		if (CollectionUtils.getSize(gTranslations) > 0) {
			boolean hasEnglishDefault = false;
			for (GtfsRealtime.TranslatedString.Translation gTranslation : gTranslations) {
				final String language = parseLanguage(gTranslation.getLanguage());
				if (language.equals(DEFAULT_LANGUAGE)) {
					hasEnglishDefault = true;
				}
				final String translationText = gTranslation.getText();
				if (translationText == null || translationText.trim().isEmpty()) {
					continue; // SKIP empty text
				}
				if (translations.containsKey(language)) {
					MTLog.w(this, "Language '%s' translation '%s' already provided with '%s'!", language, translationText, translations.get(language));
				}
				translations.put(language, translationText.trim());
			}
			if (!hasEnglishDefault) {
				translations.put(DEFAULT_LANGUAGE, translations.valueAt(0));
			}
		}
		return translations;
	}

	@Nullable
	private Pattern serviceIdCleanupPattern = null;

	private boolean serviceIdCleanupPatternSet = false;

	@SuppressWarnings("unused") // TODO use later for trip_updates, vehicle_location...
	@Nullable
	private Pattern getServiceIdCleanupPattern(@NonNull Context context) {
		if (this.serviceIdCleanupPattern == null && !serviceIdCleanupPatternSet) {
			this.serviceIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getSERVICE_ID_CLEANUP_REGEX(context));
			this.serviceIdCleanupPatternSet = true;
		}
		return this.serviceIdCleanupPattern;
	}

	@Nullable
	private Pattern routeIdCleanupPattern = null;

	private boolean routeIdCleanupPatternSet = false;

	@Nullable
	public Pattern getRouteIdCleanupPattern(@NonNull Context context) {
		if (this.routeIdCleanupPattern == null && !routeIdCleanupPatternSet) {
			this.routeIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getROUTE_ID_CLEANUP_REGEX(context));
			this.routeIdCleanupPatternSet = true;
		}
		return this.routeIdCleanupPattern;
	}

	@Nullable
	private Pattern tripIdCleanupPattern = null;

	private boolean tripIdCleanupPatternSet = false;

	@Nullable
	private Pattern getTripIdCleanupPattern(@NonNull Context context) {
		if (this.tripIdCleanupPattern == null && !tripIdCleanupPatternSet) {
			this.tripIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getTRIP_ID_CLEANUP_REGEX(context));
			this.tripIdCleanupPatternSet = true;
		}
		return this.tripIdCleanupPattern;
	}

	@Nullable
	private Pattern stopIdCleanupPattern = null;

	private boolean stopIdCleanupPatternSet = false;

	@Nullable
	private Pattern getStopIdCleanupPattern(@NonNull Context context) {
		if (this.stopIdCleanupPattern == null && !stopIdCleanupPatternSet) {
			this.stopIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getSTOP_ID_CLEANUP_REGEX(context));
			this.stopIdCleanupPatternSet = true;
		}
		return this.stopIdCleanupPattern;
	}

	@Nullable
	private String parseProviderTargetUUID(@NonNull Context context, String agencyTag, @NonNull GtfsRealtime.EntitySelector gEntitySelector) {
		if (gEntitySelector.hasRouteId()) {
			if (gEntitySelector.hasDirectionId()) {
				if (gEntitySelector.hasStopId()) {
					return getAgencyRouteDirectionStopTagTargetUUID(agencyTag,
							GtfsRealtimeExt.getRouteIdHash(gEntitySelector, getRouteIdCleanupPattern(context)),
							gEntitySelector.getDirectionId(),
							GtfsRealtimeExt.getStopIdHash(gEntitySelector, getStopIdCleanupPattern(context))
					);
				} else { // no stop
					return getAgencyRouteDirectionTagTargetUUID(agencyTag,
							GtfsRealtimeExt.getRouteIdHash(gEntitySelector, getRouteIdCleanupPattern(context)),
							gEntitySelector.getDirectionId()
					);
				}
			} else { // no direction
				if (gEntitySelector.hasStopId()) {
					return getAgencyRouteStopTagTargetUUID(agencyTag,
							GtfsRealtimeExt.getRouteIdHash(gEntitySelector, getRouteIdCleanupPattern(context)),
							GtfsRealtimeExt.getStopIdHash(gEntitySelector, getStopIdCleanupPattern(context)));
				}
			}
			return getAgencyRouteTagTargetUUID(agencyTag,
					GtfsRealtimeExt.getRouteIdHash(gEntitySelector, getRouteIdCleanupPattern(context)));
		} else if (gEntitySelector.hasStopId()) {
			return getAgencyStopTagTargetUUID(agencyTag,
					GtfsRealtimeExt.getStopIdHash(gEntitySelector, getStopIdCleanupPattern(context)));
		} else if (gEntitySelector.hasRouteType()) {
			return getAgencyRouteTypeTagTargetUUID(agencyTag,
					gEntitySelector.getRouteType());
		} else if (gEntitySelector.hasAgencyId()) {
			return getAgencyTagTargetUUID(agencyTag);
		} else if (gEntitySelector.hasTrip()) {
			final String tripIdHash = GtfsRealtimeExt.getTripIdHash(gEntitySelector, getTripIdCleanupPattern(context));
			MTLog.w(this, "parseTargetUUID() > unsupported TRIP entity selector: %s (%s) (IGNORED)",
					GtfsRealtimeExt.toStringExt(gEntitySelector.getTrip()),
					tripIdHash
			);
			return null;
		}
		MTLog.w(this, "parseTargetUUID() > unexpected entity selector: %s (IGNORED)", GtfsRealtimeExt.toStringExt(gEntitySelector));
		return null;
	}

	@NonNull
	private String parseLanguage(@Nullable String gLanguage) {
		if (gLanguage == null || gLanguage.isEmpty()) {
			return DEFAULT_LANGUAGE;
		}
		final String providedLanguage = Locale.forLanguageTag(gLanguage).getLanguage();
		if (providedLanguage.isEmpty()) {
			return DEFAULT_LANGUAGE;
		}
		return providedLanguage;
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

	private static final String DEFAULT_LANGUAGE = Locale.ENGLISH.getLanguage();

	@Nullable
	private static String serviceUpdateLanguage = null;

	@NonNull
	@Override
	public String getServiceUpdateLanguage() {
		if (serviceUpdateLanguage == null) {
			final Set<String> serviceUpdateLanguages = getServiceUpdateLanguages();
			if (serviceUpdateLanguages == null || serviceUpdateLanguages.isEmpty()) {
				return DEFAULT_LANGUAGE; // we will know later
			}
			if (serviceUpdateLanguages.contains(Locale.getDefault().getLanguage())) {
				serviceUpdateLanguage = Locale.getDefault().getLanguage();
			} else {
				serviceUpdateLanguage = DEFAULT_LANGUAGE;
			}
		}
		return serviceUpdateLanguage;
	}

	@Nullable
	private static Set<String> serviceUpdateLanguages = null;

	private void setServiceUpdateLanguages(@NonNull Set<String> languages) {
		serviceUpdateLanguages = languages;
		GtfsRealTimeStorage.saveServiceUpdateLanguages(requireContextCompat(), serviceUpdateLanguages);
	}

	@Nullable
	private Set<String> getServiceUpdateLanguages() {
		if (serviceUpdateLanguages == null) {
			serviceUpdateLanguages = GtfsRealTimeStorage.getServiceUpdateLanguages(requireContextCompat(), null);
		}
		return serviceUpdateLanguages;
	}

	@Nullable
	private static Integer serviceUpdateLastUpdateCode = null;

	private void setServiceUpdateLastUpdateCode(@NonNull Integer code) {
		if (Objects.equals(serviceUpdateLastUpdateCode, code)) return;
		serviceUpdateLastUpdateCode = code;
		GtfsRealTimeStorage.saveServiceUpdateLastUpdateCode(requireContextCompat(), serviceUpdateLastUpdateCode);
	}

	@Nullable
	private Integer getServiceUpdateLastUpdateCode() {
		if (serviceUpdateLastUpdateCode == null) {
			final int code = GtfsRealTimeStorage.getServiceUpdateLastUpdateCode(requireContextCompat(), -1);
			serviceUpdateLastUpdateCode = code == -1 ? null : code;
		}
		return serviceUpdateLastUpdateCode;
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

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
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
		return GTFSRealTimeDbHelper.getDbVersion(requireContextCompat());
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
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		cursor = VehicleLocationProvider.queryS(this, uri, selection);
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
		type = VehicleLocationProvider.getTypeS(this, uri);
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

	public static class GTFSRealTimeDbHelper extends MTSQLiteOpenHelper { // will store statuses & vehicle location...

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

		static final String T_GTFS_REAL_TIME_VEHICLE_LOCATION = VehicleLocationDbHelper.T_VEHICLE_LOCATION;

		private static final String T_GTFS_REAL_TIME_VEHICLE_LOCATION_SQL_CREATE = VehicleLocationDbHelper.getSqlCreateBuilder(
				T_GTFS_REAL_TIME_VEHICLE_LOCATION).build();

		private static final String T_GTFS_REAL_TIME_VEHICLE_LOCATION_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_GTFS_REAL_TIME_VEHICLE_LOCATION);

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
				dbVersion++; // add "service_update.original_id" column
				dbVersion++; // add "vehicle_location" table
				dbVersion++; // add "vehicle_location.report_timestamp" column
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
			db.execSQL(T_GTFS_REAL_TIME_VEHICLE_LOCATION_SQL_DROP);
			db.execSQL(T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_DROP);
			GtfsRealTimeStorage.saveServiceUpdateLastUpdateMs(context, 0L);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_GTFS_REAL_TIME_VEHICLE_LOCATION_SQL_CREATE);
			db.execSQL(T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
