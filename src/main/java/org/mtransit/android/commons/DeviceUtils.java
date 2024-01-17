package org.mtransit.android.commons;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;

public final class DeviceUtils {

	private DeviceUtils() {
	}

	public static void showAppDetailsSettings(@NonNull Context context, @NonNull String pkg) {
		LinkUtils.open(context,
				new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
						Uri.parse("package:" + pkg)),
				null);
	}

	public static void showLocaleSettings(@NonNull Context context) {
		LinkUtils.open(context,
				new Intent(Settings.ACTION_LOCALE_SETTINGS),
				null);
	}

	public static void showDateSettings(@NonNull Context context) {
		LinkUtils.open(context,
				new Intent(Settings.ACTION_DATE_SETTINGS),
				null);
	}

	public static void showLocationSourceSettings(@NonNull Context context) {
		LinkUtils.open(context,
				new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
				null);
	}

	public static void showIgnoreBatteryOptimizationSettings(@NonNull Context context) {
		LinkUtils.open(context,
				new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
				null);
	}
}
