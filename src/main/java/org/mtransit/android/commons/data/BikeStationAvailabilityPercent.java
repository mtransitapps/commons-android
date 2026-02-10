package org.mtransit.android.commons.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class BikeStationAvailabilityPercent extends AvailabilityPercent {

	private static final String LOG_TAG = BikeStationAvailabilityPercent.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	// R8: commons-android/src/main/res/raw/org_mtransit_android_keep_poi_status_availability.xml
	public BikeStationAvailabilityPercent(@Nullable Integer id, @NonNull String targetUUID,
			long lastUpdateMs, long maxValidityInMs, long readFromSourceAtInMs, @Nullable String sourceLabel,
			int value1Color, int value1ColorBg,
			@Nullable Integer value1SubValue1Color, @Nullable Integer value1SubValue1ColorBg,
			int value2Color, int value2ColorBg) {
		super(id, targetUUID, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, false);
		setValue1EmptyRes("no_bikes"); // WARNING: R8
		setValue1QuantityRes("bikes_quantity"); // WARNING: R8
		setValue1Color(value1Color);
		setValue1ColorBg(value1ColorBg);
		setValue1SubValueDefaultEmptyRes("no_default_bikes"); // WARNING: R8
		setValue1SubValueDefaultQuantityRes("default_bikes_quantity"); // WARNING: R8
		setValue1SubValue1EmptyRes("no_e_bikes"); // WARNING: R8
		setValue1SubValue1QuantityRes("e_bikes_quantity"); // WARNING: R8
		setValue1SubValue1Color(value1SubValue1Color);
		setValue1SubValue1ColorBg(value1SubValue1ColorBg);
		setValue2EmptyRes("no_docks"); // WARNING: R8
		setValue2QuantityRes("docks_quantity"); // WARNING: R8
		setValue2Color(value2Color);
		setValue2ColorBg(value2ColorBg);
	}
}
