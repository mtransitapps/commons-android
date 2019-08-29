package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;
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
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

@SuppressLint("Registered")
public class NextBusProvider extends MTContentProvider implements ServiceUpdateProviderContract, StatusProviderContract {

	private static final String TAG = NextBusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = NextBusDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.next_bus_authority);
		}
		return authority;
	}

	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getTARGET_AUTHORITY(Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.next_bus_for_poi_authority);
		}
		return targetAuthority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String agencyTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getAGENCY_TAG(Context context) {
		if (agencyTag == null) {
			agencyTag = context.getResources().getString(R.string.next_bus_agency_tag);
		}
		return agencyTag;
	}

	private static Boolean usingStopCodeAsStopId = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isUSING_STOP_CODE_AS_STOP_ID(Context context) {
		if (usingStopCodeAsStopId == null) {
			usingStopCodeAsStopId = context.getResources().getBoolean(R.bool.next_bus_use_stop_code_as_stop_id);
		}
		return usingStopCodeAsStopId;
	}

	private static Boolean usingStopIdAsStopTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isUSING_STOP_ID_AS_STOP_TAG(Context context) {
		if (usingStopIdAsStopTag == null) {
			usingStopIdAsStopTag = context.getResources().getBoolean(R.bool.next_bus_use_stop_id_as_stop_tag);
		}
		return usingStopIdAsStopTag;
	}

	private static String textLanguageCode = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getTEXT_LANGUAGE_CODE(Context context) {
		if (textLanguageCode == null) {
			textLanguageCode = context.getResources().getString(R.string.next_bus_messages_text_language_code);
		}
		return textLanguageCode;
	}

	private static String textSecondaryLanguageCode = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getTEXT_SECONDARY_LANGUAGE_CODE(Context context) {
		if (textSecondaryLanguageCode == null) {
			textSecondaryLanguageCode = context.getResources().getString(R.string.next_bus_messages_text_secondary_language_code);
		}
		return textSecondaryLanguageCode;
	}

	private static String textBoldWords = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getTEXT_BOLD_WORDS(Context context) {
		if (textBoldWords == null) {
			textBoldWords = context.getResources().getString(R.string.next_bus_messages_text_bold_words);
		}
		return textBoldWords;
	}

	private static String textSecondaryBoldWords = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static String getTEXT_SECONDARY_BOLD_WORDS(Context context) {
		if (textSecondaryBoldWords == null) {
			textSecondaryBoldWords = context.getResources().getString(R.string.next_bus_messages_text_secondary_bold_words);
		}
		return textSecondaryBoldWords;
	}

	private static Boolean appendHeadsignValueToRouteTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isAPPEND_HEADSIGN_VALUE_TO_ROUTE_TAG(Context context) {
		if (appendHeadsignValueToRouteTag == null) {
			appendHeadsignValueToRouteTag = context.getResources().getBoolean(R.bool.next_bus_route_tag_append_headsign_value);
		}
		return appendHeadsignValueToRouteTag;
	}

	private static java.util.List<String> routeTagHeadsignValueReplaceFrom = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getROUTE_TAG_HEADSIGN_VALUE_REPLACE_FROM(Context context) {
		if (routeTagHeadsignValueReplaceFrom == null) {
			routeTagHeadsignValueReplaceFrom = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_route_tag_headsign_values_replace_from));
		}
		return routeTagHeadsignValueReplaceFrom;
	}

	private static java.util.List<String> routeTagHeadsignValueReplaceTo = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getROUTE_TAG_HEADSIGN_VALUE_REPLACE_TO(Context context) {
		if (routeTagHeadsignValueReplaceTo == null) {
			routeTagHeadsignValueReplaceTo = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_route_tag_headsign_values_replace_to));
		}
		return routeTagHeadsignValueReplaceTo;
	}

	private static java.util.List<String> stopTagCleanRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSTOP_TAG_CLEAN_REGEX(Context context) {
		if (stopTagCleanRegex == null) {
			stopTagCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_stop_tag_clean_regex));
		}
		return stopTagCleanRegex;
	}

	private static java.util.List<String> stopTagCleanReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSTOP_TAG_CLEAN_REPLACEMENT(Context context) {
		if (stopTagCleanReplacement == null) {
			stopTagCleanReplacement = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_stop_tag_clean_replacement));
		}
		return stopTagCleanReplacement;
	}

	private static Boolean scheduleHeadsignCleanStreetTypes = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isSCHEDULE_HEADSIGN_CLEAN_STREET_TYPES(Context context) {
		if (scheduleHeadsignCleanStreetTypes == null) {
			scheduleHeadsignCleanStreetTypes = context.getResources().getBoolean(R.bool.next_bus_schedule_head_sign_clean_street_types);
		}
		return scheduleHeadsignCleanStreetTypes;
	}

	private static Boolean scheduleHeadsignCleanStreetTypesFrCa = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static boolean isSCHEDULE_HEADSIGN_CLEAN_STREET_TYPES_FR_CA(Context context) {
		if (scheduleHeadsignCleanStreetTypesFrCa == null) {
			scheduleHeadsignCleanStreetTypesFrCa = context.getResources().getBoolean(R.bool.next_bus_schedule_head_sign_clean_street_types_fr_ca);
		}
		return scheduleHeadsignCleanStreetTypesFrCa;
	}

	private static java.util.List<String> scheduleHeadsignCleanRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REGEX(Context context) {
		if (scheduleHeadsignCleanRegex == null) {
			scheduleHeadsignCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_schedule_head_sign_clean_regex));
		}
		return scheduleHeadsignCleanRegex;
	}

	private static java.util.List<String> scheduleHeadsignCleanReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(Context context) {
		if (scheduleHeadsignCleanReplacement == null) {
			scheduleHeadsignCleanReplacement = Arrays.asList(context.getResources().getStringArray(R.array.next_bus_schedule_head_sign_clean_replacement));
		}
		return scheduleHeadsignCleanReplacement;
	}

	private static int scheduleHeadsignUseDirectionTitle = -1;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static int getSCHEDULE_HEADSIGN_USE_DIRECTION_TITLE(Context context) {
		if (scheduleHeadsignUseDirectionTitle < 0) {
			scheduleHeadsignUseDirectionTitle = context.getResources().getInteger(R.integer.next_bus_schedule_head_sign_use_direction_title);
		}
		return scheduleHeadsignUseDirectionTitle;
	}

	private static java.util.List<String> scheduleHeadsignDirectionTitleRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_DIRECTION_TITLE_REGEX(Context context) {
		if (scheduleHeadsignDirectionTitleRegex == null) {
			scheduleHeadsignDirectionTitleRegex = Arrays.asList(context.getResources()
					.getStringArray(R.array.next_bus_schedule_head_sign_direction_title_regex));
		}
		return scheduleHeadsignDirectionTitleRegex;
	}

	private static java.util.List<String> scheduleHeadsignDirectionTitleReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_DIRECTION_TITLE_REPLACEMENT(Context context) {
		if (scheduleHeadsignDirectionTitleReplacement == null) {
			scheduleHeadsignDirectionTitleReplacement = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_direction_title_replacement));
		}
		return scheduleHeadsignDirectionTitleReplacement;
	}

	private static int scheduleHeadsignUsePredictionsDirTitleBecauseNoPredictions = -1;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static int getSCHEDULE_HEADSIGN_USE_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS(Context context) {
		if (scheduleHeadsignUsePredictionsDirTitleBecauseNoPredictions < 0) {
			scheduleHeadsignUsePredictionsDirTitleBecauseNoPredictions = context.getResources().getInteger(
					R.integer.next_bus_schedule_head_sign_use_predictions_dir_title_because_no_predictions);
		}
		return scheduleHeadsignUsePredictionsDirTitleBecauseNoPredictions;
	}

	private static java.util.List<String> scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REGEX(Context context) {
		if (scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsRegex == null) {
			scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsRegex = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_dir_title_because_no_predictions_regex));
		}
		return scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsRegex;
	}

	private static java.util.List<String> scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REPLACEMENT(Context context) {
		if (scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsReplacement == null) {
			scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsReplacement = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_dir_title_because_no_predictions_replacement));
		}
		return scheduleHeadsignPredictionsDirTitleBecauseNoPredictionsReplacement;
	}

	private static int scheduleHeadsignUsePredictionsRouteTitle = -1;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static int getSCHEDULE_HEADSIGN_USE_PREDICTIONS_ROUTE_TITLE(Context context) {
		if (scheduleHeadsignUsePredictionsRouteTitle < 0) {
			scheduleHeadsignUsePredictionsRouteTitle = context.getResources().getInteger(R.integer.next_bus_schedule_head_sign_use_predictions_route_title);
		}
		return scheduleHeadsignUsePredictionsRouteTitle;
	}

	private static java.util.List<String> scheduleHeadsignPredictionsRouteTitleRegex = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_PREDICTIONS_ROUTE_TITLE_REGEX(Context context) {
		if (scheduleHeadsignPredictionsRouteTitleRegex == null) {
			scheduleHeadsignPredictionsRouteTitleRegex = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_route_title_regex));
		}
		return scheduleHeadsignPredictionsRouteTitleRegex;
	}

	private static java.util.List<String> scheduleHeadsignPredictionsRouteTitleReplacement = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_PREDICTIONS_ROUTE_TITLE_REPLACEMENT(Context context) {
		if (scheduleHeadsignPredictionsRouteTitleReplacement == null) {
			scheduleHeadsignPredictionsRouteTitleReplacement = Arrays.asList(context.getResources().getStringArray(
					R.array.next_bus_schedule_head_sign_predictions_route_title_replacement));
		}
		return scheduleHeadsignPredictionsRouteTitleReplacement;
	}

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10);

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);

	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10);

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public String getServiceUpdateDbTableName() {
		return NextBusDbHelper.T_NEXT_BUS_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<ServiceUpdate>();
		HashSet<String> targetUUIDs = getTargetUUIDs(rts);
		for (String targetUUID : targetUUIDs) {
			ArrayList<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUID);
			if (cachedServiceUpdates != null) {
				serviceUpdates.addAll(cachedServiceUpdates);
			}
		}
		enhanceRTServiceUpdateForStop(serviceUpdates, rts);
		return serviceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(ArrayList<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	private HashSet<String> getTargetUUIDs(RouteTripStop rts) {
		HashSet<String> targetUUIDs = new HashSet<String>();
		targetUUIDs.add(getAgencyTargetUUID(rts.getAuthority()));
		targetUUIDs.add(getAgencyRouteTagTargetUUID(rts.getAuthority(), getRouteTag(rts)));
		targetUUIDs.add(getAgencyRouteStopTagTargetUUID(rts));
		return targetUUIDs;
	}

	private String getAgencyRouteStopTagTargetUUID(RouteTripStop rts) {
		return getAgencyRouteStopTagTargetUUID(rts.getAuthority(), getRouteTag(rts), getStopTag(rts));
	}

	public String getRouteTag(RouteTripStop rts) {
		StringBuilder sb = new StringBuilder();
		sb.append(rts.getRoute().getShortName());
		if (isAPPEND_HEADSIGN_VALUE_TO_ROUTE_TAG(getContext())) {
			sb.append(geRouteTagHeadsignValue(rts));
		}
		return sb.toString();
	}

	private String geRouteTagHeadsignValue(RouteTripStop rts) {
		String tripHeadsingValue = rts.getTrip().getHeadsignValue();
		for (int i = 0; i < getROUTE_TAG_HEADSIGN_VALUE_REPLACE_FROM(getContext()).size(); i++) {
			if (getROUTE_TAG_HEADSIGN_VALUE_REPLACE_FROM(getContext()).get(i).equals(tripHeadsingValue)) {
				tripHeadsingValue = getROUTE_TAG_HEADSIGN_VALUE_REPLACE_TO(getContext()).get(i);
			}
		}
		return tripHeadsingValue;
	}

	public String getStopTag(RouteTripStop rts) {
		if (isUSING_STOP_ID_AS_STOP_TAG(getContext())) {
			return String.valueOf(rts.getStop().getId());
		}
		return rts.getStop().getCode();
	}

	public String getStopId(RouteTripStop rts) {
		if (isUSING_STOP_CODE_AS_STOP_ID(getContext())) {
			return rts.getStop().getCode();
		}
		return String.valueOf(rts.getStop().getId());
	}

	public String cleanStopTag(String stopTag) {
		for (int i = 0; i < getSTOP_TAG_CLEAN_REGEX(getContext()).size(); i++) {
			try {
				stopTag = Pattern.compile(getSTOP_TAG_CLEAN_REGEX(getContext()).get(i), Pattern.CASE_INSENSITIVE).matcher(stopTag)
						.replaceAll(getSTOP_TAG_CLEAN_REPLACEMENT(getContext()).get(i));
			} catch (Exception e) {
				MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", stopTag, i);
			}
		}
		return stopTag;
	}

	protected static String getAgencyRouteTagTargetUUID(String agencyAuthority, String routeTag) {
		return POI.POIUtils.getUUID(agencyAuthority, routeTag);
	}

	protected static String getAgencyRouteStopTagTargetUUID(String agencyAuthority, String routeTag, String stopTag) {
		return POI.POIUtils.getUUID(agencyAuthority, routeTag, stopTag);
	}

	protected static String getAgencyTargetUUID(String agencyAuthority) {
		return POI.POIUtils.getUUID(agencyAuthority);
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(String targetUUID, String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
	}

	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (serviceUpdateFilter == null || serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(rts.getAuthority(), serviceUpdateFilter.isInFocusOrDefault());
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(serviceUpdateFilter);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(getAgencyTargetUUID(rts.getAuthority())));
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	public ServiceUpdate getServiceUpdateNone(String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, getServiceUpdateLanguage());
	}

	private static final String AGENCY_SOURCE_ID = "next_bus_com_messages";

	private static final String AGENCY_SOURCE_LABEL = "NextBus";

	private void updateAgencyServiceUpdateDataIfRequired(String tagetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(tagetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(String tagetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(tagetAuthority, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(String tagetAuthority, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(tagetAuthority);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	private static final String AGENCY_URL_PART_1_BEFORE_AGENCY_TAG = "http://webservices.nextbus.com/service/publicXMLFeed?command=messages&a=";

	private static String getAgencyUrlString(Context context) {
		return new StringBuilder() //
				.append(AGENCY_URL_PART_1_BEFORE_AGENCY_TAG) //
				.append(getAGENCY_TAG(context)) //
				.toString();
	}

	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(String tagetAuthority) {
		try {
			String urlString = getAgencyUrlString(getContext());
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				NextBusMessagesDataHandler handler = new NextBusMessagesDataHandler(this, newLastUpdateInMs, getTARGET_AUTHORITY(getContext()),
						getServiceUpdateMaxValidityInMs(), getTEXT_LANGUAGE_CODE(getContext()), getTEXT_SECONDARY_LANGUAGE_CODE(getContext()),
						getTEXT_BOLD_WORDS(getContext()), getTEXT_SECONDARY_BOLD_WORDS(getContext()));
				xr.setContentHandler(handler);
				xr.parse(new InputSource(urlc.getInputStream()));
				return handler.getServiceUpdates();
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
			MTLog.w(TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) { // Unknown error
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}


	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getDBHelper().getWritableDatabase().delete(getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency service update data!");
		}
		return affectedRows;
	}

	private static String serviceUpdateLanguage = null;

	@Override
	public String getServiceUpdateLanguage() {
		if (serviceUpdateLanguage == null) {
			String newServiceUpdateLanguage = Locale.ENGLISH.getLanguage();
			if (LocaleUtils.isFR()) {
				if (getTEXT_LANGUAGE_CODE(getContext()).contains(Locale.FRENCH.getLanguage())
						|| getTEXT_SECONDARY_LANGUAGE_CODE(getContext()).contains(Locale.FRENCH.getLanguage())) {
					newServiceUpdateLanguage = Locale.FRENCH.getLanguage();
				}
			}
			serviceUpdateLanguage = newServiceUpdateLanguage;
		}
		return serviceUpdateLanguage;
	}

	private static final long NEXT_BUS_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30);
	private static final long NEXT_BUS_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5);
	private static final long NEXT_BUS_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long NEXT_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(2);
	private static final long NEXT_BUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

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
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		String targetUUID = getAgencyRouteStopTagTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, targetUUID);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom NextBus Route & Stop tags
			if (cachedStatus instanceof Schedule) {
				((Schedule) cachedStatus).setDescentOnly(rts.isDescentOnly());
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

	@Override
	public String getStatusDbTableName() {
		return NextBusDbHelper.T_NEXT_BUS_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Override
	public POIStatus getNewStatus(StatusProviderContract.Filter statusFilter) {
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		loadPredictionsFromWWW(getStopId(rts));
		return getCachedStatus(statusFilter);
	}

	private static final String PREDICTION_URL_PART_1_BEFORE_AGENCY_TAG = "http://webservices.nextbus.com/service/publicXMLFeed?command=predictions&a=";
	private static final String PREDICTION_URL_PART_2_BEFORE_STOP_ID = "&stopId=";

	private static String getPredictionUrlString(Context context, String stopId) {
		return new StringBuilder() //
				.append(PREDICTION_URL_PART_1_BEFORE_AGENCY_TAG) //
				.append(getAGENCY_TAG(context)) //
				.append(PREDICTION_URL_PART_2_BEFORE_STOP_ID) //
				.append(stopId) //
				.toString();
	}

	private void loadPredictionsFromWWW(String stopId) {
		try {
			String urlString = getPredictionUrlString(getContext(), stopId);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				NextBusPredictionsDataHandler handler = new NextBusPredictionsDataHandler(this, newLastUpdateInMs);
				xr.setContentHandler(handler);
				xr.parse(new InputSource(urlc.getInputStream()));
				Collection<? extends POIStatus> statuses = handler.getStatuses();
				Collection<String> targetUUIDs = handler.getStatusesTargetUUIDs();
				StatusProvider.deleteCachedStatus(this, targetUUIDs);
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
	}

	private static NextBusDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private NextBusDbHelper getDBHelper(Context context) {
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
		return NextBusDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public NextBusDbHelper getNewDbHelper(Context context) {
		return new NextBusDbHelper(context.getApplicationContext());
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

	@Override
	public String getTypeMT(Uri uri) {
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

	private static class NextBusPredictionsDataHandler extends MTDefaultHandler {

		private static final String TAG = NextBusProvider.TAG + ">" + NextBusPredictionsDataHandler.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
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
		private static final String MESSAGE = "message";

		private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

		private String currentLocalName = BODY;

		private String currentRouteTag = null;

		private String currentStopTag = null;

		private String currentRouteTitle = null;

		private String currentDirTitleBecauseNoPredictions = null;

		private String currentDirectionTitle = null;

		private HashSet<Long> currentPredictionEpochTimes = new HashSet<Long>();

		private HashMap<String, Schedule> statuses = new HashMap<String, Schedule>();

		private NextBusProvider provider;
		private String authority;
		private long lastUpdateInMs;

		public NextBusPredictionsDataHandler(NextBusProvider provider, long lastUpdateInMs) {
			this.provider = provider;
			this.authority = NextBusProvider.getTARGET_AUTHORITY(this.provider.getContext());
			this.lastUpdateInMs = lastUpdateInMs;
		}

		public Collection<? extends POIStatus> getStatuses() {
			return this.statuses == null ? null : this.statuses.values();
		}

		public Collection<String> getStatusesTargetUUIDs() {
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
			} else if (PREDICTION.equals(this.currentLocalName)) {
				try {
					Long epochTime = Long.valueOf(attributes.getValue(PREDICTION_EPOCH_TIME));
					if (epochTime != null) {
						this.currentPredictionEpochTimes.add(TimeUtils.timeToTheTensSecondsMillis(epochTime));
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while reading prediction epoch time!");
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
				if (!this.statuses.containsKey(targetUUID)) {
					this.statuses.put(targetUUID, new Schedule(targetUUID, this.lastUpdateInMs, this.provider.getStatusMaxValidityInMs(), this.lastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false));
				}
				String tripHeadsign = cleanTripHeadsign(getTripHeadsign(this.currentRouteTitle, this.currentDirTitleBecauseNoPredictions,
						this.currentDirectionTitle));
				for (Long epochTime : this.currentPredictionEpochTimes) {
					Schedule.Timestamp newTimestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(epochTime));
					if (!TextUtils.isEmpty(tripHeadsign)) {
						newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, tripHeadsign);
					}
					this.statuses.get(targetUUID).addTimestampWithoutSort(newTimestamp);
				}
			}
		}

		private String cleanTripHeadsign(String tripHeadsign) {
			try {
				if (isSCHEDULE_HEADSIGN_CLEAN_STREET_TYPES(this.provider.getContext())) {
					tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
				}
				if (isSCHEDULE_HEADSIGN_CLEAN_STREET_TYPES_FR_CA(this.provider.getContext())) {
					tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
				}
				for (int c = 0; c < getSCHEDULE_HEADSIGN_CLEAN_REGEX(this.provider.getContext()).size(); c++) {
					try {
						tripHeadsign = Pattern.compile(getSCHEDULE_HEADSIGN_CLEAN_REGEX(this.provider.getContext()).get(c), Pattern.CASE_INSENSITIVE)
								.matcher(tripHeadsign).replaceAll(getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(this.provider.getContext()).get(c));
					} catch (Exception e) {
						MTLog.w(this, e, "Error while cleaning trip head sign %s for %s cleaning configuration!", tripHeadsign, c);
					}
				}
				tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
				tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
				tripHeadsign = CleanUtils.CLEAN_ET.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
				tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
				return tripHeadsign;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
				return tripHeadsign;
			}
		}

		private String getTripHeadsign(String routeTitle, String dirTitleBecauseNoPredictions, String directionTitle) {
			int useDirectionTitle = getSCHEDULE_HEADSIGN_USE_DIRECTION_TITLE(this.provider.getContext());
			int usePredictionsRouteTitle = getSCHEDULE_HEADSIGN_USE_PREDICTIONS_ROUTE_TITLE(this.provider.getContext());
			int usePredictionsDirTitleBecauseNoPredictions = getSCHEDULE_HEADSIGN_USE_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS(this.provider.getContext());
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
			for (int c = 0; c < getSCHEDULE_HEADSIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REGEX(this.provider.getContext()).size(); c++) {
				try {
					dirTitleBecauseNoPredictions = Pattern
							.compile(getSCHEDULE_HEADSIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REGEX(this.provider.getContext()).get(c),
									Pattern.CASE_INSENSITIVE).matcher(dirTitleBecauseNoPredictions)
							.replaceAll(getSCHEDULE_HEADSIGN_PREDICTIONS_DIR_TITLE_BECAUSE_NO_PREDICTIONS_REPLACEMENT(this.provider.getContext()).get(c));
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", dirTitleBecauseNoPredictions, c);
				}
			}
			return dirTitleBecauseNoPredictions;
		}

		private String cleanRouteTitle(String routeTitle) {
			for (int c = 0; c < getSCHEDULE_HEADSIGN_PREDICTIONS_ROUTE_TITLE_REGEX(this.provider.getContext()).size(); c++) {
				try {
					routeTitle = Pattern
							.compile(getSCHEDULE_HEADSIGN_PREDICTIONS_ROUTE_TITLE_REGEX(this.provider.getContext()).get(c), Pattern.CASE_INSENSITIVE)
							.matcher(routeTitle).replaceAll(getSCHEDULE_HEADSIGN_PREDICTIONS_ROUTE_TITLE_REPLACEMENT(this.provider.getContext()).get(c));
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", routeTitle, c);
				}
			}
			return routeTitle;
		}

		private String cleanDirectionTitle(String directionTitle) {
			for (int c = 0; c < getSCHEDULE_HEADSIGN_DIRECTION_TITLE_REGEX(this.provider.getContext()).size(); c++) {
				try {
					directionTitle = Pattern.compile(getSCHEDULE_HEADSIGN_DIRECTION_TITLE_REGEX(this.provider.getContext()).get(c), Pattern.CASE_INSENSITIVE)
							.matcher(directionTitle).replaceAll(getSCHEDULE_HEADSIGN_DIRECTION_TITLE_REPLACEMENT(this.provider.getContext()).get(c));
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning stop tag %s for %s cleaning configuration!", directionTitle, c);
				}
			}
			return directionTitle;
		}
	}

	public static class NextBusMessagesDataHandler extends MTDefaultHandler {

		private static final String TAG = NextBusProvider.TAG + ">" + NextBusMessagesDataHandler.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
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
		private static final String ROUTE_CONFIGURED_FOR_MESSAGE = "routeConfiguredForMessage";
		private static final String ROUTE_CONFIGURED_FOR_MESSAGE_TAG = "tag";
		private static final String STOP = "stop";
		private static final String STOP_TAG = "tag";
		private static final String TEXT = "text";
		private static final String TEXT_SECONDARY_LANGUAGE = "textSecondaryLanguage";
		private static final String INTERVAL = "interval";

		private String currentLocalName = BODY;

		private boolean currentRouteAll = false;

		private long newLastUpdateInMs;

		private long serviceUpdateMaxValidityInMs;

		private ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<ServiceUpdate>();

		private String authority;

		private String currentRouteTag = null;

		private String currentRouteConfiguredForMessageRouteTag = null;
		private ArrayMap<String, HashSet<String>> currentRouteConfiguredForMessage = new ArrayMap<String, HashSet<String>>();

		private StringBuilder currentTextSb = new StringBuilder();
		private StringBuilder currentTextSecondaryLanguageSb = new StringBuilder();

		private String textLanguageCode;
		private String textSecondaryLanguageCode;

		private Pattern textBoldWords;
		private Pattern textSecondaryBoldWords;

		private ArrayMap<String, HashSet<String>> textMessageIdTargetUUID = new ArrayMap<String, HashSet<String>>();
		private ArrayMap<String, HashSet<String>> textSecondaryMessageIdTargetUUID = new ArrayMap<String, HashSet<String>>();

		private String currentMessageId;
		private String currentMessagePriority;

		private NextBusProvider provider;

		public NextBusMessagesDataHandler(NextBusProvider provider, long newLastUpdateInMs, String authority, long serviceUpdateMaxValidityInMs,
				String textLanguageCode, String textSecondaryLanguageCode, String textBoldWords, String textSecondaryBoldWords) {
			this.provider = provider;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.authority = authority;
			this.serviceUpdateMaxValidityInMs = serviceUpdateMaxValidityInMs;
			this.textLanguageCode = textLanguageCode;
			this.textSecondaryLanguageCode = textSecondaryLanguageCode;
			try {
				this.textBoldWords = Pattern.compile(textBoldWords, Pattern.CASE_INSENSITIVE);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while compiling text bold regex pattern '%s'!", textBoldWords);
			}
			try {
				this.textSecondaryBoldWords = Pattern.compile(textSecondaryBoldWords, Pattern.CASE_INSENSITIVE);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while compiling text bold regex pattern '%s'!", textSecondaryBoldWords);
			}
		}

		public ArrayList<ServiceUpdate> getServiceUpdates() {
			return this.serviceUpdates;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (BODY.equals(this.currentLocalName)) {
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
			} else if (MESSAGE.equals(this.currentLocalName)) {
				this.currentMessagePriority = attributes.getValue(MESSAGE_PRIORITY);
				this.currentMessageId = attributes.getValue(MESSAGE_ID);
				if (!this.textMessageIdTargetUUID.containsKey(this.currentMessageId)) {
					this.textMessageIdTargetUUID.put(this.currentMessageId, new HashSet<String>());
				}
				if (!this.textSecondaryMessageIdTargetUUID.containsKey(this.currentMessageId)) {
					this.textSecondaryMessageIdTargetUUID.put(this.currentMessageId, new HashSet<String>());
				}
				this.currentRouteConfiguredForMessageRouteTag = null;
				this.currentRouteConfiguredForMessage.clear();
				this.currentTextSb = new StringBuilder();
				this.currentTextSecondaryLanguageSb = new StringBuilder();
			} else if (ROUTE_CONFIGURED_FOR_MESSAGE.equals(this.currentLocalName)) {
				this.currentRouteConfiguredForMessageRouteTag = attributes.getValue(ROUTE_CONFIGURED_FOR_MESSAGE_TAG);
				if (!this.currentRouteConfiguredForMessage.containsKey(this.currentRouteConfiguredForMessageRouteTag)) {
					this.currentRouteConfiguredForMessage.put(this.currentRouteConfiguredForMessageRouteTag, new HashSet<String>());
				}
			} else if (STOP.equals(this.currentLocalName)) {
				String stopTag = this.provider.cleanStopTag(attributes.getValue(STOP_TAG));
				this.currentRouteConfiguredForMessage.get(this.currentRouteConfiguredForMessageRouteTag).add(stopTag);
			} else if (TEXT.equals(this.currentLocalName)) { // ignore
			} else if (TEXT_SECONDARY_LANGUAGE.equals(this.currentLocalName)) { // ignore
			} else if (INTERVAL.equals(this.currentLocalName)) { // ignore
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

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (MESSAGE.equals(localName)) {
				if (this.currentTextSb.length() == 0 && this.currentTextSecondaryLanguageSb.length() == 0) {
					return; // no message
				}
				String textHtml = enhanceHtml(this.currentTextSb.toString(), this.textBoldWords);
				String textSecondaryHtml = enhanceHtml(this.currentTextSecondaryLanguageSb.toString(), this.textSecondaryBoldWords);
				if (this.currentRouteConfiguredForMessage.size() > 0) { // ROUTE(s)
					for (String routeTag : this.currentRouteConfiguredForMessage.keySet()) {
						if (this.currentRouteConfiguredForMessage.get(routeTag).size() == 0) {
							String targetUUID = NextBusProvider.getAgencyRouteTagTargetUUID(this.authority, routeTag);
							int severity = findRouteSeverity();
							addServiceUpdates(targetUUID, severity, textHtml, textSecondaryHtml);
						} else {
							for (String stopTag : this.currentRouteConfiguredForMessage.get(routeTag)) {
								String targetUUID = NextBusProvider.getAgencyRouteStopTagTargetUUID(this.authority, routeTag, stopTag);
								int severity = findStopPriority();
								addServiceUpdates(targetUUID, severity, textHtml, textSecondaryHtml);
							}
						}
					}
				} else if (this.currentRouteTag != null) {
					String targetUUID = NextBusProvider.getAgencyRouteTagTargetUUID(this.authority, this.currentRouteTag);
					int severity = findAgencySeverity();
					addServiceUpdates(targetUUID, severity, textHtml, textSecondaryHtml);
				} else if (this.currentRouteAll) { // AGENCY
					String targetUUID = NextBusProvider.getAgencyTargetUUID(this.authority);
					int severity = findAgencySeverity();
					addServiceUpdates(targetUUID, severity, textHtml, textSecondaryHtml);
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

		private void addServiceUpdates(String targetUUID, int severity, String textHtml, String textSecondaryHtml) {
			if (this.currentTextSb.length() > 0) {
				if (!this.textMessageIdTargetUUID.get(this.currentMessageId).contains(targetUUID)) {
					this.serviceUpdates.add(new ServiceUpdate(null, targetUUID, this.newLastUpdateInMs, this.serviceUpdateMaxValidityInMs, this.currentTextSb
							.toString(), textHtml, severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, this.textLanguageCode));
					this.textMessageIdTargetUUID.get(this.currentMessageId).add(targetUUID);
				}
			}
			if (this.currentTextSecondaryLanguageSb.length() > 0) {
				if (!this.textSecondaryMessageIdTargetUUID.get(this.currentMessageId).contains(targetUUID)) {
					this.serviceUpdates.add(new ServiceUpdate(null, targetUUID, this.newLastUpdateInMs, this.serviceUpdateMaxValidityInMs,
							this.currentTextSecondaryLanguageSb.toString(), textSecondaryHtml, severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL,
							this.textSecondaryLanguageCode));
					this.textSecondaryMessageIdTargetUUID.get(this.currentMessageId).add(targetUUID);
				}
			}
		}

		private String enhanceHtml(String originalHtml, Pattern boldWords) {
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

		private String enhanceHtmlBold(String html, Pattern regex) {
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
	}

	public static class NextBusDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = NextBusDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link NextBusDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "nextbus.db";

		/**
		 * Override if multiple {@link NextBusDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pNextBusMessagesLastUpdate";

		public static final String T_NEXT_BUS_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_NEXT_BUS_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_NEXT_BUS_SERVICE_UPDATE).build();

		private static final String T_NEXT_BUS_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEXT_BUS_SERVICE_UPDATE);

		public static final String T_NEXT_BUS_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_NEXT_BUS_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_NEXT_BUS_STATUS).build();

		private static final String T_NEXT_BUS_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEXT_BUS_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link NextBusDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.next_bus_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public NextBusDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_NEXT_BUS_SERVICE_UPDATE_SQL_DROP);
			db.execSQL(T_NEXT_BUS_STATUS_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_NEXT_BUS_SERVICE_UPDATE_SQL_CREATE);
			db.execSQL(T_NEXT_BUS_STATUS_SQL_CREATE);
		}
	}
}
