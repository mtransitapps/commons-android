package org.mtransit.android.commons.data;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;

@SuppressWarnings({"WeakerAccess"})
public class AvailabilityPercent extends POIStatus implements MTLog.Loggable {

	protected static final String LOG_TAG = AvailabilityPercent.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	// the higher the status integer value is, the more important it is
	protected static final int STATUS_OK = 0;
	protected static final int STATUS_LOCKED = 1;
	protected static final int STATUS_CLOSED = 2;
	protected static final int STATUS_NOT_PUBLIC = 99;
	protected static final int STATUS_NOT_INSTALLED = 100;

	private static final int ENOUGH_AVAILABILITY = 3;

	@NonNull
	protected final AvailabilityPercentValue value1; // includes default + sub-value
	@Nullable
	protected final String value1SubValueDefaultEmptyRes; // excludes sub-value
	@Nullable
	protected final String value1SubValueDefaultQuantityRes; // excludes sub-value
	@Nullable
	protected final AvailabilityPercentValue value1SubValue1; // exclude default

	@NonNull
	protected final AvailabilityPercentValue value2;

	protected final int statusMsgId;

	public AvailabilityPercent(
			@NonNull POIStatus status,
			@NonNull AvailabilityPercentValue value1,
			@Nullable String value1SubValueDefaultEmptyRes,
			@Nullable String value1SubValueDefaultQuantityRes,
			@Nullable AvailabilityPercentValue value1SubValue1,
			@NonNull AvailabilityPercentValue value2,
			int statusMsgId
	) {
		this(
				status.getId(),
				status.getTargetUUID(),
				status.getLastUpdateInMs(),
				status.getMaxValidityInMs(),
				status.getReadFromSourceAtInMs(),
				status.getSourceLabel(),
				status.isNoData(),
				value1,
				value1SubValueDefaultEmptyRes,
				value1SubValueDefaultQuantityRes,
				value1SubValue1,
				value2,
				statusMsgId
		);
	}

	public AvailabilityPercent(
			@Nullable Integer id,
			@NonNull String targetUUID,
			long lastUpdateMs,
			long maxValidityInMs,
			long readFromSourceAtInMs,
			@Nullable String sourceLabel,
			boolean noData,
			@NonNull AvailabilityPercentValue value1,
			@Nullable String value1SubValueDefaultEmptyRes,
			@Nullable String value1SubValueDefaultQuantityRes,
			@Nullable AvailabilityPercentValue value1SubValue1,
			@NonNull AvailabilityPercentValue value2,
			int statusMsgId
	) {
		super(id, targetUUID, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, lastUpdateMs, maxValidityInMs, readFromSourceAtInMs, sourceLabel, noData);
		this.value1 = value1;
		this.value1SubValueDefaultEmptyRes = value1SubValueDefaultEmptyRes;
		this.value1SubValueDefaultQuantityRes = value1SubValueDefaultQuantityRes;
		this.value1SubValue1 = value1SubValue1;
		this.value2 = value2;
		this.statusMsgId = statusMsgId;
	}

	public boolean hasValueStrictlyLowerThan(int underThisValue) {
		return this.value1.getValue() < underThisValue || this.value2.getValue() < underThisValue;
	}

	public boolean isShowingLowerValue() {
		return hasValueStrictlyLowerThan(ENOUGH_AVAILABILITY)
				&& this.value1.getValue() != this.value2.getValue();
	}

	@NonNull
	public CharSequence getLowerValueText(@NonNull Context context) {
		if (this.value1.getValue() < this.value2.getValue()) {
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
		return getValueText(
				context,
				getValue1(),
				true
		);
	}

	@NonNull
	public CharSequence getValue1SubValueDefaultText(@NonNull Context context) {
		return getValueText(
				context,
				AvailabilityPercentExtKt.getValue1Default(this),
				true
		);
	}

	@Nullable
	public CharSequence getValue1SubValue1Text(@NonNull Context context) {
		if (getValue1SubValue1() == null) return null;
		return getValueText(
				context,
				getValue1SubValue1(),
				false
		);
	}

	@NonNull
	public CharSequence getValue2Text(@NonNull Context context) {
		return getValueText(
				context,
				getValue2(),
				true
		);
	}

	private static final TypefaceSpan VALUE_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	private static final StyleSpan BOLD_STYLE = SpanUtils.getNewBoldStyleSpan();

	@NonNull
	private CharSequence getValueText(
			@NonNull Context context,
			AvailabilityPercentValue availabilityPercentValue,
			boolean importantValue
	) {
		int value = availabilityPercentValue.getValue();
		if (value < 0) {
			value = 0; // never show negative values
		}
		SpannableStringBuilder valueTextSSB = new SpannableStringBuilder(
				StringUtils.getEmptyOrPluralsIdentifier(context, availabilityPercentValue.getEmptyRes(), availabilityPercentValue.getQuantityRes(), value)
		);
		valueTextSSB = SpanUtils.setAll(valueTextSSB,
				VALUE_FONT, SpanUtils.getNewTextColor(
						ColorUtils.isDarkTheme(context) ? ColorUtils.getLighterColor(availabilityPercentValue.getColor(), availabilityPercentValue.getColorBg())
								: ColorUtils.getDarkerColor(availabilityPercentValue.getColor(), availabilityPercentValue.getColorBg())));
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

	@NonNull
	public AvailabilityPercentValue getValue1() {
		return this.value1;
	}

	@Nullable
	public AvailabilityPercentValue getValue1SubValue1() {
		return value1SubValue1;
	}

	@NonNull
	public AvailabilityPercentValue getValue2() {
		return this.value2;
	}

	public int getTotalValue() {
		return this.value1.getValue() + this.value2.getValue();
	}

	@SuppressWarnings("unused")
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
				", statusMsgId=" + statusMsgId +
				'}';
	}
}

