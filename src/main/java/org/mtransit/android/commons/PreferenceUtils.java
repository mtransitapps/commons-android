package org.mtransit.android.commons;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.mtransit.android.commons.task.MTAsyncTask;

import java.util.Set;

// TODO @Deprecated
@SuppressWarnings("deprecation")
public class PreferenceUtils {

	private static final String LOG_TAG = PreferenceUtils.class.getSimpleName();

	private static final String LCL_PREF_NAME = "lcl";


	// region Common

	@WorkerThread
	private static int getPref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, int defaultValue) {
		return sharedPreferences.getInt(prefKey, defaultValue);
	}

	@WorkerThread
	private static boolean getPref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, boolean defaultValue) {
		return sharedPreferences.getBoolean(prefKey, defaultValue);
	}

	@WorkerThread
	private static long getPref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, long defaultValue) {
		return sharedPreferences.getLong(prefKey, defaultValue);
	}

	@Nullable
	@WorkerThread
	private static Set<String> getPref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Set<String> defaultValue) {
		return sharedPreferences.getStringSet(prefKey, defaultValue);
	}

	@WorkerThread
	private static String getPref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable String defaultValue) {
		return sharedPreferences.getString(prefKey, defaultValue);
	}

	@WorkerThread
	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Integer newValue) {
		final SharedPreferences.Editor editor = sharedPreferences.edit();
		if (newValue == null) {
			editor.remove(prefKey);
		} else {
			editor.putInt(prefKey, newValue);
		}
		editor.apply();
	}

	@WorkerThread
	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Boolean newValue) {
		final SharedPreferences.Editor editor = sharedPreferences.edit();
		if (newValue == null) {
			editor.remove(prefKey);
		} else {
			editor.putBoolean(prefKey, newValue);
		}
		editor.apply();
	}

	@WorkerThread
	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable Long newValue) {
		final SharedPreferences.Editor editor = sharedPreferences.edit();
		if (newValue == null) {
			editor.remove(prefKey);
		} else {
			editor.putLong(prefKey, newValue);
		}
		editor.apply();
	}

	@WorkerThread
	private static void savePref(@NonNull SharedPreferences sharedPreferences, @NonNull String prefKey, @Nullable String newValue) {
		final SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(prefKey, newValue);
		editor.apply();
	}


	// endregion Common

	// region Default

	// endregion Default

	// region Local

	@WorkerThread
	public static long getPrefLcl(@Nullable Context context, @NonNull String prefKey, long defaultValue) {
		if (context == null) return defaultValue;
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@WorkerThread
	@Nullable
	public static String getPrefLcl(@Nullable Context context, @NonNull String prefKey, @Nullable String defaultValue) {
		if (context == null) return defaultValue;
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@WorkerThread
	@NonNull
	public static String getPrefLclNN(@NonNull Context context, @NonNull String prefKey, @NonNull String defaultValue) {
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@WorkerThread
	@Nullable
	public static Set<String> getPrefLcl(@Nullable Context context, @NonNull String prefKey, @Nullable Set<String> defaultValue) {
		if (context == null) return defaultValue;
		return getPref(getPrefLcl(context), prefKey, defaultValue);
	}

	@WorkerThread
	@NonNull
	public static SharedPreferences getPrefLcl(@NonNull Context context) {
		return context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE);
	}

	@WorkerThread
	public static boolean hasPrefLcl(@Nullable Context context, @NonNull String prefKey) {
		if (context == null) return false;
		return getPrefLcl(context).contains(prefKey);
	}

	@WorkerThread
	public static void savePrefLclSync(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Integer newValue) {
		if (context == null) return;
		savePref(getPrefLcl(context), prefKey, newValue);
	}

	@AnyThread
	public static void savePrefLclAsync(@NonNull Context context, @NonNull String prefKey, @Nullable Integer newValue) {
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefDefaultAsync";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	@WorkerThread
	public static void savePrefLclSync(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Boolean newValue) {
		if (context == null) return;
		savePref(getPrefLcl(context), prefKey, newValue);
	}

	@WorkerThread
	public static void savePrefLclSync(@Nullable final Context context, @NonNull final String prefKey, @Nullable final Long newValue) {
		if (context == null) return;
		savePref(getPrefLcl(context), prefKey, newValue);
	}

	@AnyThread
	public static void savePrefLclAsync(@NonNull Context context, @NonNull String prefKey, @Nullable Long newValue) {
		new MTAsyncTask<Void, Void, Void>() {
			@NonNull
			@Override
			public String getLogTag() {
				return LOG_TAG + ">savePrefLclAsync";
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				savePref(getPrefLcl(context), prefKey, newValue);
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR);
	}

	@WorkerThread
	public static void savePrefLclSync(@Nullable final Context context, @NonNull final String prefKey, @Nullable final String newValue) {
		if (context == null) return;
		savePref(getPrefLcl(context), prefKey, newValue);
	}


	// endregion Local
}
