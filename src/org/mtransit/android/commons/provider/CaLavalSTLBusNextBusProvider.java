package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.data.RouteTripStop;

import android.annotation.SuppressLint;

@SuppressLint("Registered")
public class CaLavalSTLBusNextBusProvider extends NextBusProvider {

	private static final String TAG = CaLavalSTLBusNextBusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String WEST_TRIP_HEADSIGN_VALUE = "W";
	private static final String WEST_FR_TRIP_HEADSIGN_VALUE = "O";

	@Override
	public String getRouteTag(RouteTripStop rts) {
		StringBuilder sb = new StringBuilder();
		sb.append(rts.getRoute().getShortName());
		String tripHeadsingValue = rts.getTrip().getHeadsignValue(); // N / S / E / W / O
		if (WEST_TRIP_HEADSIGN_VALUE.equals(tripHeadsingValue)) {
			tripHeadsingValue = WEST_FR_TRIP_HEADSIGN_VALUE;
		}
		sb.append(tripHeadsingValue);
		return sb.toString();
	}

	private static final String CP = "CP";

	@Override
	public String cleanStopTag(String stopTag) {
		if (stopTag.startsWith(CP)) {
			stopTag = stopTag.substring(2);
		}
		return stopTag;
	}
}
