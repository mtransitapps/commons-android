package org.mtransit.android.commons.provider;

import java.util.Map;

import android.database.Cursor;

public interface POIProviderContract extends ProviderContract {

	Cursor getPOI(POIFilter poiFilter);

	Cursor getPOIFromDB(POIFilter poiFilter);

	Map<String, String> getPOIProjectionMap();

	String[] getPOIProjection();

	String getPOITable();

	Cursor getSearchSuggest(String query);

	String getSearchSuggestTable();

	Map<String, String> getSearchSuggestProjectionMap();
}
