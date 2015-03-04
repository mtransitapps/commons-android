package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.data.ScheduleTimestampsFilter;

public interface ScheduleTimestampsProviderContract extends ProviderContract {

	public static final String SCHEDULE_TIMESTAMPS_PATH = "schedule";

	public ScheduleTimestamps getScheduleTimestamps(ScheduleTimestampsFilter scheduleTimestampsFilter);

}
