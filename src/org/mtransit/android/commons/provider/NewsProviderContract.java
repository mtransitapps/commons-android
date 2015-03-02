package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.HashMap;

import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.NewsProvider.NewsFilter;

import android.database.Cursor;
import android.net.Uri;

public interface NewsProviderContract extends ProviderContract {

	String getAuthority();

	Uri getAuthorityUri();

	Cursor getNewsFromDB(NewsFilter newsFilter);

	String getNewsDbTableName();

	String[] getNewsProjection();

	HashMap<String, String> getNewsProjectionMap();

	void cacheNews(ArrayList<News> newNews);

	ArrayList<News> getCachedNews(NewsProvider.NewsFilter newsFilter);

	ArrayList<News> getNewNews(NewsProvider.NewsFilter newsFilter);

	boolean purgeUselessCachedNews();

	boolean deleteCachedNews(Integer id);

	long getNewsMaxValidityInMs();

	long getNewsValidityInMs(boolean inFocusOrDefault);

	long getMinDurationBetweenNewsRefreshInMs(boolean inFocusOrDefault);
}
