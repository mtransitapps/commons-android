package org.mtransit.android.commons.data;

import static org.mtransit.commons.Constants.EMPTY;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.DataSourceTypeId.DataSourceType;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.commons.CommonsApp;
import org.mtransit.commons.FeatureFlags;

import java.text.Normalizer;
import java.util.Locale;

public class DefaultPOI implements POI {

	private static final String LOG_TAG = DefaultPOI.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final String authority;
	private final int id;
	@NonNull
	private String name = EMPTY;
	private double lat = 0.0d;
	private double lng = 0.0d;
	private int accessible = Accessibility.DEFAULT;
	@ItemViewType
	private final int type;
	@DataSourceType
	private int dataSourceTypeId; // no final to support to extended type
	@ItemStatusType
	private final int statusType;
	@ItemActionType
	private final int actionsType;
	@Nullable
	private Integer scoreOpt = null; // optional

	/**
	 * @param id useful to store in DB
	 */
	public DefaultPOI(@NonNull String authority, int id, @DataSourceType int dataSourceTypeId, @ItemViewType int type, @ItemStatusType int statusType, @ItemActionType int actionsType) {
		this.authority = authority;
		this.id = id;
		this.dataSourceTypeId = dataSourceTypeId;
		this.type = type;
		this.statusType = statusType;
		this.actionsType = actionsType;
		resetUUID();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (this.getClass() != o.getClass()) {
			return false;
		}
		DefaultPOI otherPOI = (DefaultPOI) o;
		if (!this.getUUID().equals(otherPOI.getUUID())) {
			return false;
		}
		if (this.getType() != otherPOI.getType()) {
			return false;
		}
		if (this.getStatusType() != otherPOI.getStatusType()) {
			return false;
		}
		if (this.getActionsType() != otherPOI.getActionsType()) {
			return false;
		}
		//noinspection RedundantIfStatement
		if (!StringUtils.equals(this.getName(), otherPOI.getName())) {
			return false;
		}
		return true;
	}

	@NonNull
	@Override
	public String toString() {
		return DefaultPOI.class.getSimpleName() + "{" +
				"authority='" + authority + '\'' +
				", id=" + id +
				", name='" + name + '\'' +
				", type=" + type +
				", lat=" + lat +
				", lng=" + lng +
				", accessible=" + accessible +
				", dst=" + dataSourceTypeId +
				", statusType=" + statusType +
				", actionsType=" + actionsType +
				", score=" + scoreOpt +
				'}';
	}

	@DataSourceType
	@Override
	public int getDataSourceTypeId() {
		return this.dataSourceTypeId;
	}

	@Override
	public void setDataSourceTypeId(@DataSourceType int dataSourceTypeId) {
		this.dataSourceTypeId = dataSourceTypeId;
	}

	@ItemViewType
	@Override
	public int getType() {
		return this.type;
	}

	@Nullable
	private String uuid = null;

	@NonNull
	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(getAuthority(), getId());
		}
		return this.uuid;
	}

	public void resetUUID() {
		this.uuid = null;
	}

	@Override
	public int compareToAlpha(@Nullable Context contextOrNull, @Nullable POI another) {
		if (another == null) {
			return ComparatorUtils.AFTER;
		}
		String thisName = Normalizer.normalize(this.getName(), Normalizer.Form.NFD).toLowerCase(Locale.getDefault());
		String anotherName = Normalizer.normalize(another.getName(), Normalizer.Form.NFD).toLowerCase(Locale.getDefault());
		return thisName.compareTo(anotherName);
	}

	@NonNull
	@Override
	public String getAuthority() {
		return authority;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setName(@NonNull String name) {
		this.name = name;
	}

	@Override
	public void setLat(double lat) {
		this.lat = lat;
	}

	@Override
	public void setLng(double lng) {
		this.lng = lng;
	}

	@NonNull
	@Override
	public String getName() {
		if (Boolean.FALSE.equals(CommonsApp.isAndroid)) {
			return this.name;
		}
		return HtmlUtils.fromHtmlCompact(this.name).toString();
	}

	@NonNull
	@Override
	public CharSequence getLabel() {
		String name = this.name;
		if (FeatureFlags.F_ACCESSIBILITY_CONSUMER) {
			name = Accessibility.decorate(name, this.accessible, false);
		}
		return HtmlUtils.fromHtmlCompact(name);
	}

	@Override
	public double getLat() {
		return lat;
	}

	@Override
	public double getLng() {
		return lng;
	}

	public void setAccessible(int accessible) {
		this.accessible = accessible;
	}

	public int getAccessible() {
		return accessible;
	}

	@Override
	public boolean hasLocation() {
		return true;
	}

	@ItemStatusType
	@Override
	public int getStatusType() {
		return this.statusType;
	}

	@ItemActionType
	@Override
	public int getActionsType() {
		return this.actionsType;
	}

	@Override
	public void setScore(@Nullable Integer score) {
		this.scoreOpt = score;
	}

	@Nullable
	@Override
	public Integer getScore() {
		return this.scoreOpt;
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		values.put(POIProviderContract.Columns.T_POI_K_ID, getId());
		values.put(POIProviderContract.Columns.T_POI_K_NAME, getName());
		values.put(POIProviderContract.Columns.T_POI_K_LAT, getLat());
		values.put(POIProviderContract.Columns.T_POI_K_LNG, getLng());
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			values.put(POIProviderContract.Columns.T_POI_K_ACCESSIBLE, getAccessible());
		}
		values.put(POIProviderContract.Columns.T_POI_K_TYPE, getType());
		values.put(POIProviderContract.Columns.T_POI_K_STATUS_TYPE, getStatusType());
		values.put(POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE, getActionsType());
		if (getScore() != null) {
			values.put(POIProviderContract.Columns.T_POI_K_SCORE_META_OPT, getScore());
		}
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static DefaultPOI fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		final DefaultPOI defaultPOI = new DefaultPOI(
				authority,
				getIdFromCursor(c),
				getDataSourceTypeIdFromCursor(c),
				getTypeFromCursor(c),
				c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_STATUS_TYPE)),
				CursorExtKt.optIntNN(c, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE, POI.ITEM_ACTION_TYPE_NONE)
		);
		fromCursor(c, defaultPOI);
		return defaultPOI;
	}

	public static void fromCursor(@NonNull Cursor c, @NonNull DefaultPOI defaultPOI) {
		defaultPOI.setName(c.getString(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_NAME)));
		defaultPOI.setLat(c.getDouble(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_LAT)));
		defaultPOI.setLng(c.getDouble(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_LNG)));
		final int a11yIdx = FeatureFlags.F_ACCESSIBILITY_CONSUMER ? c.getColumnIndex(POIProviderContract.Columns.T_POI_K_ACCESSIBLE) : -1;
		defaultPOI.setAccessible(a11yIdx < 0 ? Accessibility.DEFAULT : c.getInt(a11yIdx));
		defaultPOI.setScore(CursorExtKt.optInt(c, POIProviderContract.Columns.T_POI_K_SCORE_META_OPT, null));
	}

	public static int getIdFromCursor(@NonNull Cursor c) {
		return c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_ID));
	}

	@DataSourceType
	public static int getDataSourceTypeIdFromCursor(@NonNull Cursor c) {
		try {
			return c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_DST_ID_META));
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while retrieving POI dst!");
			return DataSourceTypeId.INVALID; // default
		}
	}

	@ItemViewType
	public static int getTypeFromCursor(@NonNull Cursor c) {
		try {
			return c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_TYPE));
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while retrieving POI type!");
			return POI.ITEM_VIEW_TYPE_BASIC_POI; // default
		}
	}

	@Nullable
	public static POI fromJSONStatic(@NonNull JSONObject json) {
		final int type = getTypeFromJSON(json);
		switch (type) {
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return fromBasicJSONStatic(json, type);
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return RouteTripStop.fromJSONStatic(json);
		case POI.ITEM_VIEW_TYPE_MODULE:
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
		default:
			MTLog.w(LOG_TAG, "Unexpected POI type '%s'! (using default) (json: %s)", type, json);
			return fromBasicJSONStatic(json, type);
		}
	}

	@ItemViewType
	private static int getTypeFromJSON(@NonNull JSONObject json) {
		try {
			return json.getInt(JSON_TYPE);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while retrieving POI type from '%s'!", json);
			return POI.ITEM_VIEW_TYPE_BASIC_POI; // default
		}
	}

	@Nullable
	private static POI fromBasicJSONStatic(@NonNull JSONObject json, int type) {
		try {
			final DefaultPOI defaultPOI = new DefaultPOI(
					getAuthorityFromJSON(json), //
					getIdFromJSON(json), //
					getDSTypeIdFromJSON(json), //
					type, //
					json.getInt(JSON_STATUS_TYPE),
					json.optInt(JSON_ACTION_TYPE, -1)
			);
			fromJSON(json, defaultPOI);
			return defaultPOI;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@Nullable
	@Override
	public POI fromJSON(@NonNull JSONObject json) {
		try {
			final DefaultPOI defaultPOI = new DefaultPOI(
					getAuthority(),
					getIdFromJSON(json),
					getDataSourceTypeId(),
					json.getInt(JSON_TYPE),
					json.getInt(JSON_STATUS_TYPE),
					json.optInt(JSON_ACTION_TYPE, -1)
			);
			fromJSON(json, defaultPOI);
			return defaultPOI;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	private static final String JSON_AUTHORITY = "authority";
	private static final String JSON_ID = "id";
	protected static final String JSON_NAME = "name";
	protected static final String JSON_LAT = "lat";
	protected static final String JSON_LNG = "lng";
	private static final String JSON_DATA_SOURCE_TYPE_ID = "dst";
	private static final String JSON_TYPE = "type";
	private static final String JSON_STATUS_TYPE = "statusType";
	private static final String JSON_ACTION_TYPE = "actionsType";
	private static final String JSON_SCORE_OPT = "scoreOpt";

	public static void fromJSON(@NonNull JSONObject json, @NonNull POI defaultPOI) throws JSONException {
		defaultPOI.setName(json.getString(JSON_NAME));
		defaultPOI.setLat(json.getDouble(JSON_LAT));
		defaultPOI.setLng(json.getDouble(JSON_LNG));
		if (json.has(JSON_SCORE_OPT)) {
			defaultPOI.setScore(json.getInt(JSON_SCORE_OPT));
		}
	}

	public static int getIdFromJSON(@NonNull JSONObject json) throws JSONException {
		return json.getInt(JSON_ID);
	}

	@NonNull
	public static String getAuthorityFromJSON(@NonNull JSONObject json) throws JSONException {
		return json.getString(JSON_AUTHORITY);
	}

	@DataSourceType
	public static int getDSTypeIdFromJSON(@NonNull JSONObject json) throws JSONException {
		return json.getInt(JSON_DATA_SOURCE_TYPE_ID);
	}

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			toJSON(this, json);
			return json;
		} catch (JSONException jsone) {
			MTLog.w(this, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

	public static void toJSON(@NonNull DefaultPOI defaultPOI, @NonNull JSONObject json) throws JSONException {
		json.put(JSON_AUTHORITY, defaultPOI.getAuthority());
		json.put(JSON_ID, defaultPOI.getId());
		json.put(JSON_NAME, defaultPOI.getName());
		json.put(JSON_LAT, defaultPOI.getLat());
		json.put(JSON_LNG, defaultPOI.getLng());
		json.put(JSON_DATA_SOURCE_TYPE_ID, defaultPOI.getDataSourceTypeId());
		json.put(JSON_TYPE, defaultPOI.getType());
		json.put(JSON_STATUS_TYPE, defaultPOI.getStatusType());
		json.put(JSON_ACTION_TYPE, defaultPOI.getActionsType());
		if (defaultPOI.getScore() != null) {
			json.put(JSON_SCORE_OPT, defaultPOI.getScore());
		}
	}
}
