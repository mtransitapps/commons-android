package org.mtransit.android.commons.provider;

import java.util.ArrayList;

import org.mtransit.android.commons.data.News;

import android.net.Uri;

public interface NewsProviderContract extends ProviderContract {

	public Uri getAuthorityUri();

	String getNewsDbTableName();

	String[] getNewsProjection();

	public void cacheNews(ArrayList<News> newNews);

	ArrayList<News> getCachedNews(NewsProvider.NewsFilter newsFilter);

	ArrayList<News> getNewNews(NewsProvider.NewsFilter newsFilter);

	boolean purgeUselessCachedNews();

	boolean deleteCachedNews(Integer id);

	long getNewsMaxValidityInMs();

	long getNewsValidityInMs(boolean inFocusOrDefault);

	long getMinDurationBetweenNewsRefreshInMs(boolean inFocusOrDefault);
}
