package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.data.POIStatus;

import android.net.Uri;

public interface StatusProviderContract extends ProviderContract {

	public long getStatusMaxValidityInMs();

	public long getStatusValidityInMs(boolean inFocus);

	public long getMinDurationBetweenRefreshInMs(boolean inFocus);

	public POIStatus getNewStatus(StatusFilter statusFilter);

	public void cacheStatus(POIStatus newStatusToCache);

	public POIStatus getCachedStatus(StatusFilter statusFilter);

	public boolean purgeUselessCachedStatuses();

	public boolean deleteCachedStatus(int cachedStatusId);

	public Uri getAuthorityUri();

	public int getStatusType();

	public String getStatusDbTableName();

}
