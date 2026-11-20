package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DataSourceTypeId.DataSourceType;
import org.mtransit.android.commons.provider.poi.POIProviderContract;
import org.mtransit.commons.CommonsApp;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

import kotlinx.serialization.json.JsonObject;

public class DefaultPOI implements POI {

	protected static final String LOG_TAG = DefaultPOI.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + this.id;
	}

	@NonNull
	private final String authority;
	private final int id;
	@NonNull
	private final String name;
	private final double lat;
	private final double lng;
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
	public DefaultPOI(
			@NonNull String authority,
			int id,
			@DataSourceType int dataSourceTypeId,
			@ItemViewType int type,
			@ItemStatusType int statusType,
			@ItemActionType int actionsType,
			double lat,
			double lng,
			@NonNull String name
	) {
		this.authority = authority;
		this.id = id;
		this.dataSourceTypeId = dataSourceTypeId;
		this.type = type;
		this.statusType = statusType;
		this.actionsType = actionsType;
		this.lat = lat;
		this.lng = lng;
		this.name = name;
		resetUUID();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (this.getClass() != o.getClass()) return false;
		final DefaultPOI that = (DefaultPOI) o;
		return id == that.id
				&& authority.equals(that.authority)
				&& name.equals(that.name)
				&& Double.compare(lat, that.lat) == 0
				&& Double.compare(lng, that.lng) == 0
				&& accessible == that.accessible
				&& type == that.type
				&& dataSourceTypeId == that.dataSourceTypeId
				&& statusType == that.statusType
				&& actionsType == that.actionsType
				&& Objects.equals(scoreOpt, that.scoreOpt)
				;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				id,
				authority,
				name,
				lat,
				lng,
				accessible,
				type,
				dataSourceTypeId,
				statusType,
				actionsType,
				scoreOpt
		);
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
			this.uuid = POI.POIUtils.makeUUID(getAuthority(), getId());
		}
		return this.uuid;
	}

	public void resetUUID() {
		this.uuid = null;
	}

	@Override
	public int compareToAlpha(@Nullable POI another) {
		if (another == null) return ComparatorUtils.BEFORE;
		if (!(this instanceof RouteDirectionStop) && another instanceof RouteDirectionStop) {
			return ComparatorUtils.AFTER; // RDS before other type always (needed for stable comparator)
		}
		final String thisName = Normalizer.normalize(this.getName(), Normalizer.Form.NFD).toLowerCase(Locale.getDefault());
		final String anotherName = Normalizer.normalize(another.getName(), Normalizer.Form.NFD).toLowerCase(Locale.getDefault());
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
		return HtmlUtils.fromHtmlCompact(
				Accessibility.decorate(this.name, this.accessible, false)
		);
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
		final ContentValues values = new ContentValues();
		values.put(POIProviderContract.Columns.T_POI_K_ID, getId());
		values.put(POIProviderContract.Columns.T_POI_K_NAME, getName());
		values.put(POIProviderContract.Columns.T_POI_K_LAT, getLat());
		values.put(POIProviderContract.Columns.T_POI_K_LNG, getLng());
		values.put(POIProviderContract.Columns.T_POI_K_ACCESSIBLE, getAccessible());
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
				c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_TYPE)),
				c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_STATUS_TYPE)),
				CursorExtKt.optIntNN(c, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE, POI.ITEM_ACTION_TYPE_NONE),
				getLatFromCursor(c),
				getLngFromCursor(c),
				getNameFromCursor(c)
		);
		fromCursor(c, defaultPOI);
		return defaultPOI;
	}

	public static void fromCursor(@NonNull Cursor c, @NonNull DefaultPOI defaultPOI) {
		defaultPOI.setAccessible(CursorExtKt.optIntNN(c, POIProviderContract.Columns.T_POI_K_ACCESSIBLE, Accessibility.DEFAULT));
		defaultPOI.setScore(CursorExtKt.optInt(c, POIProviderContract.Columns.T_POI_K_SCORE_META_OPT, null));
	}

	public static int getIdFromCursor(@NonNull Cursor c) {
		return c.getInt(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_ID));
	}

	public static double getLatFromCursor(@NonNull Cursor c) {
		return c.getDouble(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_LAT));
	}

	public static double getLngFromCursor(@NonNull Cursor c) {
		return c.getDouble(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_LNG));
	}

	@NonNull
	public static String getNameFromCursor(@NonNull Cursor c) {
		return c.getString(c.getColumnIndexOrThrow(POIProviderContract.Columns.T_POI_K_NAME));
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

	@NonNull
	@Override
	public JsonObject toJson() {
		return DefaultPOIExtKt.toJsonK(this);
	}
}
