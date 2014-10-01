package org.mtransit.android.commons.data;

import android.content.ContentValues;

public class BikeStationAvailabilityPercent extends AvailabilityPercent {

	private static final String TAG = BikeStationAvailabilityPercent.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public BikeStationAvailabilityPercent(int bikeStationId, String targetUUID, long lastUpdateMs, long maxValidityInMs, int value1Color, int value1ColorBg,
			int value2Color, int value2ColorBg) {
		this(null, bikeStationId, targetUUID, lastUpdateMs, maxValidityInMs, value1Color, value1ColorBg, value2Color, value2ColorBg);
	}

	public BikeStationAvailabilityPercent(Integer id, int bikeStationId, String targetUUID, long lastUpdateMs, long maxValidityInMs, int value1Color,
			int value1ColorBg, int value2Color, int value2ColorBg) {
		super(id, targetUUID, lastUpdateMs, maxValidityInMs);
		setValue1EmptyRes("no_bikes");
		setValue1QuantityRes("bikes_quantity");
		setValue1Color(value1Color);
		setValue1ColorBg(value1ColorBg);
		setValue2EmptyRes("no_docks");
		setValue2QuantityRes("docks_quantity");
		setValue2Color(value2Color);
		setValue2ColorBg(value2ColorBg);
	}

	public void setBikeStationId(int bikeStationId) {
	}

}
