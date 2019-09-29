package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.StatusProviderContract;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AvailabilityPercent extends POIStatus implements MTLog.Loggable {

	private static final String TAG_TAG = AvailabilityPercent.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG_TAG;
	}

	// the higher the status integer value is, the more important it is
	private static final int STATUS_OK = 0;
	private static final int STATUS_LOCKED = 1;
	private static final int STATUS_CLOSED = 2;
	private static final int STATUS_NOT_PUBLIC = 99;
	private static final int STATUS_NOT_INSTALLED = 100;

	private static final int ENOUGH_AVAILABILITY = 3;

	private int value1;
	private int value2;
	@Nullable
	private Integer value1SubValue1;

	private String value1EmptyRes;
	private String value1QuantityRes;
	@ColorInt
	private int value1Color;
	@ColorInt
	private int value1ColorBg;

	@Nullable
	private String value1SubValue1EmptyRes;
	@Nullable
	private String value1SubValue1QuantityRes;
	@ColorInt
	@Nullable
	private Integer value1SubValue1Color;
	@ColorInt
	@Nullable
	private Integer value1SubValue1ColorBg;

	private String value2EmptyRes;
	private String value2QuantityRes;
	@ColorInt
	private int value2Color;
	@ColorInt
	private int value2ColorBg;

	private int statusMsgId = STATUS_OK;

	public AvailabilityPercent(@NonNull POIStatus status) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs(), status.getReadFromSourceAtInMs());
	}

	public AvailabilityPercent(@NonNull String targetUUID, long lastUpdateMs, long maxValidityInMs, long readFromSourceAtInMs) {
		this(null, targetUUID, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs);
	}

	public AvailabilityPercent(@Nullable Integer id, @NonNull String targetUUID, long lastUpdateMs, long maxValidityInMs, long readFromSourceAtInMs) {
		this(id, targetUUID, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, false);
	}

	public AvailabilityPercent(@Nullable Integer id, @NonNull String targetUUID, long lastUpdateMs, long maxValidityInMs, long readFromSourceAtInMs, boolean noData) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, noData);
	}

	public boolean hasValueStrictlyLowerThan(int underThisValue) {
		return this.value1 < underThisValue || this.value2 < underThisValue;
	}

	public int getLowerValue() {
		return this.value1 < this.value2 ? this.value1 : this.value2;
	}

	public boolean isShowingLowerValue() {
		return hasValueStrictlyLowerThan(ENOUGH_AVAILABILITY)
				&& this.value1 != this.value2;
	}

	@ColorInt
	public int getLowerValueColor() {
		return this.value1 < this.value2 ? this.value1Color : this.value2Color;
	}

	@ColorInt
	public int getLowerValueColorBg() {
		return this.value1 < this.value2 ? this.value1ColorBg : this.value2ColorBg;
	}

	@NonNull
	public CharSequence getLowerValueText(@NonNull Context context) {
		if (this.value1 < this.value2) {
			return getValue1Text(context);
		} else {
			return getValue2Text(context);
		}
	}

	@NonNull
	public CharSequence getValue1Text(@NonNull Context context) {
		return getValue1Text(context, false);
	}

	@NonNull
	public CharSequence getValue1Text(@NonNull Context context, boolean excludeSubValue1) {
		return getValueText(context,
				getValue1(excludeSubValue1),
				getValue1EmptyRes(), getValue1QuantityRes(),
				getValue1Color(), getValue1ColorBg());
	}

	@Nullable
	public CharSequence getValue1SubValue1Text(@NonNull Context context) {
		if (getValue1SubValue1() == null
				|| getValue1SubValue1EmptyRes() == null
				|| getValue1SubValue1QuantityRes() == null
				|| getValue1SubValue1Color() == null
				|| getValue1SubValue1ColorBg() == null) {
			return null;
		}
		return getValueText(context,
				getValue1SubValue1(),
				getValue1SubValue1EmptyRes(), getValue1SubValue1QuantityRes(),
				getValue1SubValue1Color(), getValue1SubValue1ColorBg());
	}

	@NonNull
	public CharSequence getValue2Text(@NonNull Context context) {
		return getValueText(context,
				getValue2(),
				getValue2EmptyRes(), getValue2QuantityRes(),
				getValue2Color(), getValue2ColorBg());
	}

	private static final TypefaceSpan VALUE_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static final StyleSpan VALUE_STYLE = SpanUtils.getNewBoldStyleSpan();

	@NonNull
	private CharSequence getValueText(@NonNull Context context,
			int value,
			String valueEmptyRes, String valueQuantityRes,
			@ColorInt int valueColor, @ColorInt int valueColorBg) {
		if (value < 0) {
			value = 0; // never show negative values
		}
		SpannableStringBuilder valueTextSSB = new SpannableStringBuilder( //
				StringUtils.getEmptyOrPluralsIdentifier(context, valueEmptyRes, valueQuantityRes, value)
		);
		valueTextSSB = SpanUtils.setAll(valueTextSSB, //
				VALUE_FONT, SpanUtils.getNewTextColor(ColorUtils.getDarkerColor(valueColor, valueColorBg)));
		if (value == 0) {
			valueTextSSB = SpanUtils.setAll(valueTextSSB, VALUE_STYLE);
		}
		return valueTextSSB;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isStatusOK() {
		return this.statusMsgId == STATUS_OK;
	}

	@Nullable
	private static ForegroundColorSpan statusTextColor = null;

	@NonNull
	private static ForegroundColorSpan getStatusTextColor(@NonNull Context context) {
		if (statusTextColor == null) {
			statusTextColor = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusTextColor;
	}

	private static final TypefaceSpan STATUS_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static final StyleSpan STATUS_STYLE = SpanUtils.getNewBoldStyleSpan();

	@Nullable
	public CharSequence getStatusMsg(@NonNull Context context) {
		SpannableStringBuilder statusMsbSSB;
		switch (this.statusMsgId) {
		case STATUS_NOT_INSTALLED:
			statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.not_installed));
			break;
		case STATUS_NOT_PUBLIC:
			statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.not_public));
			break;
		case STATUS_LOCKED:
			statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.locked));
			break;
		case STATUS_CLOSED:
			statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.closed));
			break;
		default:
			statusMsbSSB = new SpannableStringBuilder(context.getString(R.string.ellipsis));
			MTLog.w(this, "Unexpected availability percent status '%s'!", this.statusMsgId);
		}
		return SpanUtils.setAll(statusMsbSSB, //
				getStatusTextColor(context), STATUS_STYLE, STATUS_FONT);
	}

	// VALUE 1

	public void setValue1(int value1) {
		this.value1 = value1;
	}

	public int getValue1() {
		return getValue1(false);
	}

	public int getValue1(boolean excludeSubValue1) {
		if (excludeSubValue1) {
			return this.value1 - getValue1SubValue1(0);
		}
		return this.value1;
	}

	public void setValue1EmptyRes(@NonNull String value1EmptyRes) {
		this.value1EmptyRes = value1EmptyRes;
	}

	@Nullable
	public String getValue1EmptyRes() {
		return value1EmptyRes;
	}

	public void setValue1QuantityRes(@NonNull String value1QuantityRes) {
		this.value1QuantityRes = value1QuantityRes;
	}

	@Nullable
	public String getValue1QuantityRes() {
		return value1QuantityRes;
	}

	public void setValue1Color(@ColorInt int value1Color) {
		this.value1Color = value1Color;
	}

	@ColorInt
	public int getValue1Color() {
		return value1Color;
	}

	public void setValue1ColorBg(@ColorInt int value1ColorBg) {
		this.value1ColorBg = value1ColorBg;
	}

	@ColorInt
	public int getValue1ColorBg() {
		return value1ColorBg;
	}

	// VALUE 1 SUB-VALUE 1

	public void setValue1SubValue1(@Nullable Integer value1SubValue1) {
		this.value1SubValue1 = value1SubValue1;
	}

	@Nullable
	public Integer getValue1SubValue1() {
		return value1SubValue1;
	}

	public int getValue1SubValue1(int defaultValue1SubValue1) {
		return value1SubValue1 == null ? defaultValue1SubValue1 : value1SubValue1;
	}

	public void setValue1SubValue1EmptyRes(@NonNull String value1SubValue1EmptyRes) {
		this.value1SubValue1EmptyRes = value1SubValue1EmptyRes;
	}

	@Nullable
	public String getValue1SubValue1EmptyRes() {
		return value1SubValue1EmptyRes;
	}

	public void setValue1SubValue1QuantityRes(@NonNull String value1SubValue1QuantityRes) {
		this.value1SubValue1QuantityRes = value1SubValue1QuantityRes;
	}

	@Nullable
	public String getValue1SubValue1QuantityRes() {
		return value1SubValue1QuantityRes;
	}

	public void setValue1SubValue1Color(@Nullable @ColorInt Integer value1SubValue1Color) {
		this.value1SubValue1Color = value1SubValue1Color;
	}

	@ColorInt
	@Nullable
	public Integer getValue1SubValue1Color() {
		return value1SubValue1Color;
	}

	public void setValue1SubValue1ColorBg(@Nullable @ColorInt Integer value1SubValue1ColorBg) {
		this.value1SubValue1ColorBg = value1SubValue1ColorBg;
	}

	@ColorInt
	@Nullable
	public Integer getValue1SubValue1ColorBg() {
		return value1SubValue1ColorBg;
	}

	// VALUE 2

	public void setValue2(int value2) {
		this.value2 = value2;
	}

	public int getValue2() {
		return value2;
	}

	public void setValue2EmptyRes(@NonNull String value2EmptyRes) {
		this.value2EmptyRes = value2EmptyRes;
	}

	@Nullable
	public String getValue2EmptyRes() {
		return value2EmptyRes;
	}

	public void setValue2QuantityRes(@NonNull String value2QuantityRes) {
		this.value2QuantityRes = value2QuantityRes;
	}

	@Nullable
	public String getValue2QuantityRes() {
		return value2QuantityRes;
	}

	public void setValue2Color(@ColorInt int value2Color) {
		this.value2Color = value2Color;
	}

	@ColorInt
	public int getValue2Color() {
		return value2Color;
	}

	public void setValue2ColorBg(@ColorInt int value2ColorBg) {
		this.value2ColorBg = value2ColorBg;
	}

	@ColorInt
	public int getValue2ColorBg() {
		return value2ColorBg;
	}

	public int getTotalValue() {
		return this.value1 + this.value2;
	}

	public void setStatusInstalled(boolean installed) {
		if (!installed) {
			incStatusId(STATUS_NOT_INSTALLED);
		}
	}

	public void setStatusPublic(boolean isPublic) {
		if (!isPublic) {
			incStatusId(STATUS_NOT_PUBLIC);
		}
	}

	public void setStatusLocked(boolean locked) {
		if (locked) {
			incStatusId(STATUS_LOCKED);
		}
	}

	public void setStatusClosed(boolean closed) {
		if (closed) {
			incStatusId(STATUS_CLOSED);
		}
	}

	public void incStatusId(int statusId) {
		if (this.statusMsgId < statusId) {
			this.statusMsgId = statusId;
		}
	}

	public int getStatusMsgId() {
		return statusMsgId;
	}

	@Nullable
	public static AvailabilityPercent fromCursorWithExtra(@NonNull Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	@Nullable
	private static AvailabilityPercent fromExtraJSONString(@NonNull POIStatus status, @Nullable String extrasJSONString) {
		try {
			JSONObject json = extrasJSONString == null ? null : new JSONObject(extrasJSONString);
			if (json == null) {
				return null;
			}
			return fromExtraJSON(status, json);
		} catch (JSONException jsone) {
			MTLog.w(TAG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Nullable
	private static AvailabilityPercent fromExtraJSON(@NonNull POIStatus status, @NonNull JSONObject extrasJSON) {
		try {
			AvailabilityPercent availabilityPercent = new AvailabilityPercent(status);
			availabilityPercent.statusMsgId = extrasJSON.getInt(JSON_STATUS_MSG_ID);
			availabilityPercent.value1 = extrasJSON.getInt(JSON_VALUE1);
			availabilityPercent.value1EmptyRes = extrasJSON.getString(JSON_VALUE1_EMPTY_RES);
			availabilityPercent.value1QuantityRes = extrasJSON.getString(JSON_VALUE1_QUANTITY_RES);
			availabilityPercent.value1Color = extrasJSON.getInt(JSON_VALUE1_COLOR);
			availabilityPercent.value1ColorBg = extrasJSON.getInt(JSON_VALUE1_COLOR_BG);
			availabilityPercent.value1SubValue1 = extrasJSON.optInt(JSON_VALUE1_SUB_VALUE1);
			availabilityPercent.value1SubValue1EmptyRes = extrasJSON.getString(JSON_VALUE1_SUB_VALUE1_EMPTY_RES);
			availabilityPercent.value1SubValue1QuantityRes = extrasJSON.getString(JSON_VALUE1_SUB_VALUE1_QUANTITY_RES);
			availabilityPercent.value1SubValue1Color = extrasJSON.getInt(JSON_VALUE1_SUB_VALUE1_COLOR);
			availabilityPercent.value1SubValue1ColorBg = extrasJSON.getInt(JSON_VALUE1_SUB_VALUE1_COLOR_BG);
			availabilityPercent.value2 = extrasJSON.getInt(JSON_VALUE2);
			availabilityPercent.value2EmptyRes = extrasJSON.getString(JSON_VALUE2_EMPTY_RES);
			availabilityPercent.value2QuantityRes = extrasJSON.getString(JSON_VALUE2_QUANTITY_RES);
			availabilityPercent.value2Color = extrasJSON.getInt(JSON_VALUE2_COLOR);
			availabilityPercent.value2ColorBg = extrasJSON.getInt(JSON_VALUE2_COLOR_BG);
			return availabilityPercent;
		} catch (JSONException jsone) {
			MTLog.w(TAG_TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static final String JSON_STATUS_MSG_ID = "statusMsgId";
	private static final String JSON_VALUE1 = "value1";
	private static final String JSON_VALUE1_EMPTY_RES = "value1EmptyRes";
	private static final String JSON_VALUE1_QUANTITY_RES = "value1QuantityRes";
	private static final String JSON_VALUE1_COLOR = "value1Color";
	private static final String JSON_VALUE1_COLOR_BG = "value1ColorBg";
	private static final String JSON_VALUE1_SUB_VALUE1 = "value1SubValue1";
	private static final String JSON_VALUE1_SUB_VALUE1_EMPTY_RES = "value1SubValue1EmptyRes";
	private static final String JSON_VALUE1_SUB_VALUE1_QUANTITY_RES = "value1SubValue1QuantityRes";
	private static final String JSON_VALUE1_SUB_VALUE1_COLOR = "value1SubValue1Color";
	private static final String JSON_VALUE1_SUB_VALUE1_COLOR_BG = "value1SubValue1ColorBg";
	private static final String JSON_VALUE2 = "value2";
	private static final String JSON_VALUE2_EMPTY_RES = "value2EmptyRes";
	private static final String JSON_VALUE2_QUANTITY_RES = "value2QuantityRes";
	private static final String JSON_VALUE2_COLOR = "value2Color";
	private static final String JSON_VALUE2_COLOR_BG = "value2ColorBg";

	@Nullable
	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_STATUS_MSG_ID, this.statusMsgId);
			json.put(JSON_VALUE1, this.value1);
			json.put(JSON_VALUE1_EMPTY_RES, this.value1EmptyRes);
			json.put(JSON_VALUE1_QUANTITY_RES, this.value1QuantityRes);
			json.put(JSON_VALUE1_COLOR, this.value1Color);
			json.put(JSON_VALUE1_COLOR_BG, this.value1ColorBg);
			if (this.value1SubValue1 != null
					&& this.value1SubValue1EmptyRes != null
					&& this.value1SubValue1QuantityRes != null
					&& this.value1SubValue1Color != null
					&& this.value1SubValue1ColorBg != null) {
				json.put(JSON_VALUE1_SUB_VALUE1, this.value1SubValue1);
				json.put(JSON_VALUE1_SUB_VALUE1_EMPTY_RES, this.value1SubValue1EmptyRes);
				json.put(JSON_VALUE1_SUB_VALUE1_QUANTITY_RES, this.value1SubValue1QuantityRes);
				json.put(JSON_VALUE1_SUB_VALUE1_COLOR, this.value1SubValue1Color);
				json.put(JSON_VALUE1_SUB_VALUE1_COLOR_BG, this.value1SubValue1ColorBg);
			}
			json.put(JSON_VALUE2, this.value2);
			json.put(JSON_VALUE2_EMPTY_RES, this.value2EmptyRes);
			json.put(JSON_VALUE2_QUANTITY_RES, this.value2QuantityRes);
			json.put(JSON_VALUE2_COLOR, this.value2Color);
			json.put(JSON_VALUE2_COLOR_BG, this.value2ColorBg);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG_TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}

	@NonNull
	@Override
	public String toString() {
		return AvailabilityPercent.class.getSimpleName() + "{" +
				"value1=" + value1 +
				", value1SubValue1=" + value1SubValue1 +
				", value2=" + value2 +
				", value1EmptyRes='" + value1EmptyRes + '\'' +
				", value1QuantityRes='" + value1QuantityRes + '\'' +
				", value1Color=" + value1Color +
				", value1ColorBg=" + value1ColorBg +
				", value1SubValue1EmptyRes='" + value1SubValue1EmptyRes + '\'' +
				", value1SubValue1QuantityRes='" + value1SubValue1QuantityRes + '\'' +
				", value1SubValue1Color=" + value1SubValue1Color +
				", value1SubValue1ColorBg=" + value1SubValue1ColorBg +
				", value2EmptyRes='" + value2EmptyRes + '\'' +
				", value2QuantityRes='" + value2QuantityRes + '\'' +
				", value2Color=" + value2Color +
				", value2ColorBg=" + value2ColorBg +
				", statusMsgId=" + statusMsgId +
				'}';
	}

	public static class AvailabilityPercentStatusFilter extends StatusProviderContract.Filter {

		private static final String LOG_TAG = AvailabilityPercentStatusFilter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		public AvailabilityPercentStatusFilter(@NonNull String targetUUID) {
			super(POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, targetUUID);
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

		@Nullable
		public static StatusProviderContract.Filter fromJSON(@NonNull JSONObject json) {
			try {
				String targetUUID = StatusProviderContract.Filter.getTargetUUIDFromJSON(json);
				AvailabilityPercentStatusFilter availabilityPercentStatusFilter = new AvailabilityPercentStatusFilter(targetUUID);
				StatusProviderContract.Filter.fromJSON(availabilityPercentStatusFilter, json);
				return availabilityPercentStatusFilter;
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
		private static String toJSONString(@NonNull StatusProviderContract.Filter statusFilter) {
			JSONObject json = toJSON(statusFilter);
			return json == null ? null : json.toString();
		}

		@Nullable
		private static JSONObject toJSON(@NonNull StatusProviderContract.Filter statusFilter) {
			try {
				JSONObject json = new JSONObject();
				StatusProviderContract.Filter.toJSON(statusFilter, json);
				return json;
			} catch (JSONException jsone) {
				MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
				return null;
			}
		}
	}
}
