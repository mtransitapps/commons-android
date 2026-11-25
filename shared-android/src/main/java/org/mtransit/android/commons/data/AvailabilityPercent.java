package org.mtransit.android.commons.data;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AvailabilityPercent extends POIStatus implements MTLog.Loggable {

	protected static final String LOG_TAG = AvailabilityPercent.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	// the higher the status integer value is, the more important it is
	private static final int STATUS_OK = 0;
	private static final int STATUS_LOCKED = 1;
	private static final int STATUS_CLOSED = 2;
	private static final int STATUS_NOT_PUBLIC = 99;
	private static final int STATUS_NOT_INSTALLED = 100;

	private static final int ENOUGH_AVAILABILITY = 3;

	protected int value1;
	@Nullable
	protected String value1EmptyRes;
	@Nullable
	protected String value1QuantityRes;
	@ColorInt
	protected int value1Color;
	@ColorInt
	protected int value1ColorBg;

	@Nullable
	protected String value1SubValueDefaultEmptyRes;
	@Nullable
	protected String value1SubValueDefaultQuantityRes;

	@Nullable
	protected Integer value1SubValue1;
	@Nullable
	protected String value1SubValue1EmptyRes;
	@Nullable
	protected String value1SubValue1QuantityRes;
	@ColorInt
	@Nullable
	protected Integer value1SubValue1Color;
	@ColorInt
	@Nullable
	protected Integer value1SubValue1ColorBg;

	protected int value2;
	@Nullable
	protected String value2EmptyRes;
	@Nullable
	protected String value2QuantityRes;
	@ColorInt
	protected int value2Color;
	@ColorInt
	protected int value2ColorBg;

	protected int statusMsgId = STATUS_OK;

	public AvailabilityPercent(@NonNull POIStatus status) {
		this(
				status.getId(),
				status.getTargetUUID(),
				status.getLastUpdateInMs(),
				status.getMaxValidityInMs(),
				status.getReadFromSourceAtInMs(),
				status.getSourceLabel(),
				status.isNoData()
		);
	}

	public AvailabilityPercent(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			@Nullable String sourceLabel
	) {
		this(id, targetUUID, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, false);
	}

	public AvailabilityPercent(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			@Nullable String sourceLabel,
			boolean noData
	) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, noData);
	}

	public boolean hasValueStrictlyLowerThan(int underThisValue) {
		return this.value1 < underThisValue || this.value2 < underThisValue;
	}

	public int getLowerValue() {
		return Math.min(this.value1, this.value2);
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
				getValue1EmptyRes(),
				getValue1QuantityRes(),
				getValue1Color(),
				getValue1ColorBg(),
				true);
	}

	@NonNull
	public CharSequence getValue1SubValueDefaultText(@NonNull Context context) {
		return getValueText(context,
				getValue1SubValueDefault(),
				getValue1SubValueDefaultEmptyRes(),
				getValue1SubValueDefaultQuantityRes(),
				getValue1SubValueDefaultColor(),
				getValue1SubValueDefaultColorBg(),
				true);
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
				getValue1SubValue1EmptyRes(),
				getValue1SubValue1QuantityRes(),
				getValue1SubValue1Color(),
				getValue1SubValue1ColorBg(),
				false
		);
	}

	@NonNull
	public CharSequence getValue2Text(@NonNull Context context) {
		return getValueText(context,
				getValue2(),
				getValue2EmptyRes(),
				getValue2QuantityRes(),
				getValue2Color(),
				getValue2ColorBg(),
				true);
	}

	private static final TypefaceSpan VALUE_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static final StyleSpan BOLD_STYLE = SpanUtils.getNewBoldStyleSpan();

	@NonNull
	private CharSequence getValueText(
			@NonNull Context context,
			int value,
			String valueEmptyRes, String valueQuantityRes,
			@ColorInt int valueColor, @ColorInt int valueColorBg,
			boolean importantValue
	) {
		if (value < 0) {
			value = 0; // never show negative values
		}
		SpannableStringBuilder valueTextSSB = new SpannableStringBuilder( //
				StringUtils.getEmptyOrPluralsIdentifier(context, valueEmptyRes, valueQuantityRes, value)
		);
		valueTextSSB = SpanUtils.setAll(valueTextSSB, //
				VALUE_FONT, SpanUtils.getNewTextColor(
						ColorUtils.isDarkTheme(context) ? ColorUtils.getLighterColor(valueColor, valueColorBg) : ColorUtils.getDarkerColor(valueColor, valueColorBg)));
		if (importantValue && value == 0) {
			valueTextSSB = SpanUtils.setAll(valueTextSSB, BOLD_STYLE);
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

	public static void resetColorCache() {
		statusTextColor = null;
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

	// VALUE 1 SUB-VALUE DEFAULT

	public int getValue1SubValueDefault() {
		return getValue1(true);
	}

	public void setValue1SubValueDefaultEmptyRes(@NonNull String value1SubValueDefaultEmptyRes) {
		this.value1SubValueDefaultEmptyRes = value1SubValueDefaultEmptyRes;
	}

	@Nullable
	public String getValue1SubValueDefaultEmptyRes() {
		return value1SubValueDefaultEmptyRes;
	}

	public void setValue1SubValueDefaultQuantityRes(@NonNull String value1SubValueDefaultQuantityRes) {
		this.value1SubValueDefaultQuantityRes = value1SubValueDefaultQuantityRes;
	}

	@Nullable
	public String getValue1SubValueDefaultQuantityRes() {
		return value1SubValueDefaultQuantityRes;
	}

	@ColorInt
	public int getValue1SubValueDefaultColor() {
		return getValue1Color();
	}

	@ColorInt
	public int getValue1SubValueDefaultColorBg() {
		return getValue1ColorBg();
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
		return fromExtraJsonString(
				POIStatus.fromCursor(cursor),
				POIStatus.getExtrasFromCursor(cursor)
		);
	}

	@Nullable
	private static AvailabilityPercent fromExtraJsonString(@NonNull POIStatus status, @Nullable String extrasJsonString) {
		return AvailabilityPercentExtKt.toAvailabilityPercentOrNull(extrasJsonString, status);
	}

	@NonNull
	@Override
	public String getExtrasJsonString() {
		return AvailabilityPercentExtKt.toJsonString(this);
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
}

