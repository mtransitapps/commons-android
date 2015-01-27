package org.mtransit.android.commons;

import org.mtransit.android.commons.task.MTAsyncTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {

	private static final String TAG = PreferenceUtils.class.getSimpleName();

	public static final String LCL_PREF_NAME = "lcl";

	public static final String PREFS_UNITS = "pUnits";
	public static final String PREFS_UNITS_METRIC = "metric";
	public static final String PREFS_UNITS_IMPERIAL = "imperial";
	public static final String PREFS_UNITS_DEFAULT = PREFS_UNITS_METRIC;

	public static final String PREFS_LCL_NEARBY_TAB_TYPE = "pNearbyTabType";

	public static final int PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT = -1;

	private static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY = "pAgencyTypeTabAgency";

	public static String getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(int typeId) {
		return PREFS_LCL_AGENCY_TYPE_TAB_AGENCY + typeId;
	}

	public static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT = null;

	private static final String PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB = "pRTSRouteTripIdTab";

	public static String getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(String authority, long routeId) {
		return PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB + authority + routeId;
	}

	public static final long PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT = -1l;

	private static final String PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID = "pRTSRouteShowingListInsteadOfGrid";

	public static final String PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET = PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID + "LastSet";

	public static String getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(String authority) {
		return PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID + authority;
	}

	public static final boolean PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT = true;

	public static final boolean PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = true;

	private static final String PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP = "pAgencyPoisShowingListInsteadOfMap";

	public static final String PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET = PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP + "LastSet";

	public static String getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(String authority) {
		return PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP + authority;
	}

	public static final boolean PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = true;

	public static final String PREFS_LCL_ROOT_SCREEN_ITEM_ID = "pRootScreenItemId";

	public static final String PREFS_LCL_ROOT_SCREEN_ITEM_ID_DEFAULT = null; // worst default

	public static SharedPreferences getPrefDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static String getPrefDefault(Context context, String prefKey, String defaultValue) {
		if (context == null) {
			MTLog.w(TAG, "Context null, using default value '%s' for preference '%s'!", defaultValue, prefKey);
			return defaultValue;
		}
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	public static boolean getPrefDefault(Context context, String prefKey, boolean defaultValue) {
		if (context == null) {
			MTLog.w(TAG, "Context null, using default value '%s' for preference '%s'!", defaultValue, prefKey);
			return defaultValue;
		}
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	public static int getPrefLcl(Context context, String prefKey, int defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	public static long getPrefLcl(Context context, String prefKey, long defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	public static String getPrefLcl(Context context, String prefKey, String defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	private static int getPref(SharedPreferences sharedPreferences, String prefKey, int defaultValue) {
		return sharedPreferences.getInt(prefKey, defaultValue);
	}

	private static boolean getPref(SharedPreferences sharedPreferences, String prefKey, boolean defaultValue) {
		return sharedPreferences.getBoolean(prefKey, defaultValue);
	}

	private static long getPref(SharedPreferences sharedPreferences, String prefKey, long defaultValue) {
		return sharedPreferences.getLong(prefKey, defaultValue);
	}

	private static String getPref(SharedPreferences sharedPreferences, String prefKey, String defaultValue) {
		return sharedPreferences.getString(prefKey, defaultValue);
	}

	public static void savePrefDefault(final Context context, final String prefKey, final boolean newValue, final boolean sync) {
		if (sync) {
			savePref(getPrefDefault(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefDefault(context), prefKey, newValue);
				return null;
			}

		}.execute();
	}

	public static void savePrefLcl(final Context context, final String prefKey, final int newValue, final boolean sync) {
		if (sync) {
			savePref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
				return null;
			}
		}.execute();
	}

	public static void savePrefLcl(final Context context, final String prefKey, final long newValue, final boolean sync) {
		if (sync) {
			savePref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
				return null;
			}
		}.execute();
	}

	public static void savePrefLcl(final Context context, final String prefKey, final String newValue, final boolean sync) {
		if (sync) {
			savePref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
				return null;
			}
		}.execute();
	}

	private static void savePref(SharedPreferences sharedPreferences, String prefKey, int newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(prefKey, newValue);
		editor.apply();
	}

	private static void savePref(SharedPreferences sharedPreferences, String prefKey, boolean newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(prefKey, newValue);
		editor.apply();
	}

	private static void savePref(SharedPreferences sharedPreferences, String prefKey, long newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong(prefKey, newValue);
		editor.apply();
	}

	private static void savePref(SharedPreferences sharedPreferences, String prefKey, String newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(prefKey, newValue);
		editor.apply();
	}

}
