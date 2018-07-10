package org.mtransit.android.commons.provider;

public interface AgencyProviderContract extends ProviderContract {

	String ALL_PATH = "all";
	String VERSION_PATH = "version";
	String LABEL_PATH = "label";
	String COLOR_PATH = "color";
	String SHORT_NAME_PATH = "shortName";
	String DEPLOYED_PATH = "deployed";
	String SETUP_REQUIRED_PATH = "setupRequired";
	String AREA_PATH = "area";

	String AREA_MIN_LAT = "areaMinLat";
	String AREA_MAX_LAT = "areaMaxLat";
	String AREA_MIN_LNG = "areaMinLng";
	String AREA_MAX_LNG = "areaMaxLng";
}
