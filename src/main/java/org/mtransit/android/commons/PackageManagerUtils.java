package org.mtransit.android.commons;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class PackageManagerUtils {

	private static final String LOG_TAG = PackageManagerUtils.class.getSimpleName();

	public static void removeLauncherIcon(@NonNull Activity activity) {
		removeLauncherIcon(activity, activity.getClass());
	}

	public static void removeLauncherIcon(@Nullable Context context, @NonNull Class<?> activityClass) {
		try {
			if (context != null && activityClass.getCanonicalName() != null) {
				context.getPackageManager().setComponentEnabledSetting( //
						new ComponentName(context, activityClass), //
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED, //
						PackageManager.DONT_KILL_APP);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while removing launcher icon!");
		}
	}

	public static void resetLauncherIcon(@NonNull Activity activity) {
		resetLauncherIcon(activity, activity.getClass());
	}

	public static void resetLauncherIcon(@Nullable Context context, @NonNull Class<?> activityClass) {
		try {
			if (context != null && activityClass.getCanonicalName() != null) {
				context.getPackageManager().setComponentEnabledSetting( //
						new ComponentName(context, activityClass), //
						PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, //
						PackageManager.DONT_KILL_APP);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while adding launcher icon!");
		}
	}

	public static void openApp(@NonNull Context context, @NonNull String pkg, @Nullable int... intentFlags) {
		try {
			Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
			if (intent == null) {
				throw new PackageManager.NameNotFoundException();
			}
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			if (intentFlags != null) {
				for (int intentFlag : intentFlags) {
					intent.addFlags(intentFlag);
				}
			}
			context.startActivity(intent);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while opening the application!");
		}
	}

	//	adb shell pm disable-user --user 0 org.mtransit.android...
	//	adb shell pm list users
	//	adb shell pm list packages -d
	//	https://developer.android.com/topic/performance/power/test-power
	public static boolean isAppEnabled(@NonNull Context context, @NonNull String pkg) {
		try {
			int appEnabledSetting = context.getPackageManager().getApplicationEnabledSetting(pkg);
			return appEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
					|| appEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		} catch (IllegalArgumentException e) {
			return false; // app does not exist
		}
	}

	public static int getAppEnabledState(@NonNull Context context, @NonNull String pkg) {
		try {
			return context.getPackageManager().getApplicationEnabledSetting(pkg);
		} catch (IllegalArgumentException e) {
			return -1; // app does not exist
		}
	}

	public static boolean isAppInstalled(@NonNull Context context, @NonNull String pkg) {
		try {
			context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	/**
	 * Only works with apps pkg added to AndroidManifest.xml (API Level 30+)
	 */
	public static boolean isAppInstalledDefault(@NonNull Context context, @NonNull Intent intent) {
		try {
			ResolveInfo info = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY); // works API Level 30+ because added to AndroidManifest.xml
			return info != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Requires {@link android.Manifest.permission#REQUEST_DELETE_PACKAGES}
	 * since {@link android.os.Build.VERSION_CODES#P}.
	 */
	public static void uninstall(@NonNull Activity activity, @NonNull Context context) {
		uninstallApp(activity, context.getPackageName());
	}

	/**
	 * Requires {@link android.Manifest.permission#REQUEST_DELETE_PACKAGES}
	 * since {@link android.os.Build.VERSION_CODES#P}.
	 */
	public static void uninstallApp(@NonNull Activity activity, @NonNull String pkg) {
		Uri uri = Uri.parse("package:" + pkg);
		Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
		activity.startActivity(intent);
	}

	@Nullable
	public static ProviderInfo[] findContentProvidersWithMetaData(@NonNull Context context, @Nullable String packageName) {
		if (TextUtils.isEmpty(packageName)) {
			return null;
		}
		List<PackageInfo> allInstalledProvidersWithMetaData = getAllInstalledProvidersWithMetaData(context.getPackageManager());
		if (allInstalledProvidersWithMetaData != null) {
			for (PackageInfo packageInfo : allInstalledProvidersWithMetaData) {
				if (packageInfo.packageName.equals(packageName)) {
					return packageInfo.providers;
				}
			}
		}
		return null;
	}

	@SuppressLint("QueryPermissionsNeeded")
	@Nullable
	private static List<PackageInfo> getAllInstalledProvidersWithMetaData(@NonNull PackageManager pm) {
		try {
			return pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while reading installed providers w/ meta-data!"); // #Android5 #Android6
			return null;
		}
	}

	@NonNull
	public static CharSequence getAppName(@NonNull Context context) {
		try {
			ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
			return context.getPackageManager().getApplicationLabel(appInfo);
		} catch (PackageManager.NameNotFoundException e) {
			MTLog.w(LOG_TAG, e, "Error while looking up app name!");
			return context.getString(R.string.ellipsis);
		}
	}

	@NonNull
	public static String getAppVersionName(@NonNull Context context) {
		return getAppVersionName(context, context.getPackageName());
	}

	@NonNull
	public static String getAppVersionName(@NonNull Context context, @NonNull String pkg) {
		try {
			return context.getPackageManager().getPackageInfo(pkg, 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			MTLog.w(LOG_TAG, e, "Error while looking up '%s' version name!", pkg);
			return context.getString(R.string.ellipsis);
		}
	}

	public static int getAppVersionCode(@NonNull Context context) {
		return getAppVersionCode(context, context.getPackageName());
	}

	public static int getAppVersionCode(@NonNull Context context, @NonNull String pkg) {
		try {
			return context.getPackageManager().getPackageInfo(pkg, 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			MTLog.w(LOG_TAG, e, "Error while looking up '%s' version code!", pkg);
			return -1;
		}
	}

	public static long getThisAppLongVersionCode(@NonNull Context context) {
		return getAppLongVersionCode(context, context.getPackageName());
	}

	public static long getAppLongVersionCode(@NonNull Context context, @NonNull String pkg) {
		try {
			return PackageInfoCompat.getLongVersionCode(context.getPackageManager().getPackageInfo(pkg, 0));
		} catch (PackageManager.NameNotFoundException e) {
			MTLog.w(LOG_TAG, e, "Error while looking up '%s' long version code!", pkg);
			return -1L;
		}
	}

	private PackageManagerUtils() {
	}
}
