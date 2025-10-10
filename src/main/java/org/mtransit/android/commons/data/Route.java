package org.mtransit.android.commons.data;

import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.GTFSCommons;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route implements MTLog.Loggable {

	private static final String LOG_TAG = Route.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final ShortNameComparator SHORT_NAME_COMPARATOR = new ShortNameComparator();

	@NonNull
	private final String authority;
	private final long id;
	@NonNull
	private final String shortName;
	@NonNull
	private final String longName;
	@NonNull
	private final String color;

	@Nullable
	private final Integer originalIdHash;
	@Nullable
	private final Integer type;

	@VisibleForTesting
	public Route(@NonNull String authority,
				 long id,
				 @NonNull String shortName,
				 @NonNull String longName,
				 @NonNull String color
	) {
		this(authority, id, shortName, longName, color, GTFSCommons.DEFAULT_ID_HASH, GTFSCommons.DEFAULT_ROUTE_TYPE);
	}

	public Route(
			@NonNull String authority,
			long id,
			@NonNull String shortName,
			@NonNull String longName,
			@NonNull String color,
			@Nullable Integer originalIdHash,
			@Nullable Integer type
	) {
		this.authority = authority;
		this.id = id;
		this.shortName = shortName;
		this.longName = longName;
		this.color = color;
		this.colorInt = null;
		this.originalIdHash = originalIdHash;
		this.type = type;
	}

	@NonNull
	public static Route fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return new Route(
				authority,
				CursorExtKt.getLong(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_ID),
				CursorExtKt.getString(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_SHORT_NAME),
				CursorExtKt.getString(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_LONG_NAME),
				CursorExtKt.getString(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_COLOR),
				FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? CursorExtKt.optInt(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH,
				FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT && FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE ? CursorExtKt.optInt(c, GTFSProviderContract.RouteColumns.T_ROUTE_K_TYPE, GTFSCommons.DEFAULT_ROUTE_TYPE) : GTFSCommons.DEFAULT_ROUTE_TYPE
		);
	}

	public boolean hasColor() {
		return !TextUtils.isEmpty(this.color);
	}

	@NonNull
	public String getColor() {
		return color;
	}

	@Nullable
	private Integer colorInt;

	@ColorInt
	public int getColorInt() {
		if (this.colorInt == null) {
			this.colorInt = ColorUtils.parseColor(getColor());
		}
		return this.colorInt;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Route)) {
			return false;
		}
		Route otherRoute = (Route) o;
		if (getId() != otherRoute.getId()) {
			return false;
		}
		if (!StringUtils.equals(getShortName(), otherRoute.getShortName())) {
			return false;
		}
		if (!StringUtils.equals(getLongName(), otherRoute.getLongName())) {
			return false;
		}
		//noinspection RedundantIfStatement
		if (!StringUtils.equals(getColor(), otherRoute.getColor())) {
			return false;
		}
		return true;
	}

	@NonNull
	@Override
	public String toString() {
		return Route.class.getSimpleName() + "{" +
				"authority=" + authority +
				", id=" + id +
				", shortName='" + shortName + '\'' +
				", longName='" + longName + '\'' +
				", color='" + color + '\'' +
				", colorInt=" + colorInt +
				", odIDHash=" + originalIdHash +
				", type=" + type +
				'}';
	}

	@Nullable
	public static JSONObject toJSON(@NonNull Route route) {
		try {
			final JSONObject jRoute = new JSONObject() //
					.put(JSON_AUTHORITY, route.getAuthority()) //
					.put(JSON_ID, route.getId()) //
					.put(JSON_SHORT_NAME, route.getShortName()) //
					.put(JSON_LONG_NAME, route.getLongName()) //
					.put(JSON_COLOR, route.getColor() //
					);
			if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
				jRoute.put(JSON_ORIGINAL_ID_HASH, route.getOriginalIdHash());
				if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
					jRoute.put(JSON_TYPE, route.getType());
				}
			}
			return jRoute;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while converting to JSON (%s)!", route);
			return null;
		}
	}

	private static final String JSON_AUTHORITY = "authority";
	private static final String JSON_ID = "id";
	private static final String JSON_SHORT_NAME = "shortName";
	private static final String JSON_LONG_NAME = "longName";
	private static final String JSON_COLOR = "color";
	private static final String JSON_ORIGINAL_ID_HASH = "o_id_hash";
	private static final String JSON_TYPE = "type";

	@NonNull
	public static Route fromJSON(@NonNull JSONObject jRoute, @NonNull String authority) throws JSONException {
		try {
			final String routeAuthority = JSONUtils.optString(jRoute, JSON_AUTHORITY);
			return new Route(
					routeAuthority == null ? authority : routeAuthority,
					jRoute.getLong(JSON_ID),
					jRoute.getString(JSON_SHORT_NAME),
					jRoute.getString(JSON_LONG_NAME),
					jRoute.getString(JSON_COLOR),
					FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT ? JSONUtils.optInt(jRoute, JSON_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH) : GTFSCommons.DEFAULT_ID_HASH,
					FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT && FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE ? JSONUtils.optInt(jRoute, JSON_TYPE, GTFSCommons.DEFAULT_ROUTE_TYPE) : GTFSCommons.DEFAULT_ROUTE_TYPE
			);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", jRoute);
			throw jsone;
		}
	}

	@NonNull
	public String getAuthority() {
		return authority;
	}

	public long getId() {
		return id;
	}

	@Nullable
	private String uuid = null;

	@NonNull
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(this.authority, this.id);
		}
		return this.uuid;
	}

	public void resetUUID() {
		this.uuid = null;
	}

	@NonNull
	public String getShortestName() {
		if (TextUtils.isEmpty(getShortName())) {
			return getLongName();
		}
		return getShortName();
	}

	@SuppressWarnings("unused")
	@NonNull
	public String getLongestName() {
		if (TextUtils.isEmpty(getLongName())) {
			return getShortName();
		}
		return getLongName();
	}

	@NonNull
	public String getShortName() {
		return shortName;
	}

	@NonNull
	public String getLongName() {
		return longName;
	}

	@Nullable
	public Integer getOriginalIdHash() {
		return originalIdHash;
	}

	@Nullable
	public Integer getType() {
		return type;
	}

	public static class ShortNameComparator implements Comparator<Route>, MTLog.Loggable {

		private static final String LOG_TAG = Route.class.getSimpleName() + ">" + ShortNameComparator.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final Pattern DIGITS = Pattern.compile("\\d+");

		private static final Pattern STARTS_WITH_LETTERS = Pattern.compile("^[A-Za-z]+", Pattern.CASE_INSENSITIVE);

		public boolean areDifferent(@Nullable Route lhs, @Nullable Route rhs) {
			long lId = lhs == null ? 0L : lhs.getId();
			long rId = rhs == null ? 0L : rhs.getId();
			return lId != rId;
		}

		public boolean areComparable(@Nullable Route lhs, @Nullable Route rhs) {
			return !TextUtils.isEmpty(lhs == null ? null : lhs.getShortName()) //
					&& !TextUtils.isEmpty(rhs == null ? null : rhs.getShortName());
		}

		@Override
		public int compare(@Nullable Route lhs, @Nullable Route rhs) {
			String lShortName = lhs == null ? StringUtils.EMPTY : lhs.getShortName();
			String rShortName = rhs == null ? StringUtils.EMPTY : rhs.getShortName();
			if (lShortName.equals(rShortName)) {
				return ComparatorUtils.SAME;
			}
			try {
				if (!TextUtils.isEmpty(lShortName) && !TextUtils.isEmpty(rShortName)) {
					boolean rNoDigits = !StringUtils.hasDigits(rShortName);
					boolean lNoDigits = !StringUtils.hasDigits(lShortName);
					if (rNoDigits || lNoDigits) {
						if (!rNoDigits) {
							return ComparatorUtils.BEFORE;
						} else if (!lNoDigits) {
							return ComparatorUtils.AFTER;
						} else {
							return lShortName.compareTo(rShortName);
						}
					}
					int rDigits = -1;
					String rStartsWithLetters = StringUtils.EMPTY;
					if (TextUtils.isDigitsOnly(rShortName)) {
						rDigits = Integer.parseInt(rShortName);
					} else {
						Matcher rMatcher = DIGITS.matcher(rShortName);
						if (rMatcher.find()) {
							String rDigitS = rMatcher.group();
							if (!TextUtils.isEmpty(rDigitS)) {
								rDigits = Integer.parseInt(rDigitS);
							}
						}
						rMatcher = STARTS_WITH_LETTERS.matcher(rShortName);
						if (rMatcher.find()) {
							rStartsWithLetters = rMatcher.group();
						}
					}
					int lDigits = -1;
					String lStartsWithLetters = StringUtils.EMPTY;
					if (TextUtils.isDigitsOnly(lShortName)) {
						lDigits = Integer.parseInt(lShortName);
					} else {
						Matcher lMatcher = DIGITS.matcher(lShortName);
						if (lMatcher.find()) {
							String lDigitS = lMatcher.group();
							if (!TextUtils.isEmpty(lDigitS)) {
								lDigits = Integer.parseInt(lDigitS);
							}
						}
						lMatcher = STARTS_WITH_LETTERS.matcher(lShortName);
						if (lMatcher.find()) {
							lStartsWithLetters = lMatcher.group();
						}
					}
					if (rDigits != lDigits) {
						if (!rStartsWithLetters.equals(lStartsWithLetters)) {
							return lStartsWithLetters.compareTo(rStartsWithLetters);
						}
						return lDigits - rDigits;
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while sorting routes!");
			}
			return lShortName.compareTo(rShortName);
		}
	}
}
