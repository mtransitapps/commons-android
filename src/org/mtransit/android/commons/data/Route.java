package org.mtransit.android.commons.data;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider.RouteColumns;

import android.database.Cursor;
import android.graphics.Color;
import android.text.TextUtils;

public class Route implements MTLog.Loggable {

	private static final String TAG = Route.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final ShortNameComparator SHORT_NAME_COMPATOR = new ShortNameComparator();

	public long id;
	public String shortName;
	public String longName;

	private String color;
	private String textColor;

	public static Route fromCursor(Cursor c) {
		Route route = new Route();
		route.id = c.getLong(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_LONG_NAME));
		route.setColor(c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_COLOR)));
		route.setTextColor(c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_TEXT_COLOR)));
		return route;
	}

	public void setColor(String color) {
		this.color = color;
		this.colorInt = null;
	}

	public String getColor() {
		return color;
	}

	private Integer colorInt = null;

	public int getColorInt() {
		if (this.colorInt == null) {
			this.colorInt = ColorUtils.parseColor(getColor());
		}
		return this.colorInt;
	}

	public void setTextColor(String textColor) {
		this.textColor = textColor;
		this.textColorInt = null;
	}

	protected String getTextColor() {
		return "FFFFFF"; // Color.WHITE;
	}

	private Integer textColorInt = null;

	public int getTextColorInt() {
		if (this.textColorInt == null) {
			this.textColorInt = Color.WHITE;
		}
		return this.textColorInt;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Route)) {
			return false;
		}
		Route otherRoute = (Route) o;
		if (this.id != otherRoute.id) {
			return false;
		}
		if (!StringUtils.equals(this.shortName, otherRoute.shortName)) {
			return false;
		}
		if (!StringUtils.equals(this.longName, otherRoute.longName)) {
			return false;
		}
		if (!StringUtils.equals(this.color, otherRoute.color)) {
			return false;
		}
		if (!StringUtils.equals(this.textColor, otherRoute.textColor)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Route.class.getSimpleName()).append(":[") //
				.append("id:").append(id).append(',') //
				.append("shortName:").append(shortName).append(',') //
				.append("longName:").append(longName).append(',') //
				.append("color:").append(color).append(',') //
				.append("textColor:").append(textColor) //
				.append(']').toString();
	}

	public static JSONObject toJSON(Route route) {
		try {
			return new JSONObject() //
					.put(JSON_ID, route.id) //
					.put(JSON_SHORT_NAME, route.shortName) //
					.put(JSON_LONG_NAME, route.longName) //
					.put(JSON_COLOR, route.color) //
					.put(JSON_TEXT_COLOR, route.textColor);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", route);
			return null;
		}
	}

	private static final String JSON_ID = "id";
	private static final String JSON_SHORT_NAME = "shortName";
	private static final String JSON_LONG_NAME = "longName";
	private static final String JSON_COLOR = "color";
	private static final String JSON_TEXT_COLOR = "textColor";

	public static Route fromJSON(JSONObject jRoute) {
		try {
			Route route = new Route();
			route.id = jRoute.getLong(JSON_ID);
			route.shortName = jRoute.getString(JSON_SHORT_NAME);
			route.longName = jRoute.getString(JSON_LONG_NAME);
			route.setColor(jRoute.getString(JSON_COLOR));
			route.setTextColor(jRoute.getString(JSON_TEXT_COLOR));
			return route;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jRoute);
			return null;
		}
	}

	public static class ShortNameComparator implements Comparator<Route> {

		private static final Pattern DIGITS = Pattern.compile("[\\d]+");

		@Override
		public int compare(Route lhs, Route rhs) {
			String lShortName = lhs == null ? StringUtils.EMPTY : lhs.shortName;
			String rShortName = lhs == null ? StringUtils.EMPTY : rhs.shortName;
			if (lShortName.equals(rShortName)) {
				return ComparatorUtils.SAME;
			}
			if (!TextUtils.isEmpty(lShortName) && !TextUtils.isEmpty(rShortName)) {
				int rDigits = -1;
				Matcher rMatcher = DIGITS.matcher(rShortName);
				if (rMatcher.find()) {
					String rDigitS = rMatcher.group();
					if (!TextUtils.isEmpty(rDigitS)) {
						rDigits = Integer.parseInt(rDigitS);
					}
				}
				int lDigits = -1;
				Matcher lMatcher = DIGITS.matcher(lShortName);
				if (lMatcher.find()) {
					String lDigitS = lMatcher.group();
					if (!TextUtils.isEmpty(lDigitS)) {
						lDigits = Integer.parseInt(lDigitS);
					}
				}
				if (rDigits != lDigits) {
					return lDigits - rDigits;
				}
			}
			return lShortName.compareTo(rShortName);
		}
	}

}
