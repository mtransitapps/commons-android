package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ScheduleTimestampsFilter;

public interface ScheduleTimestampsProviderContract extends ProviderContract {

	public ScheduleTimestamps getScheduleTimestamps(ScheduleTimestampsFilter scheduleTimestampsFilter);

}
