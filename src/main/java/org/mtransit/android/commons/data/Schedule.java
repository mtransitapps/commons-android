package org.mtransit.android.commons.data;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.status.StatusProviderContract;
import org.mtransit.commons.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class Schedule extends POIStatus implements MTLog.Loggable {

	private static final String LOG_TAG = Schedule.class.getSimpleName();

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

	private long usefulUntilInMs = -1L;

	private boolean noPickup;

	@NonNull
	private final List<Frequency> frequencies = new ArrayList<>();

	public Schedule(@NonNull POIStatus status, long providerPrecisionInMs, boolean noPickup) {
		this(
				status.getId(),
				status.getTargetUUID(),
				status.getLastUpdateInMs(),
				status.getValidityInMs(),
				status.getReadFromSourceAtInMs(),
				providerPrecisionInMs,
				noPickup,
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
			@Nullable String sourceLabel,
			boolean noData
	) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_SCHEDULE, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, noData);
		this.noPickup = noPickup;
		this.providerPrecisionInMs = providerPrecisionInMs;
		resetUsefulUntilInMs();
	}

	public boolean isNoPickup() {
		return noPickup;
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
		return Schedule.class.getSimpleName() + "{" +
				"timestamps=" + timestamps +
				", providerPrecisionInMs=" + providerPrecisionInMs +
				", usefulUntilInMs=" + usefulUntilInMs +
				", noPickup=" + noPickup +
				", frequencies=" + frequencies +
				'}';
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
			JSONObject json = extrasJSONString.isEmpty() ? null : new JSONObject(extrasJSONString);
			if (json == null) {
				return null;
			}
			return fromExtraJSON(status, json);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Nullable
	private static Schedule fromExtraJSON(@NonNull POIStatus status, @NonNull JSONObject extrasJSON) {
		try {
			long providerPrecisionInMs = extrasJSON.getInt(JSON_PROVIDER_PRECISION_IN_MS);
			boolean noPickup = extrasJSON.optBoolean(JSON_IS_NO_PICKUP, false);
			Schedule schedule = new Schedule(status, providerPrecisionInMs, noPickup);
			JSONArray jTimestamps = extrasJSON.getJSONArray(JSON_TIMESTAMPS);
			for (int i = 0; i < jTimestamps.length(); i++) {
				JSONObject jTimestamp = jTimestamps.getJSONObject(i);
				schedule.addTimestampWithoutSort(Timestamp.parseJSON(jTimestamp));
			}
			schedule.sortTimestamps();
			JSONArray jFrequencies = extrasJSON.getJSONArray(JSON_FREQUENCIES);
			for (int i = 0; i < jFrequencies.length(); i++) {
				JSONObject jFrequency = jFrequencies.getJSONObject(i);
				schedule.addFrequencyWithoutSort(Frequency.parseJSON(jFrequency));
			}
			schedule.sortFrequencies();
			return schedule;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static final String JSON_PROVIDER_PRECISION_IN_MS = "providerPrecisionInMs";
	private static final String JSON_IS_NO_PICKUP = "decentOnly"; // do NOT change JSON key string value!
	private static final String JSON_TIMESTAMPS = "timestamps";
	private static final String JSON_FREQUENCIES = "frequencies";

	@Nullable
	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_PROVIDER_PRECISION_IN_MS, this.providerPrecisionInMs);
			json.put(JSON_IS_NO_PICKUP, this.noPickup);
			JSONArray jTimestamps = new JSONArray();
			for (Timestamp timestamp : this.timestamps) {
				jTimestamps.put(timestamp.toJSON());
			}
			json.put(JSON_TIMESTAMPS, jTimestamps);
			JSONArray jFrequencies = new JSONArray();
			for (Frequency frequency : this.frequencies) {
				jFrequencies.put(frequency.toJSON());
			}
			json.put(JSON_FREQUENCIES, jFrequencies);
			return json;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
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
		resetUsefulUntilInMs();
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
		if (newTimestamp == null) {
			return;
		}
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
		resetUsefulUntilInMs();
	}

	@NonNull
	public List<Timestamp> getTimestamps() {
		return this.timestamps;
	}

	public int getTimestampsCount() {
		return this.timestamps.size();
	}

	@Nullable
	public TimeZone getTimeZone() {
		for (Timestamp timestamp : this.timestamps) {
			final String localTimeZoneId = timestamp.getLocalTimeZone();
			if (localTimeZoneId != null) {
				return TimeZone.getTimeZone(localTimeZoneId);
			}
		}
		return null;
	}

	protected static final long MIN_UI_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	protected long getUIProviderPrecisionInMs() {
		return Math.max(MIN_UI_PRECISION_IN_MS, this.providerPrecisionInMs);
	}

	private void resetUsefulUntilInMs() {
		int timestampsCount = getTimestampsCount();
		if (timestampsCount == 0) {
			this.usefulUntilInMs = 0L; // NOT USEFUL
			return;
		}
		this.usefulUntilInMs = this.timestamps.get(timestampsCount - 1).getDepartureT() + getUIProviderPrecisionInMs();
	}

	public long getUsefulUntilInMs() {
		if (this.usefulUntilInMs < 0L) {
			resetUsefulUntilInMs();
		}
		return usefulUntilInMs;
	}

	@Override
	public boolean isUseful() {
		return super.isUseful() //
				&& getUsefulUntilInMs() > TimeUtils.currentTimeToTheMinuteMillis();
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

		private static final String LOG_TAG = Frequency.class.getSimpleName();

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

		@Nullable
		static Frequency parseJSON(@NonNull JSONObject jFrequency) {
			try {
				long startTimeInMs = jFrequency.getLong(JSON_START_TIME_IN_MS);
				long endTimeInMs = jFrequency.getLong(JSON_END_TIME_IN_MS);
				int headwayInSec = jFrequency.getInt(JSON_HEADWAY_IN_SEC);
				Boolean oldSchedule = null;
				if (jFrequency.has(JSON_OLD_SCHEDULE)) {
					oldSchedule = jFrequency.optBoolean(JSON_OLD_SCHEDULE, false);
				}
				return new Frequency(startTimeInMs, endTimeInMs, headwayInSec, oldSchedule);
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'!", jFrequency);
				return null; // no partial results
			}
		}

		private static final String JSON_START_TIME_IN_MS = "startTimeInMs";
		private static final String JSON_END_TIME_IN_MS = "endTimeInMs";
		private static final String JSON_HEADWAY_IN_SEC = "headwayInSec";
		private static final String JSON_OLD_SCHEDULE = "old";

		@Nullable
		public JSONObject toJSON() {
			return toJSON(this);
		}

		@Nullable
		public static JSONObject toJSON(@NonNull Frequency frequency) {
			try {
				JSONObject jFrequency = new JSONObject();
				jFrequency.put(JSON_START_TIME_IN_MS, frequency.startTimeInMs);
				jFrequency.put(JSON_END_TIME_IN_MS, frequency.endTimeInMs);
				jFrequency.put(JSON_HEADWAY_IN_SEC, frequency.headwayInSec);
				if (frequency.hasOldSchedule()) {
					jFrequency.put(JSON_OLD_SCHEDULE, frequency.oldSchedule);
				}
				return jFrequency;
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while converting object '%s' to JSON!", frequency);
				return null; // no partial result
			}
		}
	}

	public static class Timestamp implements MTLog.Loggable { // Stop Time

		private static final String LOG_TAG = Timestamp.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private long departureInMs;
		private long originalDepartureDelayMs = 0L;
		@Direction.HeadSignType
		private int headsignType = Direction.HEADSIGN_TYPE_NONE;
		@Nullable
		private String headsignValue = null;
		@Nullable
		private String localTimeZoneId = null;
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
		private Long arrivalDiffMs = null;
		private long originalArrivalDelayMs = 0L;

		@VisibleForTesting
		public Timestamp(long departureT) {
			this.departureInMs = departureT;
		}

		public Timestamp(long departureT, @NonNull TimeZone localTimeZone) {
			this(departureT, localTimeZone.getID());
		}

		public Timestamp(long departureT, @NonNull String localTimeZoneId) {
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
			return getDepartureT() - (arrivalDiffMs == null ? 0L : arrivalDiffMs);
		}

		@Nullable
		public Long getArrivalTIfDifferent() {
			return arrivalDiffMs == null ? null : getDepartureT() - arrivalDiffMs;
		}

		public void setArrivalT(long arrivalT) {
			setArrivalDiffMs(getDepartureT() - arrivalT);
		}

		public void setArrivalDiffMs(@Nullable Long arrivalDiffMs) {
			this.arrivalDiffMs = arrivalDiffMs;
		}

		@Nullable
		public Long getArrivalDiffMs() {
			return arrivalDiffMs;
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

		private void setLocalTimeZone(@Nullable String localTimeZone) {
			this.localTimeZoneId = localTimeZone;
		}

		@Nullable
		public String getLocalTimeZone() {
			return localTimeZoneId;
		}

		@Deprecated
		public boolean hasLocalTimeZone() {
			return !TextUtils.isEmpty(this.localTimeZoneId);
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
			if (!Objects.equals(arrivalDiffMs, timestamp.arrivalDiffMs)) return false;
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
			result = 31 * result + (arrivalDiffMs != null ? arrivalDiffMs.hashCode() : 0);
			result = 31 * result + Long.hashCode(originalArrivalDelayMs);
			// result = 31 * result + (heading != null ? heading.hashCode() : 0); // LAZY
			return result;
		}

		@NonNull
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(Timestamp.class.getSimpleName());
			sb.append('{');
			sb.append("d=").append(Constants.DEBUG ? MTLog.formatDateTime(getDepartureT()) : getDepartureT());
			if (this.originalDepartureDelayMs != 0L) {
				sb.append(", oDd:").append(this.originalDepartureDelayMs);
			}
			if (arrivalDiffMs != null) {
				sb.append(", aD:").append(arrivalDiffMs);
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

		private static final String JSON_DEPARTURE = "t";
		private static final String JSON_ORIGINAL_DEPARTURE_DELAY = "tOD";
		private static final String JSON_ARRIVAL_DIFF = "tDiffA";
		private static final String JSON_ORIGINAL_ARRIVAL_DELAY = "tOA";
		private static final String JSON_TRIP_ID = "trip_id";
		private static final String JSON_STOP_SEQUENCE = "stop_seq";
		private static final String JSON_HEADSIGN_TYPE = "ht";
		private static final String JSON_HEADSIGN_VALUE = "hv";
		private static final String JSON_LOCAL_TIME_ZONE = "localTimeZone";
		private static final String JSON_REAL_TIME = "rt";
		private static final String JSON_OLD_SCHEDULE = "old";
		private static final String JSON_ACCESSIBLE = "a11y";
		private static final String JSON_CANCELLED = "cancelled";

		@Nullable
		static Timestamp parseJSON(@NonNull JSONObject jTimestamp) {
			try {
				final long departureInMs = jTimestamp.getLong(JSON_DEPARTURE);
				final Timestamp timestamp = new Timestamp(departureInMs);
				final long originalDepartureDelayMs = jTimestamp.optLong(JSON_ORIGINAL_DEPARTURE_DELAY, 0L);
				if (originalDepartureDelayMs != 0L) {
					timestamp.setOriginalDepartureDelayMs(originalDepartureDelayMs);
				}
				if (jTimestamp.has(JSON_ARRIVAL_DIFF)) {
					timestamp.setArrivalDiffMs(jTimestamp.getLong(JSON_ARRIVAL_DIFF));
				}
				final long originalArrivalDelayMs = jTimestamp.optLong(JSON_ORIGINAL_ARRIVAL_DELAY, 0L);
				if (originalArrivalDelayMs != 0L) {
					timestamp.setOriginalArrivalDelayMs(originalArrivalDelayMs);
				}
				if (jTimestamp.has(JSON_TRIP_ID)) {
					timestamp.setTripId(jTimestamp.getString(JSON_TRIP_ID));
				}
				if (jTimestamp.has(JSON_STOP_SEQUENCE)) {
					timestamp.setStopSequence(jTimestamp.getInt(JSON_STOP_SEQUENCE));
				}
				final int headSignType = jTimestamp.optInt(JSON_HEADSIGN_TYPE, -1);
				final String headSignValue = jTimestamp.optString(JSON_HEADSIGN_VALUE, StringUtils.EMPTY);
				if (headSignType >= 0 && !headSignValue.isEmpty()) {
					timestamp.setHeadsign(headSignType, headSignValue);
				} else {
					if (headSignType == Direction.HEADSIGN_TYPE_NO_PICKUP) {
						timestamp.setHeadsign(headSignType, null);
					}
				}
				String localTimeZone = jTimestamp.optString(JSON_LOCAL_TIME_ZONE);
				if (!TextUtils.isEmpty(localTimeZone)) {
					timestamp.setLocalTimeZone(localTimeZone);
				}
				if (jTimestamp.has(JSON_REAL_TIME)) {
					timestamp.setRealTime(jTimestamp.optBoolean(JSON_REAL_TIME, false));
				}
				if (jTimestamp.has(JSON_OLD_SCHEDULE)) {
					timestamp.setOldSchedule(jTimestamp.optBoolean(JSON_OLD_SCHEDULE, false));
				}
				if (jTimestamp.has(JSON_ACCESSIBLE)) {
					timestamp.setAccessible(jTimestamp.optInt(JSON_ACCESSIBLE, Accessibility.DEFAULT));
				}
				if (jTimestamp.has(JSON_CANCELLED)) {
					timestamp.setCancelled(jTimestamp.optBoolean(JSON_CANCELLED, false));
				}
				return timestamp;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'!", jTimestamp);
				return null; // no partial results
			}
		}

		@Nullable
		public JSONObject toJSON() {
			return toJSON(this);
		}

		@Nullable
		public static JSONObject toJSON(@NonNull Timestamp timestamp) {
			try {
				JSONObject jTimestamp = new JSONObject();
				jTimestamp.put(JSON_DEPARTURE, timestamp.departureInMs);
				if (timestamp.originalDepartureDelayMs != 0L) {
					jTimestamp.put(JSON_ORIGINAL_DEPARTURE_DELAY, timestamp.originalDepartureDelayMs);
				}
				if (timestamp.arrivalDiffMs != null) {
					jTimestamp.put(JSON_ARRIVAL_DIFF, timestamp.arrivalDiffMs);
				}
				if (timestamp.originalArrivalDelayMs != 0L) {
					jTimestamp.put(JSON_ORIGINAL_ARRIVAL_DELAY, timestamp.originalArrivalDelayMs);
				}
				if (timestamp.tripId != null) {
					jTimestamp.put(JSON_TRIP_ID, timestamp.tripId);
				}
				if (timestamp.stopSequence >= 0) {
					jTimestamp.put(JSON_STOP_SEQUENCE, timestamp.stopSequence);
				}
				if (timestamp.headsignType != Direction.HEADSIGN_TYPE_NONE && timestamp.headsignValue != null) {
					jTimestamp.put(JSON_HEADSIGN_TYPE, timestamp.headsignType);
					jTimestamp.put(JSON_HEADSIGN_VALUE, timestamp.headsignValue);
				} else {
					if (timestamp.headsignType == Direction.HEADSIGN_TYPE_NO_PICKUP) {
						jTimestamp.put(JSON_HEADSIGN_TYPE, timestamp.headsignType);
					}
				}
				if (timestamp.localTimeZoneId != null) {
					jTimestamp.put(JSON_LOCAL_TIME_ZONE, timestamp.localTimeZoneId);
				}
				if (timestamp.realTime != null) {
					jTimestamp.put(JSON_REAL_TIME, timestamp.realTime);
				}
				if (timestamp.oldSchedule != null) {
					jTimestamp.put(JSON_OLD_SCHEDULE, timestamp.oldSchedule);
				}
				if (timestamp.accessible != null) {
					jTimestamp.put(JSON_ACCESSIBLE, timestamp.accessible);
				}
				if (timestamp.cancelled != null) {
					jTimestamp.put(JSON_CANCELLED, timestamp.cancelled);
				}
				return jTimestamp;
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while converting object '%s' to JSON!", timestamp);
				return null; // no partial result
			}
		}
	}

	public static class ScheduleStatusFilter extends StatusProviderContract.Filter {

		private static final String LOG_TAG = ScheduleStatusFilter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@SuppressWarnings("unused") // main app
		public static final int DATA_REQUEST_MONTHS = 62;
		@SuppressWarnings("unused")
		public static final int DATA_REQUEST_YEAR = 365;

		private static final long MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT = TimeUnit.DAYS.toMillis(1L);
		private static final int MIN_USEFUL_RESULTS_DEFAULT = 10;
		public static final int MAX_DATA_REQUESTS_DEFAULT = 7 * 7; // 7 weeks
		private static final long LOOK_BEHIND_IN_MS_DEFAULT = TimeUnit.MILLISECONDS.toMillis(0L);

		@NonNull
		private final RouteDirectionStop routeDirectionStop;
		@Nullable
		private Long lookBehindInMs = null;
		@Nullable
		private Long minUsefulDurationCoveredInMs = null;
		@Nullable
		private Integer minUsefulResults = null;
		@Nullable
		private Integer maxDataRequests = null;
		@Nullable
		private Boolean includeCancelledTimestamps = null;

		public ScheduleStatusFilter(@NonNull String targetUUID, @NonNull RouteDirectionStop rds) {
			this(rds);
		}

		public ScheduleStatusFilter(@NonNull RouteDirectionStop rds) {
			super(POI.ITEM_STATUS_TYPE_SCHEDULE, rds.getUUID());
			this.routeDirectionStop = rds;
		}

		@NonNull
		public RouteDirectionStop getRouteDirectionStop() {
			return routeDirectionStop;
		}

		@NonNull
		public String getTargetAuthority() {
			return this.routeDirectionStop.getAuthority();
		}

		public long getRouteId() {
			return this.routeDirectionStop.getRoute().getId();
		}

		public long getDirectionId() {
			return this.routeDirectionStop.getDirection().getId();
		}

		public long getLookBehindInMsOrDefault() {
			return lookBehindInMs == null ? LOOK_BEHIND_IN_MS_DEFAULT : lookBehindInMs;
		}

		public void setLookBehindInMs(@Nullable Long lookBehindInMs) {
			this.lookBehindInMs = lookBehindInMs;
		}

		public long getTimestampOrDefault() {
			return getNewDefaultTimestamp();
		}

		public long getMinUsefulDurationCoveredInMsOrDefault() {
			return this.minUsefulDurationCoveredInMs == null ? MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT : this.minUsefulDurationCoveredInMs;
		}

		@SuppressWarnings("unused")
		public void setMinUsefulDurationCoveredInMs(@Nullable Long minUsefulDurationCoveredInMs) {
			this.minUsefulDurationCoveredInMs = minUsefulDurationCoveredInMs;
		}

		public int getMinUsefulResultsOrDefault() {
			return minUsefulResults == null ? MIN_USEFUL_RESULTS_DEFAULT : minUsefulResults;
		}

		@SuppressWarnings("unused")
		public void setMinUsefulResults(@Nullable Integer minUsefulResults) {
			this.minUsefulResults = minUsefulResults;
		}

		public int getMaxDataRequestsOrDefault() {
			return maxDataRequests == null ? MAX_DATA_REQUESTS_DEFAULT : maxDataRequests;
		}

		public void setMaxDataRequests(@Nullable Integer maxDataRequests) {
			this.maxDataRequests = maxDataRequests;
		}

		public boolean isIncludeCancelledTimestampsOrDefault() {
			return Boolean.TRUE.equals(this.includeCancelledTimestamps);
		}

		@SuppressWarnings("unused") // main app
		public void setIncludeCancelledTimestamps(@Nullable Boolean includeCancelledTimestamps) {
			this.includeCancelledTimestamps = includeCancelledTimestamps;
		}

		private static long getNewDefaultTimestamp() {
			return TimeUtils.currentTimeToTheMinuteMillis();
		}

		@Nullable
		@Override
		public StatusProviderContract.Filter fromJSONStringStatic(@Nullable String jsonString) {
			return fromJSONString(jsonString);
		}

		@Nullable
		public static StatusProviderContract.Filter fromJSONString(@Nullable String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_MIN_USEFUL_DURATION_COVERED_IN_MS = "minUsefulDurationCoveredInMs";
		private static final String JSON_MIN_USEFUL_RESULTS = "minUsefulResults";
		private static final String JSON_MAX_DATA_REQUESTS = "maxDataRequests";
		private static final String JSON_ROUTE_DIRECTION_STOP = "routeTripStop"; // do not change to avoid breaking compat w/ old modules
		private static final String JSON_LOOK_BEHIND_IN_MS = "lookBehindInMs";
		private static final String JSON_INCLUDE_CANCELLED_TIMESTAMPS = "includeCancelledTimestamps";

		@Nullable
		public static StatusProviderContract.Filter fromJSON(@NonNull JSONObject json) {
			try {
				final RouteDirectionStop routeDirectionStop = RouteDirectionStop.fromJSONStatic(json.getJSONObject(JSON_ROUTE_DIRECTION_STOP));
				if (routeDirectionStop == null) {
					return null;
				}
				final ScheduleStatusFilter scheduleStatusFilter = new ScheduleStatusFilter(routeDirectionStop);
				StatusProviderContract.Filter.fromJSON(scheduleStatusFilter, json);
				scheduleStatusFilter.lookBehindInMs = json.has(JSON_LOOK_BEHIND_IN_MS) ? json.getLong(JSON_LOOK_BEHIND_IN_MS) : null;
				scheduleStatusFilter.minUsefulDurationCoveredInMs =
						json.has(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS) ? json.getLong(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS) : null;
				scheduleStatusFilter.minUsefulResults = json.has(JSON_MIN_USEFUL_RESULTS) ? json.getInt(JSON_MIN_USEFUL_RESULTS) : null;
				scheduleStatusFilter.maxDataRequests = json.has(JSON_MAX_DATA_REQUESTS) ? json.getInt(JSON_MAX_DATA_REQUESTS) : null;
				scheduleStatusFilter.includeCancelledTimestamps =
						json.has(JSON_INCLUDE_CANCELLED_TIMESTAMPS) ? json.optBoolean(JSON_INCLUDE_CANCELLED_TIMESTAMPS, false) : null;
				return scheduleStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Nullable
		@Override
		public String toJSONStringStatic(@NonNull StatusProviderContract.Filter statusFilter) {
			return toJSONString(statusFilter);
		}

		@Nullable
		static String toJSONString(@NonNull StatusProviderContract.Filter statusFilter) {
			final JSONObject json = toJSON(statusFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		public static JSONObject toJSON(@NonNull StatusProviderContract.Filter statusFilter) {
			try {
				final JSONObject json = new JSONObject();
				StatusProviderContract.Filter.toJSON(statusFilter, json);
				if (statusFilter instanceof ScheduleStatusFilter) {
					final ScheduleStatusFilter scheduleFilter = (ScheduleStatusFilter) statusFilter;
					json.put(JSON_ROUTE_DIRECTION_STOP, scheduleFilter.routeDirectionStop.toJSON());
					if (scheduleFilter.lookBehindInMs != null) {
						json.put(JSON_LOOK_BEHIND_IN_MS, scheduleFilter.lookBehindInMs);
					}
					if (scheduleFilter.minUsefulDurationCoveredInMs != null) {
						json.put(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS, scheduleFilter.minUsefulDurationCoveredInMs);
					}
					if (scheduleFilter.minUsefulResults != null) {
						json.put(JSON_MIN_USEFUL_RESULTS, scheduleFilter.minUsefulResults);
					}
					if (scheduleFilter.maxDataRequests != null) {
						json.put(JSON_MAX_DATA_REQUESTS, scheduleFilter.maxDataRequests);
					}
					if (scheduleFilter.includeCancelledTimestamps != null) {
						json.put(JSON_INCLUDE_CANCELLED_TIMESTAMPS, scheduleFilter.includeCancelledTimestamps);
					}
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while making JSON object '%s'", statusFilter);
				return null;
			}
		}

		@NonNull
		@Override
		public String toString() {
			return ScheduleStatusFilter.class.getSimpleName() + "{" +
					super.toString() +
					", rds=" + routeDirectionStop +
					", lookBehindInMs=" + lookBehindInMs +
					", minUsefulDurationCoveredInMs=" + minUsefulDurationCoveredInMs +
					", minUsefulResults=" + minUsefulResults +
					", maxDataRequests=" + maxDataRequests +
					", includeCancelledTimestamps=" + includeCancelledTimestamps +
					'}';
		}
	}
}
