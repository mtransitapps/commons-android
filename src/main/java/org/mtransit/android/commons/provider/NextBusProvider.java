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

import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirection;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.ServiceUpdateKtxKt;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.mtransit.android.commons.provider.agency.AgencyUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.NumberUtils;
import org.mtransit.commons.SourceUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// https://retro.umoiq.com/xmlFeedDocs/NextBusXMLFeed.pdf
// https://retro.umoiq.com/service/publicXMLFeed?command=agencyList
// https://retro.umoiq.com/service/publicJSONFeed?command=agencyList
@SuppressLint("Registered")
public class NextBusProvider extends MTContentProvider implements ServiceUpdateProviderContract, StatusProviderContract {

	private static final String LOG_TAG = NextBusProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = NextBusDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
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
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.next_bus_authority);
		}
		return authority;
	}

	@Nullable
	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getTARGET_AUTHORITY(@NonNull Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.next_bus_for_poi_authority);
		}
		return targetAuthority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String agencyTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getAGENCY_TAG(@NonNull Context context) {
		if (agencyTag == null) {
			agencyTag = context.getResources().getString(R.string.next_bus_agency_tag);
		}
		return agencyTag;
	}

	@Nullable
	private static Boolean usingStopCodeAsStopId = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isUSING_STOP_CODE_AS_STOP_ID(@NonNull Context context) {
		if (usingStopCodeAsStopId == null) {
			usingStopCodeAsStopId = context.getResources().getBoolean(R.bool.next_bus_use_stop_code_as_stop_id);
		}
		return usingStopCodeAsStopId;
	}

	@Nullable
	private static Boolean usingStopIdAsStopTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isUSING_STOP_ID_AS_STOP_TAG(@NonNull Context context) {
		if (usingStopIdAsStopTag == null) {
			usingStopIdAsStopTag = context.getResources().getBoolean(R.bool.next_bus_use_stop_id_as_stop_tag);
		}
		return usingStopIdAsStopTag;
	}

	@Nullable
	private static String textLanguageCode = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getTEXT_LANGUAGE_CODE(@NonNull Context context) {
		if (textLanguageCode == null) {
			textLanguageCode = context.getResources().getString(R.string.next_bus_messages_text_language_code);
		}
		return textLanguageCode;
	}

	@Nullable
	private static String textSecondaryLanguageCode = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getTEXT_SECONDARY_LANGUAGE_CODE(@NonNull Context context) {
		if (textSecondaryLanguageCode == null) {
			textSecondaryLanguageCode = context.getResources().getString(R.string.next_bus_messages_text_secondary_language_code);
		}
		return textSecondaryLanguageCode;
	}

	@Nullable
	private static String textBoldWords = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getTEXT_BOLD_WORDS(@NonNull Context context) {
		if (textBoldWords == null) {
			textBoldWords = context.getResources().getString(R.string.next_bus_messages_text_bold_words);
		}
		return textBoldWords;
	}

	@Nullable
	private static String textSecondaryBoldWords = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static String getTEXT_SECONDARY_BOLD_WORDS(@NonNull Context context) {
		if (textSecondaryBoldWords == null) {
			textSecondaryBoldWords = context.getResources().getString(R.string.next_bus_messages_text_secondary_bold_words);
		}
		return textSecondaryBoldWords;
	}

	@Nullable
	private static Boolean appendHeadSignValueToRouteTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG(@NonNull Context context) {
		if (appendHeadSignValueToRouteTag == null) {
			appendHeadSignValueToRouteTag = context.getResources().getBoolean(R.bool.next_bus_route_tag_append_headsign_value);
		}
		return appendHeadSignValueToRouteTag;
	}

	@Nullable
	private static java.util.List<String> routeTagHeadSignValueReplaceFrom = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getROUTE_TAG_HEAD_SIGN_VALUE_REPLACE_FROM(@NonNull Context context) {
		if (routeTagHeadSignValueReplaceFrom == null) {
			routeTagHeadSignValueReplaceFrom = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_route_tag_headsign_values_replace_from));
		}
		return routeTagHeadSignValueReplaceFrom;
	}

	@Nullable
	private static java.util.List<String> routeTagHeadSignValueReplaceTo = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getROUTE_TAG_HEAD_SIGN_VALUE_REPLACE_TO(@NonNull Context context) {
		if (routeTagHeadSignValueReplaceTo == null) {
			routeTagHeadSignValueReplaceTo = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_route_tag_headsign_values_replace_to));
		}
		return routeTagHeadSignValueReplaceTo;
	}

	@Nullable
	private static java.util.List<String> stopTagCleanRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSTOP_TAG_CLEAN_REGEX(@NonNull Context context) {
		if (stopTagCleanRegex == null) {
			stopTagCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_stop_tag_clean_regex));
		}
		return stopTagCleanRegex;
	}

	@Nullable
	private static java.util.List<String> stopTagCleanReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSTOP_TAG_CLEAN_REPLACEMENT(@NonNull Context context) {
		if (stopTagCleanReplacement == null) {
			stopTagCleanReplacement = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_stop_tag_clean_replacement));
		}
		return stopTagCleanReplacement;
	}

	@Nullable
	private static Boolean scheduleHeadSignCleanStreetTypes = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isSCHEDULE_HEAD_SIGN_CLEAN_STREET_TYPES(@NonNull Context context) {
		if (scheduleHeadSignCleanStreetTypes == null) {
			scheduleHeadSignCleanStreetTypes = context.getResources().getBoolean(R.bool.next_bus_schedule_head_sign_clean_street_types);
		}
		return scheduleHeadSignCleanStreetTypes;
	}

	@Nullable
	private static Boolean scheduleHeadSignCleanStreetTypesFrCa = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isSCHEDULE_HEAD_SIGN_CLEAN_STREET_TYPES_FR_CA(@NonNull Context context) {
		if (scheduleHeadSignCleanStreetTypesFrCa == null) {
			scheduleHeadSignCleanStreetTypesFrCa = context.getResources().getBoolean(R.bool.next_bus_schedule_head_sign_clean_street_types_fr_ca);
		}
		return scheduleHeadSignCleanStreetTypesFrCa;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadSignCleanRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_CLEAN_REGEX(@NonNull Context context) {
		if (scheduleHeadSignCleanRegex == null) {
			scheduleHeadSignCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_schedule_head_sign_clean_regex));
		}
		return scheduleHeadSignCleanRegex;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadSignCleanReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_CLEAN_REPLACEMENT(@NonNull Context context) {
		if (scheduleHeadSignCleanReplacement == null) {
			scheduleHeadSignCleanReplacement = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_schedule_head_sign_clean_replacement));
		}
		return scheduleHeadSignCleanReplacement;
	}

	private static int scheduleHeadSignUseDirectionTitle = -1;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static int getSCHEDULE_HEAD_SIGN_USE_DIRECTION_TITLE(@NonNull Context context) {
		if (scheduleHeadSignUseDirectionTitle < 0) {
			scheduleHeadSignUseDirectionTitle = context.getResources().getInteger(R.integer.next_bus_schedule_head_sign_use_direction_title);
		}
		return scheduleHeadSignUseDirectionTitle;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadSignDirectionTitleRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_DIRECTION_TITLE_REGEX(@NonNull Context context) {
		if (scheduleHeadSignDirectionTitleRegex == null) {
			scheduleHeadSignDirectionTitleRegex = Arrays.asList(context.getResources()
					.getStringArray(R.array.next_bus_schedule_head_sign_direction_title_regex));
		}
		return scheduleHeadSignDirectionTitleRegex;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadSignDirectionTitleReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_DIRECTION_TITLE_REPLACEMENT(@NonNull Context context) {
		if (scheduleHeadSignDirectionTitleReplacement == null) {
			scheduleHeadSignDirectionTitleReplacement = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_direction_title_replacement));
		}
		return scheduleHeadSignDirectionTitleReplacement;
	}

	private static int scheduleHeadSignUsePredictionsDirTitleBecauseNoPredictions = -1;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static int getSCHEDULE_HEAD_SIGN_USE_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS(@NonNull Context context) {
		if (scheduleHeadSignUsePredictionsDirTitleBecauseNoPredictions < 0) {
			scheduleHeadSignUsePredictionsDirTitleBecauseNoPredictions = context.getResources().getInteger(
					R.integer.next_bus_schedule_head_sign_use_predictions_dir_title_because_no_predictions);
		}
		return scheduleHeadSignUsePredictionsDirTitleBecauseNoPredictions;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REGEX(@NonNull Context context) {
		if (scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsRegex == null) {
			scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsRegex = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_dir_title_because_no_predictions_regex));
		}
		return scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsRegex;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REPLACEMENT(@NonNull Context context) {
		if (scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsReplacement == null) {
			scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsReplacement = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_dir_title_because_no_predictions_replacement));
		}
		return scheduleHeadSignPredictionsDirTitleBecauseNoPredictionsReplacement;
	}

	private static int scheduleHeadSignUsePredictionsRouteTitle = -1;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static int getSCHEDULE_HEAD_SIGN_USE_PREDICTIONS_ROUTE_TITLE(@NonNull Context context) {
		if (scheduleHeadSignUsePredictionsRouteTitle < 0) {
			scheduleHeadSignUsePredictionsRouteTitle = context.getResources().getInteger(R.integer.next_bus_schedule_head_sign_use_predictions_route_title);
		}
		return scheduleHeadSignUsePredictionsRouteTitle;
	}

	private static java.util.List<String> scheduleHeadSignPredictionsRouteTitleRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_PREDICTIONS_ROUTE_TITLE_REGEX(@NonNull Context context) {
		if (scheduleHeadSignPredictionsRouteTitleRegex == null) {
			scheduleHeadSignPredictionsRouteTitleRegex = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_route_title_regex));
		}
		return scheduleHeadSignPredictionsRouteTitleRegex;
	}

	private static java.util.List<String> scheduleHeadSignPredictionsRouteTitleReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEAD_SIGN_PREDICTIONS_ROUTE_TITLE_REPLACEMENT(@NonNull Context context) {
		if (scheduleHeadSignPredictionsRouteTitleReplacement == null) {
			scheduleHeadSignPredictionsRouteTitleReplacement = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_route_title_replacement));
		}
		return scheduleHeadSignPredictionsRouteTitleReplacement;
	}

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10L);

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);

	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L);

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
		return NextBusDbHelper.T_NEXT_BUS_SERVICE_UPDATE;
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
		} else if ((serviceUpdateFilter.getRouteDirection() != null)) { // depends on agency routeTag: Toronto TTC: YES, Laval STL: NO
			return getCachedServiceUpdates(serviceUpdateFilter.getRouteDirection());
		} else if ((serviceUpdateFilter.getRoute() != null)) { // depends on agency routeTag: Toronto TTC: YES, Laval STL: NO
			return getCachedServiceUpdates(serviceUpdateFilter.getRoute());
		} else {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RDS or no route)");
			return null;
		}
	}

	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull RouteDirectionStop rds) {
		final Map<String, String> targetUUIDs = getServiceUpdateTargetUUIDs(rds);
		ArrayList<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUIDs.keySet());
		enhanceRDServiceUpdateForStop(cachedServiceUpdates, targetUUIDs);
		return cachedServiceUpdates;
	}

	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull RouteDirection rd) {
		final Map<String, String> targetUUIDs = getServiceUpdateTargetUUIDs(rd);
		ArrayList<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUIDs.keySet());
		enhanceRDServiceUpdateForStop(cachedServiceUpdates, targetUUIDs);
		// if (org.mtransit.commons.Constants.DEBUG) {
		// MTLog.d(this, "getCachedServiceUpdates(%s) > %s", rd.getUUID(), cachedServiceUpdates == null ? null : cachedServiceUpdates.size());
		// if (cachedServiceUpdates != null) {
		// for (ServiceUpdate serviceUpdate : cachedServiceUpdates) {
		// MTLog.d(this, "getCachedServiceUpdates() > - %s", serviceUpdate);
		// }
		// }
		// }
		return cachedServiceUpdates;
	}

	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull Route route) {
		final Map<String, String> targetUUIDs = getServiceUpdateTargetUUIDs(route);
		ArrayList<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUIDs.keySet());
		enhanceRDServiceUpdateForStop(cachedServiceUpdates, targetUUIDs);
		// if (org.mtransit.commons.Constants.DEBUG) {
		// MTLog.d(this, "getCachedServiceUpdates(%s) > %s", route.getUUID(), cachedServiceUpdates == null ? null : cachedServiceUpdates.size());
		// if (cachedServiceUpdates != null) {
		// for (ServiceUpdate serviceUpdate : cachedServiceUpdates) {
		// MTLog.d(this, "getCachedServiceUpdates() > - %s", serviceUpdate);
		// }
		// }
		// }
		return cachedServiceUpdates;
	}

	private void enhanceRDServiceUpdateForStop(@Nullable ArrayList<ServiceUpdate> serviceUpdates,
											   @NonNull Map<String, String> targetUUIDs // different UUID from provider target UUID
	) {
		try {
			if (serviceUpdates != null) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					ServiceUpdateKtxKt.syncTargetUUID(serviceUpdate, targetUUIDs);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update for stop!");
		}
	}

	@NonNull
	private Map<String, String> getServiceUpdateTargetUUIDs(@NonNull RouteDirectionStop rds) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getServiceUpdateAgencyTargetUUID(rds.getAuthority()), rds.getAuthority());
		if (!isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG(requireContextCompat())) {
			targetUUIDs.put(getServiceUpdateAgencyRouteTagTargetUUID(rds.getAuthority(), getRouteTag(rds.getRoute(), null)), rds.getRoute().getUUID());
		} else { // STLaval
			targetUUIDs.put(getServiceUpdateAgencyRouteTagTargetUUID(rds.getAuthority(), getRouteTag(rds)), rds.getRouteDirectionUUID());
		}
		targetUUIDs.put(getAgencyRouteStopTagTargetUUID(rds), rds.getUUID());
		return targetUUIDs;
	}

	@NonNull
	private Map<String, String> getServiceUpdateTargetUUIDs(@NonNull RouteDirection rd) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getServiceUpdateAgencyTargetUUID(rd.getAuthority()), rd.getAuthority());
		if (!isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG(requireContextCompat())) {
			targetUUIDs.put(getServiceUpdateAgencyRouteTagTargetUUID(rd.getAuthority(), getRouteTag(rd.getRoute(), null)), rd.getRoute().getUUID());
		} else { // STLaval
			targetUUIDs.put(getServiceUpdateAgencyRouteTagTargetUUID(rd.getAuthority(), getRouteTag(rd)), rd.getUUID());
		}
		return targetUUIDs;
	}

	@NonNull
	private Map<String, String> getServiceUpdateTargetUUIDs(@NonNull Route route) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getServiceUpdateAgencyTargetUUID(route.getAuthority()), route.getAuthority());
		if (!isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG(requireContextCompat())) {
			targetUUIDs.put(getServiceUpdateAgencyRouteTagTargetUUID(route.getAuthority(), getRouteTag(route, null)), route.getUUID());
		} // ELSE // STLaval
		return targetUUIDs;
	}

	private String getAgencyRouteStopTagTargetUUID(@NonNull RouteDirectionStop rds) {
		return getAgencyRouteStopTagTargetUUID(rds.getAuthority(), getRouteTag(rds), getStopTag(rds));
	}

	@NonNull
	private String getRouteTag(@NonNull RouteDirectionStop rds) {
		return getRouteTag(rds.getRoute(), rds.getDirection());
	}

	@NonNull
	private String getRouteTag(@NonNull RouteDirection rd) {
		return getRouteTag(rd.getRoute(), rd.getDirection());
	}

	@NonNull
	private String getRouteTag(@NonNull Route route, @Nullable Direction direction) {
		final StringBuilder sb = new StringBuilder();
		sb.append(route.getShortName());
		final Context context = requireContextCompat();
		if (direction != null && isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG(context)) { // STLaval
			sb.append(geRouteTagDirectionHeadSignValue(context, direction));
		}
		return sb.toString();
	}

	private String geRouteTagDirectionHeadSignValue(@NonNull Context context, @NonNull Direction direction) {
		String deadSingValue = direction.getHeadsignValue();
		for (int i = 0; i < getROUTE_TAG_HEAD_SIGN_VALUE_REPLACE_FROM(context).size(); i++) {
			if (getROUTE_TAG_HEAD_SIGN_VALUE_REPLACE_FROM(context).get(i).equals(deadSingValue)) {
				deadSingValue = getROUTE_TAG_HEAD_SIGN_VALUE_REPLACE_TO(context).get(i);
			}
		}
		return deadSingValue;
	}

	@NonNull
	private String getStopTag(@NonNull RouteDirectionStop rds) {
		return getStopTag(rds.getStop());
	}

	@NonNull
	private String getStopTag(@NonNull Stop stop) {
		if (isUSING_STOP_ID_AS_STOP_TAG(requireContextCompat())) {
			return String.valueOf(stop.getId());
		}
		return stop.getCode();
	}

	@NonNull
	private String getStopId(@NonNull RouteDirectionStop rds) {
		if (isUSING_STOP_CODE_AS_STOP_ID(requireContextCompat())) {
			return rds.getStop().getCode();
		}
		return String.valueOf(rds.getStop().getId());
	}

	@NonNull
	private String cleanStopTag(@NonNull String stopTag) {
		final Context context = requireContextCompat();
		for (int i = 0; i < getSTOP_TAG_CLEAN_REGEX(context).size(); i++) {
			try {
				stopTag = new Cleaner(
						getSTOP_TAG_CLEAN_REGEX(context).get(i),
						getSTOP_TAG_CLEAN_REPLACEMENT(context).get(i),
						true
				).clean(stopTag);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", stopTag, i);
			}
		}
		return stopTag;
	}

	@NonNull
	protected static String getServiceUpdateAgencyRouteTagTargetUUID(@NonNull String agencyAuthority, @NonNull String routeTag) {
		return POI.POIUtils.getUUID(agencyAuthority, routeTag);
	}

	@NonNull
	protected static String getAgencyRouteStopTagTargetUUID(@NonNull String agencyAuthority, @NonNull String routeTag, @NonNull String stopTag) {
		return POI.POIUtils.getUUID(agencyAuthority, routeTag, stopTag);
	}

	@NonNull
	protected static String getServiceUpdateAgencyTargetUUID(@NonNull String agencyAuthority) {
		return POI.POIUtils.getUUID(agencyAuthority);
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

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if ((serviceUpdateFilter.getPoi() instanceof RouteDirectionStop)) {
			return getNewServiceUpdates((RouteDirectionStop) serviceUpdateFilter.getPoi(), serviceUpdateFilter.isInFocusOrDefault());
		} else if ((serviceUpdateFilter.getRouteDirection() != null)) { // depends on agency routeTag: Toronto TTC: YES, Laval STL: NO
			return getNewServiceUpdates(serviceUpdateFilter.getRouteDirection(), serviceUpdateFilter.isInFocusOrDefault());
		} else if ((serviceUpdateFilter.getRoute() != null)) { // depends on agency routeTag: Toronto TTC: YES, Laval STL: NO
			return getNewServiceUpdates(serviceUpdateFilter.getRoute(), serviceUpdateFilter.isInFocusOrDefault());
		} else {
			MTLog.w(this, "getNewServiceUpdates() > no service update (poi null or not RDS or no route)");
			return null;
		}
	}

	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull RouteDirectionStop rds, boolean inFocus) {
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), inFocus);
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(rds);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			cachedServiceUpdates = makeServiceUpdateNoneList(this, getServiceUpdateAgencyTargetUUID(rds.getAuthority()), AGENCY_SOURCE_ID);
			enhanceRDServiceUpdateForStop(cachedServiceUpdates, Collections.emptyMap());
		}
		return cachedServiceUpdates;
	}

	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull RouteDirection rd, boolean inFocus) {
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), inFocus);
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(rd);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			cachedServiceUpdates = makeServiceUpdateNoneList(this, getServiceUpdateAgencyTargetUUID(rd.getAuthority()), AGENCY_SOURCE_ID);
			enhanceRDServiceUpdateForStop(cachedServiceUpdates, Collections.emptyMap());
		}
		return cachedServiceUpdates;
	}

	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull Route route, boolean inFocus) {
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), inFocus);
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(route);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			cachedServiceUpdates = makeServiceUpdateNoneList(this, getServiceUpdateAgencyTargetUUID(route.getAuthority()), AGENCY_SOURCE_ID);
			enhanceRDServiceUpdateForStop(cachedServiceUpdates, Collections.emptyMap());
		}
		return cachedServiceUpdates;
	}

	private static final String AGENCY_SOURCE_ID = "next_bus_com_messages";

	private void updateAgencyServiceUpdateDataIfRequired(@NonNull Context context, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(context, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(@NonNull Context context, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
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
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(context);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs);
		} // else keep whatever we have until max validity reached
	}

	private OkHttpClient okHttpClient = null;

	@NonNull
	private OkHttpClient getOkHttpClient(@NonNull Context context) {
		if (this.okHttpClient == null) {
			this.okHttpClient = NetworkUtils.makeNewOkHttpClientWithInterceptor(context);
		}
		return this.okHttpClient;
	}

	// TODO switch to JSON:
	// private static final String AGENCY_URL_PART_1_BEFORE_AGENCY_TAG = "https://retro.umoiq.com/service/publicJSONFeed?command=messages&a=";
	private static final String AGENCY_URL_PART_1_BEFORE_AGENCY_TAG = "https://retro.umoiq.com/service/publicXMLFeed?command=messages&a=";

	private static String getAgencyUrlString(@NonNull Context context) {
		return AGENCY_URL_PART_1_BEFORE_AGENCY_TAG + //
				getAGENCY_TAG(context) //
				;
	}

	@Nullable
	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(@NonNull Context context) {
		try {
			final String urlString = getAgencyUrlString(context);
			MTLog.i(this, "Loading from '%s'...", urlString);
			final String sourceLabel = SourceUtils.getSourceLabel(AGENCY_URL_PART_1_BEFORE_AGENCY_TAG);
			final Request urlRequest = new Request.Builder().url(urlString).build();
			try (Response response = getOkHttpClient(context).newCall(urlRequest).execute()) {
				switch (response.code()) {
				case HttpURLConnection.HTTP_OK:
					final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
					final SAXParserFactory spf = SAXParserFactory.newInstance();
					final SAXParser sp = spf.newSAXParser();
					final XMLReader xr = sp.getXMLReader();
					final NextBusMessagesDataHandler handler = new NextBusMessagesDataHandler(
							this,
							sourceLabel,
							newLastUpdateInMs,
							getAGENCY_TAG(context),
							getTARGET_AUTHORITY(context),
							getServiceUpdateMaxValidityInMs(),
							getTEXT_LANGUAGE_CODE(context),
							getTEXT_SECONDARY_LANGUAGE_CODE(context),
							getTEXT_BOLD_WORDS(context),
							getTEXT_SECONDARY_BOLD_WORDS(context)
					);
					xr.setContentHandler(handler);
					xr.parse(new InputSource(response.body().byteStream()));
					final ArrayList<ServiceUpdate> serviceUpdates = handler.getServiceUpdates();
					MTLog.i(this, "Found %d service updates.", serviceUpdates.size());
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
			if (LocaleUtils.isFR()) {
				final Context context = requireContextCompat();
				if (getTEXT_LANGUAGE_CODE(context).contains(Locale.FRENCH.getLanguage())
						|| getTEXT_SECONDARY_LANGUAGE_CODE(context).contains(Locale.FRENCH.getLanguage())) {
					newServiceUpdateLanguage = Locale.FRENCH.getLanguage();
				}
			}
			serviceUpdateLanguage = newServiceUpdateLanguage;
		}
		return serviceUpdateLanguage;
	}

	private static final long NEXT_BUS_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final long NEXT_BUS_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final long NEXT_BUS_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long NEXT_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(2L);
	private static final long NEXT_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return NEXT_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return NEXT_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return NEXT_BUS_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return NEXT_BUS_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusMaxValidityInMs() {
		return NEXT_BUS_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule w/o schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		String targetUUID = getAgencyRouteStopTagTargetUUID(rds);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, targetUUID);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rds.getUUID()); // target RDS UUID instead of custom NextBus Route & Stop tags
			if (rds.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
				}
			}
		}
		return cachedStatus;
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
		return NextBusDbHelper.T_NEXT_BUS_STATUS;
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
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		loadPredictionsFromWWW(requireContextCompat(), rds);
		return getCachedStatus(statusFilter);
	}

	// TODO switch to JSON with "publicJSONFeed"
	private static final String PREDICTION_URL_PART_1_BEFORE_AGENCY_TAG = "https://retro.umoiq.com/service/publicXMLFeed?command=predictions&a=";
	private static final String PREDICTION_URL_PART_2_BEFORE_STOP_ID = "&stopId=";

	@NonNull
	private static String getPredictionUrlString(@NonNull Context context, @NonNull String stopId) {
		return
				PREDICTION_URL_PART_1_BEFORE_AGENCY_TAG + //
						getAGENCY_TAG(context) + //
						PREDICTION_URL_PART_2_BEFORE_STOP_ID + //
						stopId //
				;
	}

	private void loadPredictionsFromWWW(@NonNull Context context, @NonNull RouteDirectionStop rds) {
		try {
			final String stopId = getStopId(rds);
			final String urlString = getPredictionUrlString(context, stopId);
			final String sourceLabel = SourceUtils.getSourceLabel(PREDICTION_URL_PART_1_BEFORE_AGENCY_TAG);
			MTLog.i(this, "Loading from '%s'...", urlString);
			final Request urlRequest = new Request.Builder().url(urlString).build();
			try (Response response = getOkHttpClient(context).newCall(urlRequest).execute()) {
				switch (response.code()) {
				case HttpURLConnection.HTTP_OK:
					long newLastUpdateInMs = TimeUtils.currentTimeMillis();
					final SAXParserFactory spf = SAXParserFactory.newInstance();
					final SAXParser sp = spf.newSAXParser();
					final XMLReader xr = sp.getXMLReader();
					final NextBusPredictionsDataHandler handler = new NextBusPredictionsDataHandler(this, sourceLabel, newLastUpdateInMs, AgencyUtils.getRDSAgencyTimeZone(context));
					xr.setContentHandler(handler);
					xr.parse(new InputSource(response.body().byteStream()));
					final Collection<? extends POIStatus> statuses = handler.getStatuses();
					MTLog.i(this, "Found %d statuses.", statuses == null ? null : statuses.size());
					final Collection<String> targetUUIDs = handler.getStatusesTargetUUIDs();
					StatusProvider.deleteCachedStatus(this, targetUUIDs);
					if (statuses != null) {
						for (POIStatus status : statuses) {
							StatusProvider.cacheStatusS(this, status);
						}
					}
					return;
				default:
					MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", response.code(),
							response.message());
				}
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
		return true;
	}

	@Nullable
	private NextBusDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private NextBusDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return NextBusDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	@NonNull
	public NextBusDbHelper getNewDbHelper(@NonNull Context context) {
		return new NextBusDbHelper(context.getApplicationContext());
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
		cursor = StatusProvider.queryS(this, uri, selection);
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
		type = StatusProvider.getTypeS(this, uri);
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

	private static class NextBusPredictionsDataHandler extends MTDefaultHandler {

		private static final String LOG_TAG = NextBusProvider.LOG_TAG + ">" + NextBusPredictionsDataHandler.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final String BODY = "body";
		private static final String PREDICTIONS = "predictions";
		private static final String PREDICTIONS_ROUTE_TAG = "routeTag";
		private static final String PREDICTIONS_STOP_TAG = "stopTag";
		private static final String PREDICTIONS_ROUTE_TITLE = "routeTitle";
		private static final String PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS = "dirTitleBecauseNoPredictions";
		private static final String DIRECTION = "direction";
		private static final String DIRECTION_TITLE = "title";
		private static final String PREDICTION = "prediction";
		private static final String PREDICTION_EPOCH_TIME = "epochTime";
		private static final String IS_SCHEDULE_BASED = "isScheduleBased";
		private static final String MESSAGE = "message";

		private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

		private String currentLocalName = BODY;

		private String currentRouteTag = null;

		private String currentStopTag = null;

		private String currentRouteTitle = null;

		private String currentDirTitleBecauseNoPredictions = null;

		private String currentDirectionTitle = null;

		private Boolean currentIsScheduleBased = null;

		private final HashSet<Long> currentPredictionEpochTimes = new HashSet<>();

		private final HashMap<String, Schedule> statuses = new HashMap<>();

		private final NextBusProvider provider;
		private final String authority;
		@Nullable
		private final String sourceLabel;
		private final long lastUpdateInMs;
		@NonNull
		private final String localTimeZoneId;

		NextBusPredictionsDataHandler(@NonNull NextBusProvider provider, @Nullable String sourceLabel, long lastUpdateInMs, @NonNull String localTimeZoneId) {
			this.provider = provider;
			this.authority = NextBusProvider.getTARGET_AUTHORITY(this.provider.requireContextCompat());
			this.sourceLabel = sourceLabel;
			this.lastUpdateInMs = lastUpdateInMs;
			this.localTimeZoneId = localTimeZoneId;
		}

		@Nullable
		public Collection<? extends POIStatus> getStatuses() {
			return this.statuses == null ? null : this.statuses.values();
		}

		Collection<String> getStatusesTargetUUIDs() {
			return this.statuses == null ? null : this.statuses.keySet();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (BODY.equals(this.currentLocalName)) {
				this.currentRouteTag = null;
				this.currentStopTag = null;
			} else if (PREDICTIONS.equals(this.currentLocalName)) {
				this.currentRouteTag = attributes.getValue(PREDICTIONS_ROUTE_TAG);
				this.currentStopTag = this.provider.cleanStopTag(attributes.getValue(PREDICTIONS_STOP_TAG));
				this.currentRouteTitle = attributes.getValue(PREDICTIONS_ROUTE_TITLE);
				this.currentDirTitleBecauseNoPredictions = attributes.getValue(PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS);
			} else if (DIRECTION.equals(this.currentLocalName)) {
				this.currentDirectionTitle = attributes.getValue(DIRECTION_TITLE);
				this.currentPredictionEpochTimes.clear();
				this.currentIsScheduleBased = null;
			} else if (PREDICTION.equals(this.currentLocalName)) {
				try {
					String epochTimeS = attributes.getValue(PREDICTION_EPOCH_TIME);
					if (epochTimeS != null && !epochTimeS.isEmpty()) {
						this.currentPredictionEpochTimes.add(
								TimeUtils.timeToTheTensSecondsMillis(
										Long.parseLong(epochTimeS)
								)
						);
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while reading prediction epoch time!");
				}
				String isScheduleBased = attributes.getValue(IS_SCHEDULE_BASED);
				if (isScheduleBased == null || isScheduleBased.isEmpty()) {
					this.currentIsScheduleBased = false; // "If the value is not set then it should be considered false."
				} else {
					this.currentIsScheduleBased = Boolean.parseBoolean(isScheduleBased);
				}
			} else if (MESSAGE.equals(this.currentLocalName)) { // ignore
			} else {
				MTLog.w(this, "startElement() > Unexpected element '%s'", this.currentLocalName);
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
				if (BODY.equals(this.currentLocalName)) { // ignore
				} else if (PREDICTIONS.equals(this.currentLocalName)) { // ignore
				} else if (DIRECTION.equals(this.currentLocalName)) { // ignore
				} else if (PREDICTION.equals(this.currentLocalName)) { // ignore
				} else if (MESSAGE.equals(this.currentLocalName)) { // ignore
				} else {
					MTLog.w(this, "characters() > Unexpected name '%s'! while parsing '%s'", this.currentLocalName, string);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (DIRECTION.equals(localName)) {
				if (TextUtils.isEmpty(this.currentRouteTag) || TextUtils.isEmpty(this.currentStopTag)) {
					return;
				}
				String targetUUID = NextBusProvider.getAgencyRouteStopTagTargetUUID(this.authority, this.currentRouteTag, this.currentStopTag);
				Schedule status = this.statuses.get(targetUUID);
				if (status == null) {
					status = new Schedule(
							null,
							targetUUID,
							this.lastUpdateInMs,
							this.provider.getStatusMaxValidityInMs(),
							this.lastUpdateInMs,
							PROVIDER_PRECISION_IN_MS,
							false,
							this.sourceLabel,
							false
					);
				}
				String tripHeadSign = cleanTripHeadSign(
						getTripHeadSign(this.currentRouteTitle, this.currentDirTitleBecauseNoPredictions, this.currentDirectionTitle)
				);
				for (Long epochTime : this.currentPredictionEpochTimes) {
					Schedule.Timestamp newTimestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(epochTime), this.localTimeZoneId);
					if (!TextUtils.isEmpty(tripHeadSign)) {
						newTimestamp.setHeadsign(Direction.HEADSIGN_TYPE_STRING, tripHeadSign);
					}
					if (this.currentIsScheduleBased != null) {
						newTimestamp.setRealTime(!this.currentIsScheduleBased);
					}
					newTimestamp.setAccessible(Accessibility.UNKNOWN); // no info available on https://retro.umoiq.com/
					status.addTimestampWithoutSort(newTimestamp);
				}
				this.statuses.put(targetUUID, status);
			}
		}

		private String cleanTripHeadSign(String tripHeadSign) {
			try {
				final Context context = this.provider.requireContextCompat();
				if (isSCHEDULE_HEAD_SIGN_CLEAN_STREET_TYPES(context)) {
					tripHeadSign = CleanUtils.cleanStreetTypes(tripHeadSign);
				}
				if (isSCHEDULE_HEAD_SIGN_CLEAN_STREET_TYPES_FR_CA(context)) {
					tripHeadSign = CleanUtils.cleanStreetTypesFRCA(tripHeadSign);
				}
				for (int c = 0; c < getSCHEDULE_HEAD_SIGN_CLEAN_REGEX(context).size(); c++) {
					try {
						tripHeadSign = new Cleaner(
								getSCHEDULE_HEAD_SIGN_CLEAN_REGEX(context).get(c),
								getSCHEDULE_HEAD_SIGN_CLEAN_REPLACEMENT(context).get(c),
								true
						).clean(tripHeadSign);
					} catch (Exception e) {
						MTLog.w(this, e, "Error while cleaning trip head sign %s for %s cleaning configuration!", tripHeadSign, c);
					}
				}
				tripHeadSign = CleanUtils.CLEAN_AT.matcher(tripHeadSign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
				tripHeadSign = CleanUtils.CLEAN_AND.matcher(tripHeadSign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
				tripHeadSign = CleanUtils.CLEAN_ET.matcher(tripHeadSign).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
				tripHeadSign = CleanUtils.cleanLabel(tripHeadSign);
				return tripHeadSign;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadSign);
				return tripHeadSign;
			}
		}

		private String getTripHeadSign(String routeTitle, String dirTitleBecauseNoPredictions, String directionTitle) {
			final Context context = this.provider.requireContextCompat();
			int useDirectionTitle = getSCHEDULE_HEAD_SIGN_USE_DIRECTION_TITLE(context);
			int usePredictionsRouteTitle = getSCHEDULE_HEAD_SIGN_USE_PREDICTIONS_ROUTE_TITLE(context);
			int usePredictionsDirTitleBecauseNoPredictions = getSCHEDULE_HEAD_SIGN_USE_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS(context);
			for (int i = 1; i <= 3; i++) {
				if (useDirectionTitle == i) {
					if (!TextUtils.isEmpty(directionTitle)) {
						return cleanDirectionTitle(directionTitle);
					}
				} else if (usePredictionsRouteTitle == i) {
					if (!TextUtils.isEmpty(routeTitle)) {
						return cleanRouteTitle(routeTitle);
					}
				} else if (usePredictionsDirTitleBecauseNoPredictions == i) {
					if (!TextUtils.isEmpty(dirTitleBecauseNoPredictions)) {
						return cleanDirTitleBecauseNoPredictions(dirTitleBecauseNoPredictions);
					}
				}
			}
			MTLog.w(this, "No schedule trip headsign in the configuration.");
			return null;
		}

		private String cleanDirTitleBecauseNoPredictions(String dirTitleBecauseNoPredictions) {
			final Context context = this.provider.requireContextCompat();
			for (int c = 0; c < getSCHEDULE_HEAD_SIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REGEX(context).size(); c++) {
				try {
					dirTitleBecauseNoPredictions = new Cleaner(
							getSCHEDULE_HEAD_SIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REGEX(context).get(c),
							getSCHEDULE_HEAD_SIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REPLACEMENT(context).get(c),
							true
					).clean(dirTitleBecauseNoPredictions);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", dirTitleBecauseNoPredictions, c);
				}
			}
			return dirTitleBecauseNoPredictions;
		}

		private String cleanRouteTitle(String routeTitle) {
			final Context context = this.provider.requireContextCompat();
			for (int c = 0; c < getSCHEDULE_HEAD_SIGN_PREDICTIONS_ROUTE_TITLE_REGEX(context).size(); c++) {
				try {
					routeTitle = new Cleaner(
							getSCHEDULE_HEAD_SIGN_PREDICTIONS_ROUTE_TITLE_REGEX(context).get(c),
							getSCHEDULE_HEAD_SIGN_PREDICTIONS_ROUTE_TITLE_REPLACEMENT(context).get(c),
							true
					).clean(routeTitle);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", routeTitle, c);
				}
			}
			return routeTitle;
		}

		private String cleanDirectionTitle(String directionTitle) {
			final Context context = this.provider.requireContextCompat();
			for (int c = 0; c < getSCHEDULE_HEAD_SIGN_DIRECTION_TITLE_REGEX(context).size(); c++) {
				try {
					directionTitle = new Cleaner(
							getSCHEDULE_HEAD_SIGN_DIRECTION_TITLE_REGEX(context).get(c),
							getSCHEDULE_HEAD_SIGN_DIRECTION_TITLE_REPLACEMENT(context).get(c),
							true
					).clean(directionTitle);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", directionTitle, c);
				}
			}
			return directionTitle;
		}
	}

	private static class NextBusMessagesDataHandler extends MTDefaultHandler {

		private static final String LOG_TAG = NextBusProvider.LOG_TAG + ">" + NextBusMessagesDataHandler.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final String BODY = "body";
		private static final String ROUTE = "route";
		private static final String ROUTE_TAG = "tag";
		private static final String ROUTE_TAG_ALL = "all";
		private static final String MESSAGE = "message";
		private static final String MESSAGE_ID = "id";
		private static final String MESSAGE_PRIORITY = "priority";
		private static final String MESSAGE_PRIORITY_NORMAL = "Normal";
		private static final String MESSAGE_PRIORITY_LOW = "Low";
		private static final String MESSAGE_START_BOUNDARY = "startBoundary"; // "startBoundaryStr" also available
		private static final String MESSAGE_END_BOUNDARY = "endBoundary"; // "endBoundaryStr" also available
		private static final String ROUTE_CONFIGURED_FOR_MESSAGE = "routeConfiguredForMessage";
		private static final String ROUTE_CONFIGURED_FOR_MESSAGE_TAG = "tag";
		private static final String STOP = "stop";
		private static final String STOP_TAG = "tag";
		private static final String STOP_TITLE = "title";
		private static final String TEXT = "text";
		private static final String TEXT_SECONDARY_LANGUAGE = "textSecondaryLanguage";
		private static final String INTERVAL = "interval";

		private String currentLocalName = BODY;

		private boolean currentRouteAll = false;

		@NonNull
		private final String sourceLabel;
		private final long newLastUpdateInMs;

		private final long serviceUpdateMaxValidityInMs;

		@NonNull
		private final ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();

		private final String agencyTag;
		private final String authority;

		private String currentRouteTag = null;

		private String currentRouteConfiguredForMessageRouteTag = null;
		private final ArrayMap<String, HashSet<String>> currentRouteConfiguredForMessage = new ArrayMap<>();

		private final ArrayMap<String, String> currentStopTabAndTitle = new ArrayMap<>();

		@NonNull
		private final StringBuilder currentTextSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentTextSecondaryLanguageSb = new StringBuilder();

		private final String textLanguageCode;
		private final String textSecondaryLanguageCode;

		@Nullable
		private Cleaner textBoldWords;
		@Nullable
		private Cleaner textSecondaryBoldWords;

		private final ArrayMap<String, HashSet<String>> textMessageIdTargetUUID = new ArrayMap<>();
		private final ArrayMap<String, HashSet<String>> textSecondaryMessageIdTargetUUID = new ArrayMap<>();

		private String currentMessageId;
		private String currentMessagePriority;
		private String currentMessageStartBoundary;
		private String currentMessageEndBoundary;

		private final NextBusProvider provider;

		NextBusMessagesDataHandler(NextBusProvider provider, @NonNull String sourceLabel, long newLastUpdateInMs,
								   String agencyTag, String authority,
								   long serviceUpdateMaxValidityInMs,
								   String textLanguageCode, String textSecondaryLanguageCode,
								   String textBoldWordsRegex, String textSecondaryBoldWordsRegex
		) {
			this.provider = provider;
			this.sourceLabel = sourceLabel;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.agencyTag = agencyTag;
			this.authority = authority;
			this.serviceUpdateMaxValidityInMs = serviceUpdateMaxValidityInMs;
			this.textLanguageCode = textLanguageCode;
			this.textSecondaryLanguageCode = textSecondaryLanguageCode;
			try {
				this.textBoldWords = new Cleaner(textBoldWordsRegex, true);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while compiling text bold regex '%s'!", textBoldWordsRegex);
				this.textBoldWords = null;
			}
			try {
				this.textSecondaryBoldWords = new Cleaner(textSecondaryBoldWordsRegex, true);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while compiling text bold regex '%s'!", textSecondaryBoldWordsRegex);
				this.textSecondaryBoldWords = null;
			}
		}

		@NonNull
		ArrayList<ServiceUpdate> getServiceUpdates() {
			return this.serviceUpdates;
		}

		@SuppressLint("UnknownNullness")
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (BODY.equals(this.currentLocalName)) {
				// DO NOTHING
			} else if (ROUTE.equals(this.currentLocalName)) {
				String routeTag = attributes.getValue(ROUTE_TAG);
				if (ROUTE_TAG_ALL.equals(routeTag)) {
					this.currentRouteTag = null;
					this.currentRouteAll = true;
				} else {
					this.currentRouteTag = routeTag;
					this.currentRouteAll = false;
				}
				this.currentMessagePriority = null;
				this.currentMessageId = null;
				this.currentMessageStartBoundary = null;
				this.currentMessageEndBoundary = null;
			} else if (MESSAGE.equals(this.currentLocalName)) {
				this.currentMessagePriority = attributes.getValue(MESSAGE_PRIORITY);
				this.currentMessageId = attributes.getValue(MESSAGE_ID);
				this.currentMessageStartBoundary = attributes.getValue(MESSAGE_START_BOUNDARY);
				this.currentMessageEndBoundary = attributes.getValue(MESSAGE_END_BOUNDARY);
				if (!this.textMessageIdTargetUUID.containsKey(this.currentMessageId)) {
					this.textMessageIdTargetUUID.put(this.currentMessageId, new HashSet<>());
				}
				if (!this.textSecondaryMessageIdTargetUUID.containsKey(this.currentMessageId)) {
					this.textSecondaryMessageIdTargetUUID.put(this.currentMessageId, new HashSet<>());
				}
				this.currentRouteConfiguredForMessageRouteTag = null;
				this.currentRouteConfiguredForMessage.clear();
				this.currentStopTabAndTitle.clear();
				this.currentTextSb.setLength(0); // clear
				this.currentTextSecondaryLanguageSb.setLength(0); // clear
			} else if (ROUTE_CONFIGURED_FOR_MESSAGE.equals(this.currentLocalName)) {
				this.currentRouteConfiguredForMessageRouteTag = attributes.getValue(ROUTE_CONFIGURED_FOR_MESSAGE_TAG);
				if (!this.currentRouteConfiguredForMessage.containsKey(this.currentRouteConfiguredForMessageRouteTag)) {
					this.currentRouteConfiguredForMessage.put(this.currentRouteConfiguredForMessageRouteTag, new HashSet<>());
				}
			} else if (STOP.equals(this.currentLocalName)) {
				final String stopTag = this.provider.cleanStopTag(attributes.getValue(STOP_TAG));
				final String stopTitle = attributes.getValue(STOP_TITLE);
				HashSet<String> currentRouteConfiguredForMessageRoute = this.currentRouteConfiguredForMessage.get(this.currentRouteConfiguredForMessageRouteTag);
				if (currentRouteConfiguredForMessageRoute == null) {
					currentRouteConfiguredForMessageRoute = new HashSet<>();
				}
				currentRouteConfiguredForMessageRoute.add(stopTag);
				this.currentRouteConfiguredForMessage.put(this.currentRouteConfiguredForMessageRouteTag, currentRouteConfiguredForMessageRoute);
				this.currentStopTabAndTitle.put(stopTag, stopTitle);
			} else if (TEXT.equals(this.currentLocalName)) { // ignore
			} else if (TEXT_SECONDARY_LANGUAGE.equals(this.currentLocalName)) { // ignore
			} else if (INTERVAL.equals(this.currentLocalName)) { // ignore
			} else {
				MTLog.w(this, "startElement() > Unexpected element '%s'", this.currentLocalName);
			}
		}

		@SuppressLint("UnknownNullness")
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			try {
				String string = new String(ch, start, length);
				if (TextUtils.isEmpty(string)) {
					return;
				}
				if (BODY.equals(this.currentLocalName)) { // ignore
				} else if (ROUTE.equals(this.currentLocalName)) { // ignore
				} else if (MESSAGE.equals(this.currentLocalName)) { // ignore
				} else if (ROUTE_CONFIGURED_FOR_MESSAGE.equals(this.currentLocalName)) { // ignore
				} else if (INTERVAL.equals(this.currentLocalName)) { // ignore
				} else if (STOP.equals(this.currentLocalName)) { // ignore
				} else if (TEXT.equals(this.currentLocalName)) {
					this.currentTextSb.append(string);
				} else if (TEXT_SECONDARY_LANGUAGE.equals(this.currentLocalName)) {
					this.currentTextSecondaryLanguageSb.append(string);
				} else {
					MTLog.w(this, "characters() > Unexpected name '%s'! while parsing '%s'", this.currentLocalName, string);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		@SuppressLint("UnknownNullness")
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (MESSAGE.equals(localName)) {
				if (this.currentTextSb.length() == 0 && this.currentTextSecondaryLanguageSb.length() == 0) {
					return; // no message
				}
				final Long startBoundaryLong = NumberUtils.parseLongOrNull(this.currentMessageStartBoundary);
				if (startBoundaryLong != null) {
					if (this.newLastUpdateInMs < startBoundaryLong) {
						MTLog.d(this, "SKIP (starting at: %s).", startBoundaryLong);
						return; // to soon, not started yet
					}
				}
				final Long endBoundaryLong = NumberUtils.parseLongOrNull(this.currentMessageEndBoundary);
				if (endBoundaryLong != null) {
					if (this.newLastUpdateInMs > endBoundaryLong) {
						MTLog.d(this, "SKIP (ended since: %s).", endBoundaryLong);
						return; // already ended
					}
				}
				if (!this.currentRouteConfiguredForMessage.isEmpty()) { // ROUTE(s)
					for (String routeTag : this.currentRouteConfiguredForMessage.keySet()) {
						if (this.currentRouteTag != null && !this.currentRouteTag.equals(routeTag)) {
							// MTLog.d(this, "SKIP (other route tag: %s vs %s).", this.currentRouteTag, routeTag);
							continue; // will be repeated for each route tag
						}
						final HashSet<String> currentRouteConfiguredForMessageRoute = this.currentRouteConfiguredForMessage.get(routeTag);
						final int stopCount = currentRouteConfiguredForMessageRoute == null ? 0 : currentRouteConfiguredForMessageRoute.size();
						if (stopCount == 0) {
							final String routeTargetUUID = NextBusProvider.getServiceUpdateAgencyRouteTagTargetUUID(this.authority, routeTag);
							final int severity = findRouteSeverity();
							//noinspection UnnecessaryLocalVariable
							final String title = routeTag;
							addServiceUpdates(routeTargetUUID, severity, title);
						} else {
							for (String stopTag : currentRouteConfiguredForMessageRoute) {
								final String routeStopTargetUUID = NextBusProvider.getAgencyRouteStopTagTargetUUID(this.authority, routeTag, stopTag);
								final String title = stopCount < 10
										? this.currentStopTabAndTitle.getOrDefault(stopTag, stopTag)
										: routeTag;
								final int severity = findStopPriority();
								addServiceUpdates(routeStopTargetUUID, severity, title);
							}
							// ADD duplicates for routeTag (UI will only show it once)
							final String routeTargetUUID = NextBusProvider.getServiceUpdateAgencyRouteTagTargetUUID(this.authority, routeTag);
							final int severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
							//noinspection UnnecessaryLocalVariable
							final String title = routeTag;
							addServiceUpdates(routeTargetUUID, severity, title);
						}
					}
				} else if (this.currentRouteTag != null) {
					final String routeTargetUUID = NextBusProvider.getServiceUpdateAgencyRouteTagTargetUUID(this.authority, this.currentRouteTag);
					final String title = this.currentRouteTag;
					final int severity = findAgencySeverity();
					addServiceUpdates(routeTargetUUID, severity, title);
				} else if (this.currentRouteAll) { // AGENCY
					final String agencyTargetUUID = NextBusProvider.getServiceUpdateAgencyTargetUUID(this.authority);
					final String title = this.agencyTag.toUpperCase(Locale.ROOT);
					final int severity = findAgencySeverity();
					addServiceUpdates(agencyTargetUUID, severity, title);
				} else {
					MTLog.w(this, "Unexpected combination of tags!");
				}
			}
		}

		private int findStopPriority() {
			if (MESSAGE_PRIORITY_NORMAL.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_WARNING_POI;
			} else if (MESSAGE_PRIORITY_LOW.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_POI;
			}
			MTLog.w(this, "endElement() > unexpected message priority: %s", this.currentMessagePriority);
			return ServiceUpdate.SEVERITY_WARNING_UNKNOWN; // default
		}

		private int findRouteSeverity() {
			if (MESSAGE_PRIORITY_NORMAL.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
			} else if (MESSAGE_PRIORITY_LOW.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
			}
			MTLog.w(this, "endElement() > unexpected message priority: %s", this.currentMessagePriority);
			return ServiceUpdate.SEVERITY_WARNING_UNKNOWN; // default
		}

		private int findAgencySeverity() {
			if (MESSAGE_PRIORITY_NORMAL.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_AGENCY;
			} else if (MESSAGE_PRIORITY_LOW.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_AGENCY;
			}
			MTLog.w(this, "endElement() > unexpected message priority: %s", this.currentMessagePriority);
			return ServiceUpdate.SEVERITY_WARNING_UNKNOWN; // default
		}

		private void addServiceUpdates(@NonNull String targetUUID, int severity, @Nullable String title) {
			final String replacement = ServiceUpdateCleaner.getReplacement(severity);
			if (this.currentTextSb.length() > 0) {
				final HashSet<String> textMessageIdTargetUUIDCurrentMessageUUIDs = this.textMessageIdTargetUUID.get(this.currentMessageId);
				if (textMessageIdTargetUUIDCurrentMessageUUIDs != null //
						&& !textMessageIdTargetUUIDCurrentMessageUUIDs.contains(targetUUID)) {
					this.serviceUpdates.add(new ServiceUpdate(null,
							targetUUID,
							this.newLastUpdateInMs,
							this.serviceUpdateMaxValidityInMs,
							ServiceUpdateCleaner.makeText(title, this.currentTextSb.toString()),
							ServiceUpdateCleaner.makeTextHTML(
									title,
									enhanceHtml(
											ServiceUpdateCleaner.clean(this.currentTextSb.toString(), replacement),
											this.textBoldWords,
											replacement
									)
							),
							severity,
							AGENCY_SOURCE_ID,
							this.sourceLabel,
							this.currentMessageId,
							this.textLanguageCode
					));
					textMessageIdTargetUUIDCurrentMessageUUIDs.add(targetUUID);
				}
			}
			if (this.currentTextSecondaryLanguageSb.length() > 0) {
				final HashSet<String> textSecondaryMessageIdTargetUUIDMessageUUIDs = this.textSecondaryMessageIdTargetUUID.get(this.currentMessageId);
				if (textSecondaryMessageIdTargetUUIDMessageUUIDs != null //
						&& !textSecondaryMessageIdTargetUUIDMessageUUIDs.contains(targetUUID)) {
					this.serviceUpdates.add(new ServiceUpdate(null,
							targetUUID,
							this.newLastUpdateInMs,
							this.serviceUpdateMaxValidityInMs,
							ServiceUpdateCleaner.makeText(title, this.currentTextSecondaryLanguageSb.toString()),
							ServiceUpdateCleaner.makeTextHTML(
									title,
									enhanceHtml(
											ServiceUpdateCleaner.clean(this.currentTextSecondaryLanguageSb.toString(), replacement),
											this.textSecondaryBoldWords,
											replacement
									)
							),
							severity,
							AGENCY_SOURCE_ID,
							this.sourceLabel,
							this.currentMessageId,
							this.textSecondaryLanguageCode
					));
					textSecondaryMessageIdTargetUUIDMessageUUIDs.add(targetUUID);
				}
			}
		}

		private String enhanceHtml(String originalHtml, @Nullable Cleaner boldWords, @Nullable String replacement) {
			if (TextUtils.isEmpty(originalHtml)) {
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
	}

	public static class NextBusDbHelper extends MTSQLiteOpenHelper { // will store statuses & vehicle location...

		private static final String LOG_TAG = NextBusDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link NextBusDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "nextbus.db";

		/**
		 * Override if multiple {@link NextBusDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pNextBusMessagesLastUpdate";

		static final String T_NEXT_BUS_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_NEXT_BUS_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_NEXT_BUS_SERVICE_UPDATE).build();

		private static final String T_NEXT_BUS_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEXT_BUS_SERVICE_UPDATE);

		static final String T_NEXT_BUS_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_NEXT_BUS_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_NEXT_BUS_STATUS).build();

		private static final String T_NEXT_BUS_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEXT_BUS_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link NextBusDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.next_bus_db_version);
				dbVersion++; // add "service_update.original_id" column
			}
			return dbVersion;
		}

		@NonNull
		private final Context context;

		NextBusDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_NEXT_BUS_SERVICE_UPDATE_SQL_DROP);
			db.execSQL(T_NEXT_BUS_STATUS_SQL_DROP);
			PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_NEXT_BUS_SERVICE_UPDATE_SQL_CREATE);
			db.execSQL(T_NEXT_BUS_STATUS_SQL_CREATE);
		}
	}
}
