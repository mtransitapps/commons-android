package org.mtransit.android.commons.provider;

import java.util.Collection;

import org.mtransit.android.commons.data.ServiceUpdate;

import android.net.Uri;

public interface ServiceUpdateProviderContract extends ProviderContract {

	public Uri getAuthorityUri();

	public long getServiceUpdateMaxValidityInMs();

	public long getServiceUpdateValidityInMs(boolean inFocus);

	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus);

	public void cacheServiceUpdates(Collection<ServiceUpdate> newServiceUpdates);

	public Collection<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter);

	public Collection<ServiceUpdate> getNewServiceUpdates(ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter);

	public boolean deleteCachedServiceUpdate(Integer serviceUpdateId);

	public boolean deleteCachedServiceUpdate(String targetUUID, String sourceId);

	public boolean purgeUselessCachedServiceUpdates();

	public String getServiceUpdateDbTableName();

}
