package org.mtransit.android.commons.data;

public class BikeStationAvailabilityPercent extends AvailabilityPercent {

	private static final String LOG_TAG = BikeStationAvailabilityPercent.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public BikeStationAvailabilityPercent(String targetUUID, long lastUpdateMs, long maxValidityInMs, long readFromSourceAtInMs, int value1Color,
			int value1ColorBg, int value2Color, int value2ColorBg) {
		this(null, targetUUID, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, value1Color, value1ColorBg, value2Color, value2ColorBg);
	}

	public BikeStationAvailabilityPercent(Integer id, String targetUUID, long lastUpdateMs, long maxValidityInMs, long readFromSourceAtInMs, int value1Color,
			int value1ColorBg, int value2Color, int value2ColorBg) {
		super(id, targetUUID, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs);
		setValue1EmptyRes("no_bikes");
		setValue1QuantityRes("bikes_quantity");
		setValue1Color(value1Color);
		setValue1ColorBg(value1ColorBg);
		setValue2EmptyRes("no_docks");
		setValue2QuantityRes("docks_quantity");
		setValue2Color(value2Color);
		setValue2ColorBg(value2ColorBg);
	}
}
