package org.mtransit.android.commons.api;

import android.support.annotation.NonNull;
import org.mtransit.android.commons.MTLog;

import android.os.Build;

public class SupportFactory implements MTLog.Loggable {

	private static final String TAG = SupportFactory.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static SupportUtil instance;

	public static SupportUtil get() {
		if (instance == null) {
			String className = SupportFactory.class.getPackage().getName();
			switch (Build.VERSION.SDK_INT) {
			case Build.VERSION_CODES.BASE: // unsupported versions
			case Build.VERSION_CODES.BASE_1_1:
			case Build.VERSION_CODES.CUPCAKE:
			case Build.VERSION_CODES.DONUT:
			case Build.VERSION_CODES.ECLAIR:
			case Build.VERSION_CODES.ECLAIR_0_1:
			case Build.VERSION_CODES.ECLAIR_MR1:
			case Build.VERSION_CODES.FROYO:
			case Build.VERSION_CODES.GINGERBREAD:
			case Build.VERSION_CODES.GINGERBREAD_MR1:
			case Build.VERSION_CODES.HONEYCOMB:
			case Build.VERSION_CODES.HONEYCOMB_MR1:
			case Build.VERSION_CODES.HONEYCOMB_MR2:
				MTLog.d(TAG, "Unknown API Level: " + Build.VERSION.SDK_INT);
			case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
				className += ".IceCreamSandwichSupport"; // 14
				break;
			case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
				className += ".IceCreamSandwichSupportMR1"; // 15
				break;
			case Build.VERSION_CODES.JELLY_BEAN:
				className += ".JellyBeanSupport"; // 16
				break;
			case Build.VERSION_CODES.JELLY_BEAN_MR1:
				className += ".JellyBeanSupportMR1"; // 17
				break;
			case Build.VERSION_CODES.JELLY_BEAN_MR2:
				className += ".JellyBeanSupportMR2"; // 18
				break;
			case Build.VERSION_CODES.KITKAT:
			case Build.VERSION_CODES.KITKAT_WATCH: // not really supporting this
				className += ".KitKatSupport"; // 19 (20)
				break;
			case Build.VERSION_CODES.LOLLIPOP:
				className += ".LollipopSupport"; // 21
				break;
			case Build.VERSION_CODES.LOLLIPOP_MR1:
				className += ".LollipopMR1Support"; // 22
				break;
			case Build.VERSION_CODES.M:
				className += ".MarshmallowSupport"; // 23
				break;
			case Build.VERSION_CODES.N:
				className += ".NougatSupport"; // 24
				break;
			case Build.VERSION_CODES.N_MR1:
				className += ".NougatSupportMR1"; // 25
				break;
			case Build.VERSION_CODES.O:
				className += ".OreoSupport"; // 26
				break;
			case Build.VERSION_CODES.O_MR1:
				className += ".OreoSupportMR1"; // 27
				break;
			case Build.VERSION_CODES.P:
				className += ".PieSupport"; // 28
				break;
			default:
				MTLog.w(TAG, "Unknown API Level: %s", Build.VERSION.SDK_INT);
				className += ".PieSupport"; // default for newer SDK
				break;
			}
			try {
				Class<?> detectorClass = Class.forName(className);
				instance = (SupportUtil) detectorClass.getConstructor().newInstance();
			} catch (Exception e) {
				MTLog.e(TAG, e, "INTERNAL ERROR!");
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
}
