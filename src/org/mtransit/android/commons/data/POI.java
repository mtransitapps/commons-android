package org.mtransit.android.commons.data;

import java.util.Comparator;

import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public interface POI extends MTLog.Loggable {

	public static final int ITEM_VIEW_TYPE_ROUTE_TRIP_STOP = 0;
	public static final int ITEM_VIEW_TYPE_BASIC_POI = 1;

	public static final int ITEM_STATUS_TYPE_SCHEDULE = 0;
	public static final int ITEM_STATUS_TYPE_AVAILABILITY_PERCENT = 1;

	public static final int ITEM_ACTION_TYPE_ROUTE_TRIP_STOP = 0;
	public static final int ITEM_ACTION_TYPE_FAVORITABLE = 1;
	public static final int ITEM_ACTION_TYPE_APP = 2;

	public static final POIDistanceComparator POI_DISTANCE_COMPARATOR = new POIDistanceComparator();

	public int getId();

	public void setId(int id);

	public String getName();

	public void setName(String name);

	public void setDistanceString(CharSequence distanceString);

	public Double getLat();

	public void setLat(Double lat);

	public Double getLng();

	public void setLng(Double lng);

	public boolean hasLocation();

	public CharSequence getDistanceString();

	public void setDistance(float distance);

	public float getDistance();

	public String getUUID();

	public String getAuthority();

	public void setAuthority(String authority);

	public int getType();

	public void setType(int type);

	public int getStatusType();

	public boolean hasStatus();

	public void setStatus(POIStatus status);

	public POIStatus getStatus(Context context);

	public POIStatus getStatusOrNull();

	public boolean pingStatus(Context context); // use to try to load status if not too busy

	public int getActionsType();

	public CharSequence[] getActionsItems(Context context, CharSequence defaultAction, boolean isFavorite);

	public boolean onActionItemClick(Activity activity);

	public boolean onActionsItemClick(Activity activity, int itemClicked, boolean isFavorite, POIUpdateListener listener);

	public JSONObject toJSON();

	public POI fromJSON(JSONObject json);

	public ContentValues toContentValues();

	public POI fromCursor(Cursor cursor, String authority);

	public static class POIUtils implements MTLog.Loggable {

		private static final String TAG = POIUtils.class.getSimpleName();
		
		@Override
		public String getLogTag() {
			return TAG;
		}

		public static final String UID_SEPARATOR = "-";

		public static String getUUID(String authority, Object... poiUIDs) {
			StringBuilder sb = new StringBuilder(authority);
			for (Object poiUID : poiUIDs) {
				sb.append(UID_SEPARATOR).append(poiUID);
			}
			return sb.toString();
		}

		public static String extractAuthorityFromUUID(String uuid) {
			if (TextUtils.isEmpty(uuid)) {
				return null;
			}
			final String[] split = uuid.split(UID_SEPARATOR);
			if (split.length < 1) {
				return null;
			}
			return split[0];
		}

		public static String extractPoiUIDFromUUID(String uuid) {
			if (TextUtils.isEmpty(uuid)) {
				return null;
			}
			final String[] split = uuid.split(UID_SEPARATOR);
			if (split.length < 2) {
				return null;
			}
			return split[1];
		}

	}

	public static class POIDistanceComparator implements Comparator<POI> {
		@Override
		public int compare(POI lhs, POI rhs) {
			if (lhs instanceof RouteTripStop && rhs instanceof RouteTripStop) {
				RouteTripStop alhs = (RouteTripStop) lhs;
				RouteTripStop arhs = (RouteTripStop) rhs;
				// IF same stop DO
				if (alhs.stop.id == arhs.stop.id) {
					// compare route shortName as integer
					if (!TextUtils.isEmpty(alhs.route.shortName) || !TextUtils.isEmpty(arhs.route.shortName)) {
						try {
							return Integer.valueOf(alhs.route.shortName) - Integer.valueOf(arhs.route.shortName);
						} catch (NumberFormatException nfe) {
							// compare route short name as string
							return alhs.route.shortName.compareTo(arhs.route.shortName);
						}
					}
					// TODO try sorting by trip heading?
				}
			}
			float d1 = lhs.getDistance();
			float d2 = rhs.getDistance();
			if (d1 > d2) {
				return +1;
			} else if (d1 < d2) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public interface POIUpdateListener {
		public void onPOIUpdated();
	}
}