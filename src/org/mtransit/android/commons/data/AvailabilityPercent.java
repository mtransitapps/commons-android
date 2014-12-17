package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.StatusFilter;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;

public class AvailabilityPercent extends POIStatus implements MTLog.Loggable {

	private static final String TAG = AvailabilityPercent.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	// the higher the status integer value is, the more important it is
	private static final int STATUS_OK = 0;
	private static final int STATUS_LOCKED = 1;
	private static final int STATUS_CLOSED = 2;
	private static final int STATUS_NOT_PUBLIC = 99;
	private static final int STATUS_NOT_INSTALLED = 100;

	private int value1;
	private int value2;

	private String value1EmptyRes;
	private String value1QuantityRes;
	private int value1Color;
	private int value1ColorBg;

	private String value2EmptyRes;
	private String value2QuantityRes;
	private int value2Color;
	private int value2ColorBg;

	private int statusMsgId = STATUS_OK;

	public AvailabilityPercent(POIStatus status) {
		this(status.getId(), status.getTargetUUID(), status.getLastUpdateInMs(), status.getMaxValidityInMs());
	}

	public AvailabilityPercent(String targetUUID, long lastUpdateMs, long maxValidityInMs) {
		this(null, targetUUID, lastUpdateMs, maxValidityInMs);
	}

	public AvailabilityPercent(Integer id, String targetUUID, long lastUpdateMs, long maxValidityInMs) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, lastUpdateMs, maxValidityInMs);
	}

	@Override
	public boolean isUseful() {
		return super.isUseful();
	}

	public boolean hasValueStrictlyLowerThan(int underThisValue) {
		return this.value1 < underThisValue || this.value2 < underThisValue;
	}

	public int getLowerValue() {
		return this.value1 < this.value2 ? this.value1 : this.value2;
	}

	public boolean isShowingLowerValue() {
		return hasValueStrictlyLowerThan(3) && this.value1 != this.value2;
	}

	public int getLowerValueColor() {
		return this.value1 < this.value2 ? this.value1Color : this.value2Color;
	}

	public int getLowerValueColorBg() {
		return this.value1 < this.value2 ? this.value1ColorBg : this.value2ColorBg;
	}

	public CharSequence getLowerValueText(Context context) {
		if (this.value1 < this.value2) {
			return getValue1Text(context);
		} else {
			return getValue2Text(context);
		}
	}

	public CharSequence getValue1Text(Context context) {
		SpannableStringBuilder value1TextSSB = new SpannableStringBuilder();
		value1TextSSB.append(StringUtils.getEmptyOrPluralsIdentifier(context, getValue1EmptyRes(), getValue1QuantityRes(), getValue1()));
		SpanUtils.set(value1TextSSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(value1TextSSB, SpanUtils.getTextColor(ColorUtils.getDarkerColor(getValue1Color(), getValue1ColorBg())));
		if (getValue1() == 0) {
			SpanUtils.set(value1TextSSB, SpanUtils.BOLD_STYLE_SPAN);
		}
		return value1TextSSB;
	}

	public CharSequence getValue2Text(Context context) {
		SpannableStringBuilder value2TextSSB = new SpannableStringBuilder();
		value2TextSSB.append(StringUtils.getEmptyOrPluralsIdentifier(context, getValue2EmptyRes(), getValue2QuantityRes(), getValue2()));
		SpanUtils.set(value2TextSSB, POIStatus.STATUS_TEXT_FONT);
		SpanUtils.set(value2TextSSB, SpanUtils.getTextColor(ColorUtils.getDarkerColor(getValue2Color(), getValue2ColorBg())));
		if (getValue2() == 0) {
			SpanUtils.set(value2TextSSB, SpanUtils.BOLD_STYLE_SPAN);
		}
		return value2TextSSB;
	}

	public boolean isStatusOK() {
		return this.statusMsgId == STATUS_OK;
	}

	public CharSequence getStatusMsg(Context context) {
		SpannableStringBuilder statusMsbSSB = new SpannableStringBuilder();
		switch (this.statusMsgId) {
		case STATUS_NOT_INSTALLED:
			statusMsbSSB.append(context.getString(R.string.not_installed));
			break;
		case STATUS_NOT_PUBLIC:
			statusMsbSSB.append(context.getString(R.string.not_public));
			break;
		case STATUS_LOCKED:
			statusMsbSSB.append(context.getString(R.string.locked));
			break;
		case STATUS_CLOSED:
			statusMsbSSB.append(context.getString(R.string.closed));
			break;
		default:
			statusMsbSSB.append(context.getString(R.string.ellipsis));
			MTLog.w(this, "Unexpected availability percent status '%s'!", this.statusMsgId);
		}
		SpanUtils.set(statusMsbSSB, POIStatus.getDefaultStatusTextColorSpan(context));
		SpanUtils.set(statusMsbSSB, SpanUtils.BOLD_STYLE_SPAN);
		SpanUtils.set(statusMsbSSB, POIStatus.STATUS_TEXT_FONT);
		return statusMsbSSB;
	}
	public void setValue1(int value1) {
		this.value1 = value1;
	}

	public int getValue1() {
		return value1;
	}

	public void setValue1EmptyRes(String value1EmptyRes) {
		this.value1EmptyRes = value1EmptyRes;
	}

	public String getValue1EmptyRes() {
		return value1EmptyRes;
	}

	public void setValue1QuantityRes(String value1QuantityRes) {
		this.value1QuantityRes = value1QuantityRes;
	}

	public String getValue1QuantityRes() {
		return value1QuantityRes;
	}

	public void setValue1Color(int value1Color) {
		this.value1Color = value1Color;
	}

	public int getValue1Color() {
		return value1Color;
	}

	public void setValue1ColorBg(int value1ColorBg) {
		this.value1ColorBg = value1ColorBg;
	}

	public int getValue1ColorBg() {
		return value1ColorBg;
	}

	public void setValue2(int value2) {
		this.value2 = value2;
	}

	public int getValue2() {
		return value2;
	}

	public void setValue2EmptyRes(String value2EmptyRes) {
		this.value2EmptyRes = value2EmptyRes;
	}

	public String getValue2EmptyRes() {
		return value2EmptyRes;
	}

	public void setValue2QuantityRes(String value2QuantityRes) {
		this.value2QuantityRes = value2QuantityRes;
	}

	public String getValue2QuantityRes() {
		return value2QuantityRes;
	}

	public void setValue2Color(int value2Color) {
		this.value2Color = value2Color;
	}

	public int getValue2Color() {
		return value2Color;
	}

	public void setValue2ColorBg(int value2ColorBg) {
		this.value2ColorBg = value2ColorBg;
	}

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

	public static AvailabilityPercent fromCursor(Cursor cursor) {
		POIStatus status = POIStatus.fromCursor(cursor);
		String extrasJSONString = POIStatus.getExtrasFromCursor(cursor);
		return fromExtraJSONString(status, extrasJSONString);
	}

	private static AvailabilityPercent fromExtraJSONString(POIStatus status, String extrasJSONString) {
		try {
			return fromExtraJSON(status, new JSONObject(extrasJSONString));
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	private static AvailabilityPercent fromExtraJSON(POIStatus status, JSONObject extrasJSON) {
		try {
			AvailabilityPercent availabilityPercent = new AvailabilityPercent(status);
			availabilityPercent.statusMsgId = extrasJSON.getInt("statusMsgId");
			availabilityPercent.value1 = extrasJSON.getInt("value1");
			availabilityPercent.value1EmptyRes = extrasJSON.getString("value1EmptyRes");
			availabilityPercent.value1QuantityRes = extrasJSON.getString("value1QuantityRes");
			availabilityPercent.value1Color = extrasJSON.getInt("value1Color");
			availabilityPercent.value1ColorBg = extrasJSON.getInt("value1ColorBg");
			availabilityPercent.value2 = extrasJSON.getInt("value2");
			availabilityPercent.value2EmptyRes = extrasJSON.getString("value2EmptyRes");
			availabilityPercent.value2QuantityRes = extrasJSON.getString("value2QuantityRes");
			availabilityPercent.value2Color = extrasJSON.getInt("value2Color");
			availabilityPercent.value2ColorBg = extrasJSON.getInt("value2ColorBg");
			return availabilityPercent;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while retrieving extras information from cursor.");
			return null;
		}
	}

	@Override
	public JSONObject getExtrasJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("statusMsgId", this.statusMsgId);
			json.put("value1", this.value1);
			json.put("value1EmptyRes", this.value1EmptyRes);
			json.put("value1QuantityRes", this.value1QuantityRes);
			json.put("value1Color", this.value1Color);
			json.put("value1ColorBg", this.value1ColorBg);
			json.put("value2", this.value2);
			json.put("value2EmptyRes", this.value2EmptyRes);
			json.put("value2QuantityRes", this.value2QuantityRes);
			json.put("value2Color", this.value2Color);
			json.put("value2ColorBg", this.value2ColorBg);
			return json;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while converting object '%s' to JSON!", this);
			return null; // no partial result
		}
	}


	public static class AvailabilityPercentStatusFilter extends StatusFilter {

		private static final String TAG = AvailabilityPercentStatusFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public AvailabilityPercentStatusFilter(String targetUUID) {
			super(POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, targetUUID);
		}

		@Override
		public StatusFilter fromJSONStringStatic(String jsonString) {
			return fromJSONString(jsonString);
		}

		public static StatusFilter fromJSONString(String jsonString) {
			try {
				return fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		public static StatusFilter fromJSON(JSONObject json) {
			try {
				String targetUUID = StatusFilter.getTargetUUIDFromJSON(json);
				AvailabilityPercentStatusFilter availabilityPercentStatusFilter = new AvailabilityPercentStatusFilter(targetUUID);
				StatusFilter.fromJSON(availabilityPercentStatusFilter, json);
				return availabilityPercentStatusFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		@Override
		public String toJSONStringStatic(StatusFilter statusFilter) {
			return toJSONString(statusFilter);
		}

		private static String toJSONString(StatusFilter statusFilter) {
			try {
				return toJSON(statusFilter).toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", statusFilter);
				return null;
			}
		}

		private static JSONObject toJSON(StatusFilter statusFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				StatusFilter.toJSON(statusFilter, json);
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", statusFilter);
				return null;
			}
		}

	}

}
