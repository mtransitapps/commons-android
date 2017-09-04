package org.mtransit.android.commons.data;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;

import android.database.Cursor;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class Route implements MTLog.Loggable {

	private static final String TAG = Route.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final ShortNameComparator SHORT_NAME_COMPARATOR = new ShortNameComparator();

	private long id;
	private String shortName;
	private String longName;
	private String color;

	public static Route fromCursor(Cursor c) {
		Route route = new Route();
		route.setId(c.getLong(c.getColumnIndexOrThrow(GTFSProviderContract.RouteColumns.T_ROUTE_K_ID)));
		route.setShortName(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteColumns.T_ROUTE_K_SHORT_NAME)));
		route.setLongName(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteColumns.T_ROUTE_K_LONG_NAME)));
		route.setColor(c.getString(c.getColumnIndexOrThrow(GTFSProviderContract.RouteColumns.T_ROUTE_K_COLOR)));
		return route;
	}

	public boolean hasColor() {
		return !TextUtils.isEmpty(this.color);
	}

	protected void setColor(String color) {
		this.color = color;
		this.colorInt = null;
	}

	public String getColor() {
		return color;
	}

	private Integer colorInt = null;

	@ColorInt
	public int getColorInt() {
		if (this.colorInt == null) {
			this.colorInt = ColorUtils.parseColor(getColor());
		}
		return this.colorInt;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Route)) {
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
		if (!StringUtils.equals(getColor(), otherRoute.getColor())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Route.class.getSimpleName()).append(":[") //
				.append("id:").append(getId()).append(',') //
				.append("shortName:").append(getShortName()).append(',') //
				.append("longName:").append(getLongName()).append(',') //
				.append("color:").append(getColor()) //
				.append(']').toString();
	}

	public static JSONObject toJSON(Route route) {
		try {
			return new JSONObject() //
					.put(JSON_ID, route.getId()) //
					.put(JSON_SHORT_NAME, route.getShortName()) //
					.put(JSON_LONG_NAME, route.getLongName()) //
					.put(JSON_COLOR, route.getColor() //
					);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", route);
			return null;
		}
	}

	private static final String JSON_ID = "id";
	private static final String JSON_SHORT_NAME = "shortName";
	private static final String JSON_LONG_NAME = "longName";
	private static final String JSON_COLOR = "color";

	public static Route fromJSON(JSONObject jRoute) {
		try {
			Route route = new Route();
			route.setId(jRoute.getLong(JSON_ID));
			route.setShortName(jRoute.getString(JSON_SHORT_NAME));
			route.setLongName(jRoute.getString(JSON_LONG_NAME));
			route.setColor(jRoute.getString(JSON_COLOR));
			return route;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jRoute);
			return null;
		}
	}

	public static CharSequence setShortNameSize(CharSequence cs) {
		float length = cs.length();
		if (length < 3f) {
			length = 3f;
		} else if (length > 12f) {
			length = 12f;
		}
		return SpanUtils.setAll(cs, SpanUtils.getNewPercentSizeSpan(3.00f / length));
	}

	public static class ShortNameComparator implements Comparator<Route>, MTLog.Loggable {

		private static final String TAG = Route.class.getSimpleName() + ">" + ShortNameComparator.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final Pattern DIGITS = Pattern.compile("[\\d]+");

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
					boolean rNoDigits = !StringUtils.hasDigits(rShortName, true);
					boolean lNoDigits = !StringUtils.hasDigits(lShortName, true);
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

	public long getId() {
		return id;
	}

	protected void setId(long id) {
		this.id = id;
	}

	public String getShortestName() {
		if (TextUtils.isEmpty(getShortName())) {
			return getLongName();
		}
		return getShortName();
	}

	public String getLongestName() {
		if (TextUtils.isEmpty(getLongName())) {
			return getShortName();
		}
		return getLongName();
	}

	public String getShortName() {
		return shortName;
	}

	protected void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getLongName() {
		return longName;
	}

	protected void setLongName(String longName) {
		this.longName = longName;
	}
}
