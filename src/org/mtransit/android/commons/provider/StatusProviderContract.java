package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.data.POIStatus;

import android.net.Uri;

public interface StatusProviderContract extends ProviderContract {

	public long getStatusMaxValidityInMs();

	public long getStatusValidityInMs();

	public long getMinDurationBetweenRefreshInMs();

	public POIStatus getNewStatus(StatusFilter filter);

	public void cacheStatus(POIStatus newStatusToCache);

	public POIStatus getCachedStatus(String targetUUID);

	public boolean purgeUselessCachedStatuses();

	public Uri getAuthorityUri();

	public int getStatusType();

	public String getStatusDbTableName();

}
