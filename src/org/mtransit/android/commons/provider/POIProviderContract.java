package org.mtransit.android.commons.provider;

import java.util.HashMap;

import android.database.Cursor;

public interface POIProviderContract extends ProviderContract {

	public static final String POI_PATH = "poi";

	Cursor getPOI(POIFilter poiFilter);

	Cursor getPOIFromDB(POIFilter poiFilter);

	HashMap<String, String> getPOIProjectionMap();

	String[] getPOIProjection();

	String getPOITable();

	Cursor getSearchSuggest(String query);

	String getSearchSuggestTable();

	HashMap<String, String> getSearchSuggestProjectionMap();
}
