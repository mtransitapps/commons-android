package org.mtransit.android.commons.data;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider.RouteColumns;

import android.database.Cursor;
import android.text.TextUtils;

public class Route implements MTLog.Loggable {

	private static final String TAG = Route.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final ShortNameComparator SHORT_NAME_COMPATOR = new ShortNameComparator();

	public int id;
	public String shortName;
	public String longName;

	public String color;
	public String textColor;

	public static Route fromCursor(Cursor c) {
		final Route route = new Route();
		route.id = c.getInt(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_LONG_NAME));
		route.color = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_COLOR));
		route.textColor = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_TEXT_COLOR));
		return route;
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
					.put("id", route.id) //
					.put("shortName", route.shortName) //
					.put("longName", route.longName) //
					.put("color", route.color) //
					.put("textColor", route.textColor);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", route);
			return null;
		}
	}

	public static Route fromJSON(JSONObject jRoute) {
		try {
			final Route route = new Route();
			route.id = jRoute.getInt("id");
			route.shortName = jRoute.getString("shortName");
			route.longName = jRoute.getString("longName");
			route.color = jRoute.getString("color");
			route.textColor = jRoute.getString("textColor");
			return route;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jRoute);
			return null;
		}
	}

	private static class ShortNameComparator implements Comparator<Route> {

		@Override
		public int compare(Route lhs, Route rhs) {
			final String lShortName = lhs == null ? StringUtils.EMPTY : lhs.shortName;
			final String rShortName = lhs == null ? StringUtils.EMPTY : rhs.shortName;
			if (!TextUtils.isEmpty(lShortName) && !TextUtils.isEmpty(rShortName)) {
				if (TextUtils.isDigitsOnly(lShortName) && TextUtils.isDigitsOnly(rShortName)) {
					try {
						int lShortNameDigit = Integer.parseInt(lShortName);
						int rShortNameDigit = Integer.parseInt(rShortName);
						return lShortNameDigit - rShortNameDigit;
					} catch (Exception e) {
						MTLog.w(TAG, e, "Impossible to compare digit route short names '%s' & '%s'!", lhs, rhs);
					}
				}
			}
			return lShortName.compareTo(rShortName);
		}
	}

}
