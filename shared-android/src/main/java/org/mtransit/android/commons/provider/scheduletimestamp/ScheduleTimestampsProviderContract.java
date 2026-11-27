package org.mtransit.android.commons.provider.scheduletimestamp;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ScheduleTimestamps;
import org.mtransit.android.commons.provider.common.ProviderContract;

public interface ScheduleTimestampsProviderContract extends ProviderContract {

	String SCHEDULE_TIMESTAMPS_PATH = "schedule";

	class Columns {
		public static final String T_SCHEDULE_TIMESTAMPS_K_TARGET_UUID = "target";
		public static final String T_SCHEDULE_TIMESTAMPS_K_EXTRAS = "extras";
		public static final String T_SCHEDULE_TIMESTAMPS_K_STARTS_AT = "startsAt";
		public static final String T_SCHEDULE_TIMESTAMPS_K_ENDS_AT = "endsAt";
	}

	@NonNull
	ScheduleTimestamps getScheduleTimestamps(@NonNull Filter scheduleTimestampsFilter);

	@SuppressWarnings("WeakerAccess")
	class Filter implements MTLog.Loggable {

		protected static final String LOG_TAG = ScheduleTimestampsProviderContract.class.getSimpleName() + ">" + Filter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		protected final RouteDirectionStop rds;
		protected final long startsAtInMs;
		protected final long endsAtInMs;

		public Filter(@NonNull RouteDirectionStop rds, long startsAtInMs, long endsAtInMs) {
			this.rds = rds;
			this.startsAtInMs = startsAtInMs;
			this.endsAtInMs = endsAtInMs;
		}

		@NonNull
		public RouteDirectionStop getRouteDirectionStop() {
			return this.rds;
		}

		public long getStartsAtInMs() {
			return startsAtInMs;
		}

		public long getEndsAtInMs() {
			return endsAtInMs;
		}

		@NonNull
		@Override
		public String toString() {
			return Filter.class.getSimpleName() + "{" +
					"rts=" + rds.getUUID() +
					", startsAtInMs=" + startsAtInMs +
					", endsAtInMs=" + endsAtInMs +
					'}';
		}
	}
}
