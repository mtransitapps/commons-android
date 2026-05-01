package org.mtransit.android.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public class PreferenceUtils {

	private static final String LCL_PREF_NAME = "lcl";

	@WorkerThread
	@NonNull
	public static SharedPreferences getPrefDefault(@NonNull Context context) {
		//noinspection deprecation
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	@WorkerThread
	@NonNull
	public static SharedPreferences getPrefLcl(@NonNull Context context) {
		return context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE);
	}
}
