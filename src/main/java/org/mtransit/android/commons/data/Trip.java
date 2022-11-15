package org.mtransit.android.commons.data;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.FeatureFlags;

import java.lang.annotation.Retention;
import java.util.Comparator;

@SuppressWarnings("WeakerAccess")
public class Trip {

	private static final String LOG_TAG = Trip.class.getSimpleName();

	public static final HeadSignComparator HEAD_SIGN_COMPARATOR = new HeadSignComparator();

	@Retention(SOURCE)
	@IntDef({HEADSIGN_TYPE_NONE, HEADSIGN_TYPE_STRING, HEADSIGN_TYPE_DIRECTION, HEADSIGN_TYPE_INBOUND, HEADSIGN_TYPE_STOP_ID, HEADSIGN_TYPE_DESCENT_ONLY})
	public @interface HeadSignType {
	}

	public static final int HEADSIGN_TYPE_NONE = -1;
	public static final int HEADSIGN_TYPE_STRING = 0;
	public static final int HEADSIGN_TYPE_DIRECTION = 1;
	public static final int HEADSIGN_TYPE_INBOUND = 2;
	@SuppressWarnings("unused") // TODO ?
	public static final int HEADSIGN_TYPE_STOP_ID = 3;
	public static final int HEADSIGN_TYPE_DESCENT_ONLY = 4;

	private final long id;
	@HeadSignType
	private final int headsignType;
	@NonNull
	private final String headsignValue;
	private final long routeId;

	public Trip(long id,
				@HeadSignType int headsignType,
				@NonNull String headsignValue,
				long routeId) {
		this.id = id;
		this.headsignType = headsignType;
		this.headsignValue = headsignValue;
		this.routeId = routeId;
	}

	@NonNull
	public static Trip fromCursor(@NonNull Cursor c) {
		return new Trip(
				c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_ID)),
				c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_TYPE)),
				c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_HEADSIGN_VALUE)),
				c.getInt(c.getColumnIndexOrThrow(GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID))
		);
	}

	@NonNull
	@Override
	public String toString() {
		return Trip.class.getSimpleName() + "{" +
				"id=" + id +
				", headsignType=" + headsignType +
				", headsignValue='" + headsignValue + '\'' +
				", routeId=" + routeId +
				", heading='" + heading + '\'' +
				'}';
	}

	private static final String JSON_ID = "id";
	private static final String JSON_HEADSIGN_TYPE = "headsignType";
	private static final String JSON_HEADSIGN_VALUE = "headsignValue";
	private static final String JSON_ROUTE_ID = "routeId";

	@Nullable
	public static JSONObject toJSON(@NonNull Trip trip) {
		try {
			return new JSONObject() //
					.put(JSON_ID, trip.getId()) //
					.put(JSON_HEADSIGN_TYPE, trip.getHeadsignType()) //
					.put(JSON_HEADSIGN_VALUE, trip.getHeadsignValue()) //
					.put(JSON_ROUTE_ID, trip.getRouteId());
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while converting to JSON (%s)!", trip);
			return null;
		}
	}

	@NonNull
	public static Trip fromJSON(@NonNull JSONObject jTrip) throws JSONException {
		try {
			return new Trip(
					jTrip.getLong(JSON_ID),
					jTrip.getInt(JSON_HEADSIGN_TYPE),
					jTrip.getString(JSON_HEADSIGN_VALUE),
					jTrip.getInt(JSON_ROUTE_ID));
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", jTrip);
			throw jsone;
		}
	}

	@SuppressWarnings("unused")
	@NonNull
	public String getUIHeading(@NonNull Context context, boolean small) {
		final String headSignUC = getHeading(context);
		return context.getString(
				small ? R.string.trip_direction_and_head_sign_small : R.string.trip_direction_and_head_sign_large,
				headSignUC
		);
	}

	@Nullable
	private String heading = null;

	@NonNull
	public String getHeading(@NonNull Context context) {
		if (this.heading == null) {
			this.heading = getNewHeading(context, this.headsignType, this.headsignValue);
		}
		return this.heading;
	}

	@Nullable
	public String getHeading() {
		if (this.heading == null) {
			this.heading = getNewHeading(this.headsignType, this.headsignValue);
		}
		return this.heading;
	}

	public static final String HEADING_OUTBOUND = "0";
	public static final String HEADING_INBOUND = "1";

	public static final String HEADING_EAST = "E"; // 1
	public static final String HEADING_WEST = "W"; // 2
	public static final String HEADING_NORTH = "N"; // 3
	public static final String HEADING_SOUTH = "S"; // 4

	@Nullable
	public static String getNewHeading(@HeadSignType int headsignType, @Nullable String headsignValue) {
		switch (headsignType) {
		case HEADSIGN_TYPE_STRING:
			return headsignValue;
		case HEADSIGN_TYPE_DESCENT_ONLY:
		case HEADSIGN_TYPE_DIRECTION:
		case HEADSIGN_TYPE_INBOUND:
			return null; // Can't return correct heading w/o context
		case HEADSIGN_TYPE_STOP_ID: // not supported (yet?)
		case HEADSIGN_TYPE_NONE:
		default:
			MTLog.w(LOG_TAG, "Unexpected trip heading (type: %s | value: %s) w/o context!", headsignType, headsignValue);
			return null;
		}
	}

	@NonNull
	static String getNewHeading(@NonNull Context context, @HeadSignType int headsignType, String headsignValue) {
		switch (headsignType) {
		case HEADSIGN_TYPE_STRING:
			return headsignValue;
		case HEADSIGN_TYPE_DIRECTION:
			if (HEADING_EAST.equals(headsignValue)) {
				return context.getString(R.string.east);
			} else if (HEADING_NORTH.equals(headsignValue)) {
				return context.getString(R.string.north);
			} else if (HEADING_WEST.equals(headsignValue)) {
				return context.getString(R.string.west);
			} else if (HEADING_SOUTH.equals(headsignValue)) {
				return context.getString(R.string.south);
			}
			break;
		case HEADSIGN_TYPE_INBOUND:
			if (HEADING_INBOUND.equals(headsignValue)) {
				return context.getString(R.string.inbound);
			} else if (HEADING_OUTBOUND.equals(headsignValue)) {
				return context.getString(R.string.outbound);
			}
			break;
		case HEADSIGN_TYPE_DESCENT_ONLY:
			if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
				return context.getString(R.string.descent_only);
			}
		case HEADSIGN_TYPE_STOP_ID: // not supported (yet?)
		case HEADSIGN_TYPE_NONE:
		default:
			break;
		}
		MTLog.w(LOG_TAG, "Unexpected trip heading (type: %s | value: %s) w/ context!", headsignType, headsignValue);
		return context.getString(R.string.ellipsis);
	}

	public static boolean isSameHeadsign(@Nullable String stringHeadsign1, @Nullable String stringHeadsign2) {
		boolean stringHeadsign1Empty = TextUtils.isEmpty(stringHeadsign1);
		boolean stringHeadsign2Empty = TextUtils.isEmpty(stringHeadsign2);
		if (stringHeadsign1Empty) {
			//noinspection RedundantIfStatement
			if (stringHeadsign2Empty) {
				return true; // same (empty)
			}
			return false; // not the same
		} else if (stringHeadsign2Empty) {
			return false; // not the same
		}
		return StringUtils.equalsAlphabeticsAndDigits(stringHeadsign1, stringHeadsign2);
	}

	public long getId() {
		return this.id;
	}

	@HeadSignType
	public int getHeadsignType() {
		return headsignType;
	}

	@NonNull
	public String getHeadsignValue() {
		return headsignValue;
	}

	public long getRouteId() {
		return routeId;
	}

	public static class HeadSignComparator implements Comparator<Trip>, MTLog.Loggable {

		private static final String LOG_TAG = Trip.class.getSimpleName() + ">" + HeadSignComparator.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		public boolean areDifferent(@Nullable Trip lhs, @Nullable Trip rhs) {
			long lId = lhs == null ? 0L : lhs.getId();
			long rId = rhs == null ? 0L : rhs.getId();
			return lId != rId;
		}

		public boolean areComparable(@Nullable Trip lhs, @Nullable Trip rhs) {
			return lhs != null  //
					&& rhs != null;
		}

		@Override
		public int compare(@Nullable Trip lhs, @Nullable Trip rhs) {
			String lHeadsign = lhs == null ? null : lhs.getHeading();
			String rHeadsign = rhs == null ? null : rhs.getHeading();
			try {
				if (lHeadsign == null || rHeadsign == null) {
					String lHeadsignValue = lhs == null ? StringUtils.EMPTY : lhs.getHeadsignValue();
					String rHeadsignValue = rhs == null ? StringUtils.EMPTY : rhs.getHeadsignValue();
					return lHeadsignValue.compareTo(rHeadsignValue);
				}
				return lHeadsign.compareTo(rHeadsign);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while sorting trips!");
			}
			lHeadsign = lHeadsign == null ? StringUtils.EMPTY : lHeadsign;
			rHeadsign = rHeadsign == null ? StringUtils.EMPTY : rHeadsign;
			return lHeadsign.compareTo(rHeadsign);
		}
	}
}
