package org.mtransit.android.commons.data;

import org.mtransit.android.commons.MTLog;

public class BikeStation extends DefaultPOI implements POI, MTLog.Loggable {

	private static final String TAG = BikeStation.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}


	private BikeStation(String authority) {
		super(authority, POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT);
	}
}
