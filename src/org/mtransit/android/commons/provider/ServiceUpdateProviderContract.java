package org.mtransit.android.commons.provider;

import java.util.ArrayList;

import org.mtransit.android.commons.data.ServiceUpdate;

import android.net.Uri;

public interface ServiceUpdateProviderContract extends ProviderContract {

	public static final String SERVICE_UPDATE_PATH = "service";

	public Uri getAuthorityUri();

	public long getServiceUpdateMaxValidityInMs();

	public long getServiceUpdateValidityInMs(boolean inFocus);

	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus);

	public void cacheServiceUpdates(ArrayList<ServiceUpdate> newServiceUpdates);

	public ArrayList<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter);

	public ArrayList<ServiceUpdate> getNewServiceUpdates(ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter);

	public boolean deleteCachedServiceUpdate(Integer serviceUpdateId);

	public boolean deleteCachedServiceUpdate(String targetUUID, String sourceId);

	public boolean purgeUselessCachedServiceUpdates();

	public String getServiceUpdateDbTableName();

	public String getServiceUpdateLanguage();

}
