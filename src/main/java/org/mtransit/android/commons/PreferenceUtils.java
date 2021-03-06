package org.mtransit.android.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.task.MTAsyncTask;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"WeakerAccess", "unused", "deprecation"})
public class PreferenceUtils {

	private static final String LOG_TAG = PreferenceUtils.class.getSimpleName();

	private static final String LCL_PREF_NAME = "lcl";

	public static final String PREFS_THEME = "pTheme";
	public static final String PREFS_THEME_LIGHT = "light";
	public static final String PREFS_THEME_DARK = "dark";
	public static final String PREFS_THEME_SYSTEM_DEFAULT = "system_default";
	public static final String PREFS_THEME_DEFAULT = PREFS_THEME_SYSTEM_DEFAULT;

	public static final String PREFS_UNITS = "pUnits";
	public static final String PREFS_UNITS_METRIC = "metric";
	public static final String PREFS_UNITS_IMPERIAL = "imperial";
	public static final String PREFS_UNITS_DEFAULT = PREFS_UNITS_METRIC; // TODO smarter default?

	public static final String PREFS_USE_INTERNAL_WEB_BROWSER = "pUseInternalWebBrowser";
	public static final boolean PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT = true;

	public static final String PREFS_LCL_DEV_MODE_ENABLED = "pDevModeEnabled";
	public static final boolean PREFS_LCL_DEV_MODE_ENABLED_DEFAULT = false;

	public static final String PREF_USER_APP_OPEN_COUNTS = "pAppOpenCounts";
	public static final int PREF_USER_APP_OPEN_COUNTS_DEFAULT = 0;

	public static final String PREF_USER_LEARNED_DRAWER = "pUserLearnedDrawer";
	public static final boolean PREF_USER_LEARNED_DRAWER_DEFAULT = false;

	public static final String PREF_USER_REWARDED_UNTIL = "pRewardedUtil";
	public static final long PREF_USER_REWARDED_UNTIL_DEFAULT = 0L;

	public static final String PREFS_LCL_NEARBY_TAB_TYPE = "pNearbyTabType";
	public static final int PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT = -1;

	public static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT = null;
	private static final String PREFS_LCL_AGENCY_TYPE_TAB_AGENCY = "pAgencyTypeTabAgency";

	@NonNull
	public static String getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(int typeId) {
		return PREFS_LCL_AGENCY_TYPE_TAB_AGENCY + typeId;
	}

	public static final long PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT = -1L;
	private static final String PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB = "pRTSRouteTripIdTab";

	@NonNull
	public static String getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(@NonNull String authority, long routeId) {
		return PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB + authority + routeId;
	}

	public static final boolean PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT = true;
	private static final String PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID = "pRTSRouteShowingListInsteadOfGrid";
	public static final String PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_LAST_SET = PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID + "LastSet";

	@NonNull
	public static String getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(@NonNull String authority) {
		return PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID + authority;
	}

	public static final String PREFS_LCL_MAP_FILTER_TYPE_IDS = "pMapFilterTypeIds";
	public static final Set<String> PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT = new HashSet<>();
	public static final boolean PREFS_LCL_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = true;

	public static final boolean PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = true;
	private static final String PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP = "pAgencyPoisShowingListInsteadOfMap";
	public static final String PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET = PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP + "LastSet";

	@NonNull
	public static String getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(@NonNull String authority) {
		return PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP + authority;
	}

	public static final String PREFS_LCL_ROOT_SCREEN_ITEM_ID = "pRootScreenItemId";

	@SuppressWarnings("unused")
	public static final boolean PREFS_KEEP_MODULE_APP_LAUNCHER_ICON_DEFAULT = true;
	public static final String PREFS_KEEP_MODULE_APP_LAUNCHER_ICON = "pKeepModuleAppLauncherIcon";

	@NonNull
	public static SharedPreferences getPrefDefault(@NonNull Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static int getPrefDefault(@Nullable Context context, @NonNull String prefKey, int defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	public static long getPrefDefault(@Nullable Context context, @NonNull String prefKey, long defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	@Nullable
	public static String getPrefDefault(@Nullable Context context, @NonNull String prefKey, @Nullable String defaultValue) {
		if (context == null) {
			MTLog.w(LOG_TAG, "Context null, using default value '%s' for preference '%s'!", defaultValue, prefKey);
			return defaultValue;
		}
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	@NonNull
	public static String getPrefDefaultNN(@NonNull Context context, @NonNull String prefKey, @NonNull String defaultValue) {
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	public static boolean getPrefDefault(@Nullable Context context, @NonNull String prefKey, boolean defaultValue) {
		if (context == null) {
			MTLog.w(LOG_TAG, "Context null, using default value '%s' for preference '%s'!", defaultValue, prefKey);
			return defaultValue;
		}
		return getPref(getPrefDefault(context), prefKey, defaultValue);
	}

	public static boolean hasPrefDefault(@Nullable Context context, @NonNull String prefKey) {
		if (context == null) {
			return false;
		}
		return getPrefDefault(context).contains(prefKey);
	}

	public static int getPrefLcl(@Nullable Context context, @NonNull String prefKey, int defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	public static boolean getPrefLcl(@Nullable Context context, @NonNull String prefKey, boolean defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	public static long getPrefLcl(@Nullable Context context, @NonNull String prefKey, long defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@Nullable
	public static String getPrefLcl(@Nullable Context context, @NonNull String prefKey, @Nullable String defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@NonNull
	public static String getPrefLclNN(@NonNull Context context, @NonNull String prefKey, @NonNull String defaultValue) {
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@Nullable
	public static Set<String> getPrefLcl(@Nullable Context context, @NonNull String prefKey, @Nullable Set<String> defaultValue) {
		if (context == null) {
			return defaultValue;
		}
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@NonNull
	public static SharedPreferences getPrefLcl(@NonNull Context context) {
		return context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE);
	}

	public static boolean hasPrefLcl(@Nullable Context context, @NonNull String prefKey) {
		if (context == null) {
			return false;
		}
		return getPrefLcl(context).contains(prefKey);
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

	private static Set<String> getPref(SharedPreferences sharedPreferences, String prefKey, Set<String> defaultValue) {
		return sharedPreferences.getStringSet(prefKey, defaultValue);
	}

	private static String getPref(SharedPreferences sharedPreferences, String prefKey, String defaultValue) {
		return sharedPreferences.getString(prefKey, defaultValue);
	}

	public static void savePrefDefault(@Nullable final Context context, @NonNull final String prefKey, final int newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefDefault(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefDefault";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefDefault(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefDefault(@Nullable final Context context, @NonNull final String prefKey, final long newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefDefault(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefDefault";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefDefault(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefDefault(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Boolean newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefDefault(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefDefault";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefDefault(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefDefault(@Nullable final Context context, @NonNull final String prefKey, @Nullable final String newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefDefault(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">getPrefDefault";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefDefault(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefLcl(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Integer newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefLcl(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefLcl";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefLcl(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Boolean newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefLcl(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefLcl";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefLcl(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Long newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefLcl(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefLcl";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefLcl(@Nullable final Context context, @NonNull final String prefKey, @Nullable final String newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefLcl(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefLcl";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	public static void savePrefLcl(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Set<String> newValue, final boolean sync) {
		if (context == null) {
			return;
		}
		if (sync) {
			savePref(getPrefLcl(context), prefKey, newValue);
			return;
		}
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefLcl";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Integer newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		if (newValue == null) {
			editor.remove(prefKey);
		} else {
			editor.putInt(prefKey, newValue);
		}
		editor.apply();
	}

	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Boolean newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		if (newValue == null) {
			editor.remove(prefKey);
		} else {
			editor.putBoolean(prefKey, newValue);
		}
		editor.apply();
	}

	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Long newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		if (newValue == null) {
			editor.remove(prefKey);
		} else {
			editor.putLong(prefKey, newValue);
		}
		editor.apply();
	}

	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable String newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(prefKey, newValue);
		editor.apply();
	}

	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Set<String> newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putStringSet(prefKey, newValue);
		editor.apply();
	}
}
