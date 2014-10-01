package org.mtransit.android.commons;

import android.os.Bundle;
import android.os.Parcelable;

public final class BundleUtils implements MTLog.Loggable {

	private static final String TAG = BundleUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static Integer getInt(String key, Bundle... bundles) {
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (bundle != null && bundle.containsKey(key)) {
					return bundle.getInt(key);
				}
			}
		}
		MTLog.w(TAG, "Can't find the int value for key '%s' (returned null)", key);
		return null;
	}

	public static String getString(String key, Bundle... bundles) {
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (bundle != null && bundle.containsKey(key)) {
					return bundle.getString(key);
				}
			}
		}
		MTLog.w(TAG, "Can't find the string value for key '%s' (returned null)", key);
		return null;
	}

	public static Float getFloat(String key, Bundle... bundles) {
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (bundle != null && bundle.containsKey(key)) {
					return bundle.getFloat(key);
				}
			}
		}
		MTLog.w(TAG, "Can't find the float value for key '%s' (returned null)", key);
		return null;
	}

	public static <T extends Parcelable> T getParcelable(String key, Bundle... bundles) {
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (bundle != null && bundle.containsKey(key)) {
					return bundle.getParcelable(key);
				}
			}
		}
		MTLog.w(TAG, "Can't find the parcelable value for key '%s' (returned null)", key);
		return null;
	}

}
