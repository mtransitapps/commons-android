package org.mtransit.android.commons;

import org.mtransit.android.commons.task.MTAsyncTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {

	private static final String TAG = PreferenceUtils.class.getSimpleName();

	public static final String LCL_PREF_NAME = "lcl";

	public static final String PREFS_DISTANCE_UNIT = "pDistanceUnit";
	public static final String PREFS_DISTANCE_UNIT_METRIC = "metric";
	public static final String PREFS_DISTANCE_UNIT_IMPERIAL = "imperial";
	public static final String PREFS_DISTANCE_UNIT_DEFAULT = PREFS_DISTANCE_UNIT_METRIC; // TODO smarter default

	public static final String PREFS_LCL_NEARBY_TAB_TYPE = "pNearbyTabType";

	public static final int PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT = 0; // 1st tab index

	public static String getPrefDefault(Context context, String prefKey, String defaultValue) {
		if (context == null) {
			MTLog.w(TAG, "Context null, using default value '%s' for preference '%s'!", defaultValue, prefKey);
			return defaultValue;
		}
		return getPref(PreferenceManager.getDefaultSharedPreferences(context), prefKey, defaultValue);
	}

	private static String getPref(SharedPreferences sharedPreferences, String prefKey, String defaultValue) {
		return sharedPreferences.getString(prefKey, defaultValue);
	}

	public static int getPrefLcl(Context context, String prefKey, int defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	public static long getPrefLcl(Context context, String prefKey, long defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	private static int getPref(SharedPreferences sharedPreferences, String prefKey, int defaultValue) {
		return sharedPreferences.getInt(prefKey, defaultValue);
	}

	private static long getPref(SharedPreferences sharedPreferences, String prefKey, long defaultValue) {
		return sharedPreferences.getLong(prefKey, defaultValue);
	}

	public static void savePrefLcl(final Context context, final String prefKey, final int newValue, final boolean sync) {
		if (sync) {
			savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
		} else {
			new MTAsyncTask<Void, Void, Void>() {
				@Override
				public String getLogTag() {
					return TAG;
				}

				@Override
				protected Void doInBackgroundMT(Void... params) {
					savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
					return null;
				}

			}.execute();
		}
	}

	public static void savePrefLcl(final Context context, final String prefKey, final long newValue, final boolean sync) {
		if (sync) {
			savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
		} else {
			new MTAsyncTask<Void, Void, Void>() {

				@Override
				public String getLogTag() {
					return TAG;
				}

				@Override
				protected Void doInBackgroundMT(Void... params) {
					savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
					return null;
				}

			}.execute();
		}
	}

	private static void savePref(Context context, final SharedPreferences sharedPreferences, String prefKey, int newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(prefKey, newValue);
		editor.apply();
	}

	private static void savePref(Context context, final SharedPreferences sharedPreferences, String prefKey, long newValue) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong(prefKey, newValue);
		editor.apply();
	}

}
