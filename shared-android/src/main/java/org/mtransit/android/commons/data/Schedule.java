package org.mtransit.android.commons.data;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.commons.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import kotlinx.serialization.SerializationException;
import kotlinx.serialization.json.JsonObject;

@SuppressWarnings("WeakerAccess")
// aka. [route+direction+stop]>trips>stop_times
public class Schedule extends POIStatus implements MTLog.Loggable {

	protected static final String LOG_TAG = Schedule.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final TimestampComparator TIMESTAMPS_COMPARATOR = new TimestampComparator();

	private static final FrequencyComparator FREQUENCIES_COMPARATOR = new FrequencyComparator();

	@NonNull
	private final List<Timestamp> timestamps = new ArrayList<>();

	private long providerPrecisionInMs;

	private long timestampsUntilInMs = -1L;

	private boolean noPickup;

	@Nullable
	private final String localTimeZoneId;

	@NonNull
	private final List<Frequency> frequencies = new ArrayList<>();

	public Schedule(@NonNull POIStatus status, long providerPrecisionInMs, boolean noPickup, @Nullable String localTimeZoneId) {
		this(
				status.getId(),
				status.getTargetUUID(),
				status.getLastUpdateInMs(),
				status.getMaxValidityInMs(),
				status.getReadFromSourceAtInMs(),
				providerPrecisionInMs,
				noPickup,
				localTimeZoneId,
				status.getSourceLabel(),
				status.isNoData()
		);
	}

	public Schedule(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateInMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			long providerPrecisionInMs,
			boolean noPickup,
			@Nullable String localTimeZoneId,
			@Nullable String sourceLabel
	) {
		this(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, providerPrecisionInMs, noPickup, localTimeZoneId, sourceLabel, false);
	}

	public Schedule(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateInMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			long providerPrecisionInMs,
			boolean noPickup,
			@Nullable String localTimeZoneId,
			@Nullable String sourceLabel,
			boolean noData
	) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_SCHEDULE, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, noData);
		this.providerPrecisionInMs = providerPrecisionInMs;
		this.noPickup = noPickup;
		this.localTimeZoneId = localTimeZoneId;
		resetTimestampsUntilInMs();
	}

	public boolean isNoPickup() {
		return noPickup;
	}

	@Nullable
	public String getLocalTimeZoneId() {
		return localTimeZoneId;
	}

	public long getProviderPrecisionInMs() {
		return providerPrecisionInMs;
	}

	public void setProviderPrecisionInMs(long providerPrecisionInMs) {
		this.providerPrecisionInMs = providerPrecisionInMs;
	}

	@NonNull
	@Override
	public String toString() {
		return ScheduleExtKt.toStringK(this);
	}

	@Nullable
	public static Schedule fromCursorWithExtra(@NonNull Cursor cursor) {
		return fromExtraJSONString(
				POIStatus.fromCursor(cursor),
				POIStatus.getExtrasFromCursor(cursor)
		);
	}

	@Nullable
	private static Schedule fromExtraJSONString(@NonNull POIStatus status, @NonNull String extrasJSONString) {
		try {
			return ScheduleExtKt.toSchedule(status, extrasJSONString);
		} catch (SerializationException se) {
			MTLog.w(LOG_TAG, se, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Nullable
	@Override
	public JsonObject getExtrasJson() {
		return ScheduleExtKt.toJson(this);
	}

	public void setNoPickup(boolean noPickup) {
		this.noPickup = noPickup;
	}

	@SuppressWarnings("unused")
	public void setNoPickupTimestamps(boolean noPickup) {
		for (Timestamp timestamp : this.timestamps) {
			if (noPickup) {
				if (!timestamp.isNoPickup()) {
					timestamp.setHeadsign(Direction.HEADSIGN_TYPE_NO_PICKUP, null);
				}
			} else {
				if (timestamp.isNoPickup()) {
					timestamp.setResetHeadsign();
				}
			}
		}
	}

	private void addFrequencyWithoutSort(Frequency newFrequency) {
		this.frequencies.add(newFrequency);
	}

	public void setFrequenciesAndSort(@NonNull List<Frequency> frequencies) {
		this.frequencies.clear();
		this.frequencies.addAll(frequencies);
		sortFrequencies();
	}

	private void sortFrequencies() {
		CollectionUtils.sort(this.frequencies, FREQUENCIES_COMPARATOR);
		resetTimestampsUntilInMs();
	}

	@NonNull
	public List<Frequency> getFrequencies() {
		return this.frequencies;
	}

	@SuppressWarnings("unused")
	public int getFrequenciesCount() {
		return this.frequencies.size();
	}

	public void addTimestampWithoutSort(@Nullable Timestamp newTimestamp) {
		if (newTimestamp == null) return;
		this.timestamps.add(newTimestamp);
	}

	public void setTimestampsAndSort(@NonNull List<Timestamp> timestamps) {
		this.timestamps.clear();
		this.timestamps.addAll(timestamps);
		sortTimestamps();
	}

	public boolean removeTimestamp(@NonNull Timestamp timestamp) {
		return this.timestamps.remove(timestamp);
	}

	public void sortTimestamps() {
		CollectionUtils.sort(this.timestamps, TIMESTAMPS_COMPARATOR);
		resetTimestampsUntilInMs();
	}

	@NonNull
	public List<Timestamp> getTimestamps() {
		return this.timestamps;
	}

	public int getTimestampsCount() {
		return this.timestamps.size();
	}

	protected static final long MIN_UI_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	protected long getUIProviderPrecisionInMs() {
		return Math.max(MIN_UI_PRECISION_IN_MS, this.providerPrecisionInMs);
	}

	private void resetTimestampsUntilInMs() {
		final int timestampsCount = getTimestampsCount();
		if (timestampsCount == 0) {
			this.timestampsUntilInMs = 0L; // NOT USEFUL
			return;
		}
		this.timestampsUntilInMs = this.timestamps.get(timestampsCount - 1).getDepartureT() + getUIProviderPrecisionInMs();
	}

	public long getTimestampsUntilInMs() {
		if (this.timestampsUntilInMs < 0L) {
			resetTimestampsUntilInMs();
		}
		return timestampsUntilInMs;
	}

	@Override
	public boolean isUseful() {
		return isValid()
				&& TimeUtils.currentTimeToTheMinuteMillis() < getTimestampsUntilInMs();
	}

	@Override
	public boolean isValid() {
		// 2026-07-22: GTFS-RT Trip Update status provider > used (1 min) "status" instead of "max status" (1 hour) // TODO remove 3 months after PR merged
		final long maxValidityInMs = Math.max(TimeUnit.MINUTES.toMillis(10L), getMaxValidityInMs());
		return TimeUtils.currentTimeMillis() <= getLastUpdateInMs() + maxValidityInMs;
	}

	public static class TimestampComparator implements Comparator<Timestamp> {
		@Override
		public int compare(Timestamp lhs, Timestamp rhs) {
			return Long.compare(
					lhs == null ? 0L : lhs.getDepartureT(),
					rhs == null ? 0L : rhs.getDepartureT()
			);
		}
	}

	private static class FrequencyComparator implements Comparator<Frequency> {
		@Override
		public int compare(Frequency lhs, Frequency rhs) {
			if (lhs.startTimeInMs == rhs.startTimeInMs) {
				return Long.compare(lhs.endTimeInMs, rhs.endTimeInMs);
			}
			return Long.compare(lhs.startTimeInMs, rhs.startTimeInMs);
		}
	}

	@SuppressWarnings("unused")
	public static class Frequency implements MTLog.Loggable {

		protected static final String LOG_TAG = Frequency.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		public final long startTimeInMs;
		public final long endTimeInMs;
		public final int headwayInSec;

		@Nullable
		private final Boolean oldSchedule;

		public Frequency(long startTimeInMs, long endTimeInMs, int headwayInSec) {
			this(startTimeInMs, endTimeInMs, headwayInSec, null);
		}

		public Frequency(long startTimeInMs, long endTimeInMs, int headwayInSec, @Nullable Boolean oldSchedule) {
			this.startTimeInMs = startTimeInMs;
			this.endTimeInMs = endTimeInMs;
			this.headwayInSec = headwayInSec;
			this.oldSchedule = oldSchedule;
		}

		public long getStartTimeInMs() {
			return startTimeInMs;
		}

		public long getEndTimeInMs() {
			return endTimeInMs;
		}

		public int getHeadwayInSec() {
			return headwayInSec;
		}

		@Nullable
		public Boolean getOldSchedule() {
			return this.oldSchedule;
		}

		boolean hasOldSchedule() {
			return this.oldSchedule != null;
		}

		public boolean isOldSchedule() {
			return Boolean.TRUE.equals(this.oldSchedule);
		}

		@NonNull
		@Override
		public String toString() {
			return Frequency.class.getSimpleName() + "{" +
					"startTimeInMs=" + startTimeInMs +
					", endTimeInMs=" + endTimeInMs +
					", headwayInSec=" + headwayInSec +
					", oldSchedule=" + oldSchedule +
					'}';
		}
	}

	public static class Timestamp implements MTLog.Loggable { // Stop Time

		protected static final String LOG_TAG = Timestamp.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private long departureInMs;
		private long originalDepartureDelayMs = 0L;
		@Direction.HeadSignType
		protected int headsignType = Direction.HEADSIGN_TYPE_NONE;
		@Nullable
		protected String headsignValue = null;
		@Nullable
		private final String localTimeZoneId; // TODO remove once migrated fully to Schedule TZ
		@Nullable
		private Boolean realTime = null;
		@Nullable
		private Boolean oldSchedule = null;
		@Nullable
		private Integer accessible = null;
		@Nullable
		private Boolean cancelled = null;
		@Nullable
		private String tripId = null; // cleaned trip ID (string) // initial used to store trip id INT but replaced after
		private int stopSequence = -1;
		@Nullable
		private Long departureArrivalDiffMs = null;
		private long originalArrivalDelayMs = 0L;

		@VisibleForTesting
		public Timestamp(long departureT) {
			this(departureT, (String) null);
		}

		public Timestamp(long departureT, @NonNull TimeZone localTimeZone) {
			this(departureT, localTimeZone.getID());
		}

		public Timestamp(long departureT, @Nullable String localTimeZoneId) {
			this.departureInMs = departureT;
			this.localTimeZoneId = localTimeZoneId;
		}

		public long getDepartureT() {
			return this.departureInMs;
		}

		public void setDepartureT(long departureT) {
			final long originalArrivalT = getArrivalT(); // stored as diff -> do not change
			this.departureInMs = departureT;
			setArrivalT(originalArrivalT); // stored as diff -> do not change
		}

		public long getOriginalDepartureDelayMs() {
			return originalDepartureDelayMs;
		}

		public void setOriginalDepartureDelayMs(long originalDepartureDelayMs) {
			this.originalDepartureDelayMs = originalDepartureDelayMs;
		}

		public long getArrivalT() {
			return getDepartureT() - (departureArrivalDiffMs == null ? 0L : departureArrivalDiffMs);
		}

		@Nullable
		public Long getArrivalTIfDifferent() {
			return departureArrivalDiffMs == null ? null : getDepartureT() - departureArrivalDiffMs;
		}

		public void setArrivalT(long arrivalT) {
			setDepartureArrivalDiffMs(getDepartureT() - arrivalT);
		}

		protected void setDepartureArrivalDiffMs(@Nullable Long departureArrivalDiffMs) {
			if (departureArrivalDiffMs != null && departureArrivalDiffMs == 0L) {
				departureArrivalDiffMs = null;
			}
			this.departureArrivalDiffMs = departureArrivalDiffMs;
		}

		@Nullable
		protected Long getDepartureArrivalDiffMs() {
			return departureArrivalDiffMs;
		}

		public long getOriginalArrivalDelayMs() {
			return originalArrivalDelayMs;
		}

		public void setOriginalArrivalDelayMs(long originalArrivalDelayMs) {
			this.originalArrivalDelayMs = originalArrivalDelayMs;
		}

		@NonNull
		public Timestamp setHeadsign(@Direction.HeadSignType int headsignType, @Nullable String headsignValue) {
			this.headsignType = headsignType;
			this.headsignValue = headsignValue;
			return this;
		}

		public void setResetHeadsign() {
			this.headsignType = Direction.HEADSIGN_TYPE_NONE;
			this.headsignValue = null;
		}

		@Discouraged(message = "should call getHeadsign()")
		@Nullable
		public String getHeadsignValue() {
			return headsignValue;
		}

		@Discouraged(message = "should call setHeadsign()")
		public void setHeadsignValue(@Nullable String headsignValue) {
			this.headsignValue = headsignValue;
		}

		public boolean hasHeadsign() {
			if (this.headsignType == Direction.HEADSIGN_TYPE_NO_PICKUP) {
				return true;
			}
			return this.headsignType != Direction.HEADSIGN_TYPE_NONE && !TextUtils.isEmpty(this.headsignValue);
		}

		@SuppressWarnings("unused") // main app
		@NonNull
		public String getUIHeading(@NonNull Context context, boolean small) {
			final String headSignUC = getHeading(context);
			if (!headSignUC.isEmpty() && !Character.isLetterOrDigit(headSignUC.charAt(0))) {
				return headSignUC; // not direction
			}
			if (isNoPickup()) {
				return headSignUC; // not direction
			}
			return context.getString(
					small ? R.string.trip_direction_and_head_sign_small : R.string.trip_direction_and_head_sign_large,
					headSignUC
			);
		}

		public boolean isNoPickup() {
			return this.headsignType == Direction.HEADSIGN_TYPE_NO_PICKUP;
		}

		@Nullable
		private String heading = null; // VOLATILE

		@NonNull
		public String getHeading(@NonNull Context context) {
			if (this.heading == null) {
				if (hasHeadsign()) {
					this.heading = getNewHeading(context);
				} else {
					this.heading = StringUtils.EMPTY;
				}
			}
			return this.heading;
		}

		@Nullable
		public String getHeading() {
			if (this.heading == null) {
				this.heading = getNewHeading();
			}
			return this.heading;
		}

		@NonNull
		private String getNewHeading(@NonNull Context context) {
			return Direction.getNewHeading(context, this.headsignType, this.headsignValue);
		}

		@Nullable
		private String getNewHeading() {
			return Direction.getNewHeading(this.headsignType, this.headsignValue);
		}

		@Discouraged(message = "should use parent Schedule local time zone")
		@Nullable
		public String getLocalTimeZoneId() {
			return localTimeZoneId;
		}

		public void setRealTime(@Nullable Boolean realTime) {
			this.realTime = realTime;
		}

		@SuppressWarnings("unused") // kotlin var
		@Nullable
		public Boolean getRealTime() {
			return this.realTime;
		}

		public boolean isRealTime() {
			return Boolean.TRUE.equals(this.realTime);
		}

		public void setOldSchedule(@Nullable Boolean oldSchedule) {
			this.oldSchedule = oldSchedule;
		}

		@SuppressWarnings("unused") // kotlin var
		@Nullable
		public Boolean getOldSchedule() {
			return this.oldSchedule;
		}

		public boolean isOldSchedule() {
			return Boolean.TRUE.equals(this.oldSchedule);
		}

		public void setAccessible(@Nullable Integer accessible) {
			this.accessible = accessible;
		}

		@Nullable
		public Integer getAccessible() {
			return accessible;
		}

		@SuppressWarnings("unused") // main app
		public int getAccessibleOrDefault() {
			return this.accessible == null ? Accessibility.DEFAULT : this.accessible;
		}

		public void setCancelled(@Nullable Boolean cancelled) {
			this.cancelled = cancelled;
		}

		@Nullable
		public Boolean getCancelled() {
			return cancelled;
		}

		public boolean isCancelled() {
			return Boolean.TRUE.equals(this.cancelled);
		}

		public boolean isRealTimeOrCancelled() {
			return isRealTime() || isCancelled();
		}

		public void setTripId(@Nullable String tripId) {
			this.tripId = tripId;
		}

		public void setStopSequence(int stopSequence) {
			this.stopSequence = stopSequence;
		}

		@Nullable
		public Integer getStopSequenceOrNull() {
			return stopSequence < 0 ? null : stopSequence;
		}

		@Nullable
		public String getTripId() {
			return tripId;
		}

		@SuppressWarnings("RedundantIfStatement")
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Timestamp timestamp = (Timestamp) o;

			if (departureInMs != timestamp.departureInMs) return false;
			if (originalDepartureDelayMs != timestamp.originalDepartureDelayMs) return false;
			if (headsignType != timestamp.headsignType) return false;
			if (!Objects.equals(headsignValue, timestamp.headsignValue)) return false;
			if (!Objects.equals(localTimeZoneId, timestamp.localTimeZoneId)) return false;
			if (!Objects.equals(realTime, timestamp.realTime)) return false;
			if (!Objects.equals(oldSchedule, timestamp.oldSchedule)) return false;
			if (!Objects.equals(accessible, timestamp.accessible)) return false;
			if (!Objects.equals(cancelled, timestamp.cancelled)) return false;
			if (!Objects.equals(tripId, timestamp.tripId)) return false;
			if (stopSequence != timestamp.stopSequence) return false;
			if (!Objects.equals(departureArrivalDiffMs, timestamp.departureArrivalDiffMs)) return false;
			if (originalArrivalDelayMs != timestamp.originalArrivalDelayMs) return false;
			// if (!Objects.equals(heading, timestamp.heading)) return false; // LAZY
			return true;
		}

		@Override
		public int hashCode() {
			int result = Long.hashCode(departureInMs);
			result = 31 * result + Long.hashCode(originalDepartureDelayMs);
			result = 31 * result + headsignType;
			result = 31 * result + (headsignValue != null ? headsignValue.hashCode() : 0);
			result = 31 * result + (localTimeZoneId != null ? localTimeZoneId.hashCode() : 0);
			result = 31 * result + (realTime != null ? realTime.hashCode() : 0);
			result = 31 * result + (oldSchedule != null ? oldSchedule.hashCode() : 0);
			result = 31 * result + (accessible != null ? accessible : 0);
			result = 31 * result + (cancelled != null ? cancelled.hashCode() : 0);
			result = 31 * result + (tripId != null ? tripId.hashCode() : 0);
			result = 31 * result + stopSequence;
			result = 31 * result + (departureArrivalDiffMs != null ? departureArrivalDiffMs.hashCode() : 0);
			result = 31 * result + Long.hashCode(originalArrivalDelayMs);
			// result = 31 * result + (heading != null ? heading.hashCode() : 0); // LAZY
			return result;
		}

		@NonNull
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder(Timestamp.class.getSimpleName());
			sb.append('{');
			sb.append("d=").append(Constants.DEBUG ? MTLog.formatDateTime(getDepartureT()) : getDepartureT());
			if (this.originalDepartureDelayMs != 0L) {
				sb.append(", oDd:").append(this.originalDepartureDelayMs);
			}
			if (departureArrivalDiffMs != null) {
				sb.append(", aD:").append(departureArrivalDiffMs);
			}
			if (this.originalArrivalDelayMs != 0L) {
				sb.append(", oAd:").append(this.originalArrivalDelayMs);
			}
			if (tripId != null) {
				sb.append(", tripId:'").append(tripId).append('\'');
			}
			if (stopSequence >= 0) {
				sb.append(", seq:").append(stopSequence);
			}
			if (headsignType != Direction.HEADSIGN_TYPE_NONE) {
				sb.append(", ht:").append(headsignType);
			}
			if (headsignValue != null) {
				sb.append(", hv:'").append(headsignValue).append('\'');
			}
			if (localTimeZoneId != null) {
				sb.append(", tz:'").append(localTimeZoneId).append('\'');
			}
			if (realTime != null) {
				sb.append(", rt:").append(realTime);
			}
			if (oldSchedule != null) {
				sb.append(", old:").append(oldSchedule);
			}
			if (accessible != null) {
				sb.append(", a11y:").append(accessible);
			}
			if (cancelled != null) {
				sb.append(", cancelled:").append(cancelled);
			}
			sb.append('}');
			return sb.toString();
		}
	}
}
