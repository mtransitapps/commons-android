package org.mtransit.android.commons.provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.receiver.DataChange;

public class GTFSCurrentNextProvider implements MTLog.Loggable {

	private static final String LOG_TAG = GTFSCurrentNextProvider.class.getSimpleName();

	@NonNull
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

	@Nullable
	private static String currentNextData = null;

	private static final String PREF_KEY_CURRENT_NEXT_DATA = "pGTFSCurrentNextData";

	private static final String CURRENT_NEXT_DATA_UNKNOWN = "unknown";
	private static final String CURRENT_NEXT_DATA_CURRENT = "current";
	private static final String CURRENT_NEXT_DATA_NEXT = "next";
	private static final String CURRENT_NEXT_DATA_DEFAULT = CURRENT_NEXT_DATA_UNKNOWN;

	@NonNull
	private static String getCurrentNextData(@NonNull Context context) {
		if (currentNextData == null) {
			currentNextData = PreferenceUtils.getPrefLclNN(context, PREF_KEY_CURRENT_NEXT_DATA, CURRENT_NEXT_DATA_DEFAULT);
		}
		return currentNextData;
	}

	private static void setCurrentNextData(@NonNull Context context, @NonNull String newCurrentNextData) {
		if (newCurrentNextData.equals(currentNextData)) {
			return; // skip (same value)
		}
		currentNextData = newCurrentNextData;
		PreferenceUtils.savePrefLcl(context, PREF_KEY_CURRENT_NEXT_DATA, newCurrentNextData, false);
	}

	static boolean isNextData(@NonNull Context context) {
		checkForNextData(context);
		return CURRENT_NEXT_DATA_NEXT.equals(getCurrentNextData(context));
	}

	static void checkForNextData(@NonNull Context context) {
		final String oldCurrentNextData = getCurrentNextData(context);
		final int now = TimeUtils.currentTimeSec();
		boolean isNextData = hasNextData(context) //
				&& getCURRENT_LAST_DEPARTURE_IN_SEC(context) < now; // now AFTER current last departure
		// FIXME in GTFS specification, NEXT schedule overrides any previously uploaded CURRENT schedule
		final String newCurrentNextData = isNextData ? CURRENT_NEXT_DATA_NEXT : CURRENT_NEXT_DATA_CURRENT;
		if (CURRENT_NEXT_DATA_UNKNOWN.equals(oldCurrentNextData)) {
			MTLog.i(LOG_TAG, "Data: '%s' > '%s' #CurrentNext", oldCurrentNextData, newCurrentNextData);
			setCurrentNextData(context, newCurrentNextData); // 1st
		} else if (!newCurrentNextData.equals(oldCurrentNextData)) {
			MTLog.i(LOG_TAG, "Data: '%s' > '%s' #CurrentNext", oldCurrentNextData, newCurrentNextData);
			setCurrentNextData(context, newCurrentNextData); // 1st
			//noinspection ConstantConditions
			if (CURRENT_NEXT_DATA_CURRENT.equals(oldCurrentNextData) //
					&& CURRENT_NEXT_DATA_NEXT.equals(newCurrentNextData)) { // Current => Next
				broadcastNextDataChange(context); // 2nd
			} // ELSE DO NOTHING (DB version changed)
		}
	}

	private static void broadcastNextDataChange(@NonNull Context context) {
		MTLog.i(LOG_TAG, "Data: triggering switch to '%s'. #CurrentNext", getCurrentNextData(context));
		GTFSProvider.onCurrentNextDataChange(context);
		GTFSStatusProvider.onCurrentNextDataChange();
		DataChange.broadcastDataChange(context, GTFSProvider.getAUTHORITY(context), context.getPackageName(), true);
	}

	private static boolean hasNextData(@NonNull Context context) {
		return getNEXT_FIRST_DEPARTURE_IN_SEC(context) > 0 && getNEXT_LAST_DEPARTURE_IN_SEC(context) > 0;
	}

	static boolean hasCurrentData(@NonNull Context context) {
		return getCURRENT_FIRST_DEPARTURE_IN_SEC(context) > 0 && getCURRENT_LAST_DEPARTURE_IN_SEC(context) > 0;
	}
}
