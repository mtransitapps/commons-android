package org.mtransit.android.commons.provider;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.receiver.DataChange;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GTFSCurrentNextProvider implements MTLog.Loggable {

	private static final String LOG_TAG = GTFSCurrentNextProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private static Integer nextFirstDepartureInSec = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	private static Integer getNEXT_FIRST_DEPARTURE_IN_SEC(@NonNull Context context) {
		if (nextFirstDepartureInSec == null) {
			nextFirstDepartureInSec = context.getResources().getInteger(R.integer.next_gtfs_rts_first_departure_in_sec);
		}
		return nextFirstDepartureInSec;
	}

	@Nullable
	private static Integer nextLastDepartureInSec = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	private static Integer getNEXT_LAST_DEPARTURE_IN_SEC(@NonNull Context context) {
		if (nextLastDepartureInSec == null) {
			nextLastDepartureInSec = context.getResources().getInteger(R.integer.next_gtfs_rts_last_departure_in_sec);
		}
		return nextLastDepartureInSec;
	}

	@Nullable
	private static Integer currentFirstDepartureInSec = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	private static Integer getCURRENT_FIRST_DEPARTURE_IN_SEC(@NonNull Context context) {
		if (currentFirstDepartureInSec == null) {
			currentFirstDepartureInSec = context.getResources().getInteger(R.integer.current_gtfs_rts_first_departure_in_sec);
		}
		return currentFirstDepartureInSec;
	}

	@Nullable
	private static Integer currentLastDepartureInSec = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	private static Integer getCURRENT_LAST_DEPARTURE_IN_SEC(@NonNull Context context) {
		if (currentLastDepartureInSec == null) {
			currentLastDepartureInSec = context.getResources().getInteger(R.integer.current_gtfs_rts_last_departure_in_sec);
		}
		return currentLastDepartureInSec;
	}

	public static boolean isCurrentData(@NonNull Context context) {
		return hasCurrentData(context) //
				&& !isNextData(context);
	}

	@Nullable
	private static Boolean isNextData = null;

	public static boolean isNextData(@NonNull Context context) {
		checkForNextData(context);
		return isNextData != null && isNextData;
	}

	public static void checkForNextData(@NonNull Context context) {
		if (isNextData != null && isNextData) {
			return; // once next data is true, always true
		}
		if (!hasNextData(context)) {
			return; // no next data, always false
		}
		boolean isNextDataNew = hasNextData(context) //
				&& getCURRENT_LAST_DEPARTURE_IN_SEC(context) < TimeUtils.currentTimeSec(); // now AFTER current last departure
		if (isNextData == null) {
			isNextData = isNextDataNew;
		} else if (isNextData != isNextDataNew) {
			isNextData = isNextDataNew; // 1st
			broadcastNextDataChange(context); // 2nd
		}
	}

	private static void broadcastNextDataChange(@NonNull Context context) {
		GTFSProvider.onCurrentNextDataChange(context);
		GTFSStatusProvider.onCurrentNextDataChange();
		DataChange.broadcastDataChange(context, GTFSProvider.getAUTHORITY(context), context.getPackageName(), true);
	}

	@SuppressWarnings("WeakerAccess")
	public static boolean hasNextData(@NonNull Context context) {
		return getNEXT_FIRST_DEPARTURE_IN_SEC(context) > 0 && getNEXT_LAST_DEPARTURE_IN_SEC(context) > 0;
	}

	public static boolean hasCurrentData(@NonNull Context context) {
		return getCURRENT_FIRST_DEPARTURE_IN_SEC(context) > 0 && getCURRENT_LAST_DEPARTURE_IN_SEC(context) > 0;
	}
}
