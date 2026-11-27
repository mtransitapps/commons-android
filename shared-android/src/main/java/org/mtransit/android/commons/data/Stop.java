package org.mtransit.android.commons.data;

import static org.mtransit.android.commons.StringUtils.EMPTY;
import static org.mtransit.android.commons.data.StopExtKt.makeStop;

import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.commons.GTFSCommons;

import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class Stop {

	protected static final String LOG_TAG = Stop.class.getSimpleName();

	private final int id;

	@NonNull
	private final String code;
	@NonNull
	private final String name;

	private final double lat;
	private final double lng;

	@Accessibility.AccessibilityType
	private final int accessible;

	@Nullable
	private final Integer originalIdHash;
	@Nullable
	protected final String timeZoneId;

	protected Stop(
			int id,
			@NonNull String code,
			@NonNull String name,
			double lat,
			double lng,
			@Accessibility.AccessibilityType int accessible,
			@Nullable Integer originalIdHash,
			@Nullable String timeZoneId
	) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
		this.accessible = accessible;
		this.originalIdHash = originalIdHash;
		this.timeZoneId = timeZoneId;
	}

	@NonNull
	public static Stop fromCursor(@NonNull Cursor c) {
		return makeStop(
				CursorExtKt.getInt(c, GTFSProviderContract.StopColumns.T_STOP_K_ID),
				CursorExtKt.getString(c, GTFSProviderContract.StopColumns.T_STOP_K_CODE),
				CursorExtKt.getString(c, GTFSProviderContract.StopColumns.T_STOP_K_NAME),
				CursorExtKt.getDouble(c, GTFSProviderContract.StopColumns.T_STOP_K_LAT),
				CursorExtKt.getDouble(c, GTFSProviderContract.StopColumns.T_STOP_K_LNG),
				CursorExtKt.optIntNN(c, GTFSProviderContract.StopColumns.T_STOP_K_ACCESSIBLE, Accessibility.DEFAULT),
				CursorExtKt.optInt(c, GTFSProviderContract.StopColumns.T_STOP_K_ORIGINAL_ID_HASH, GTFSCommons.DEFAULT_ID_HASH),
				CursorExtKt.optString(c, GTFSProviderContract.StopColumns.T_STOP_K_TIMEZONE_ID, null)
		);
	}

	@NonNull
	@Override
	public String toString() {
		return Stop.class.getSimpleName() + "{" +
				"id=" + id +
				", code='" + code + '\'' +
				", name='" + name + '\'' +
				", lat=" + lat +
				", lng=" + lng +
				", a11y=" + accessible +
				", odIDHash=" + originalIdHash +
				", tz='" + timeZoneId + '\'' +
				'}';
	}

	public int getId() {
		return id;
	}

	@NonNull
	public String getUUID(@NonNull String authority) {
		return POI.POIUtils.makeUUID(authority, this.id);
	}

	@NonNull
	public String getCode() {
		return code;
	}

	@NonNull
	public String getName() {
		return name;
	}

	public double getLat() {
		return this.lat;
	}

	public double getLng() {
		return this.lng;
	}

	@Accessibility.AccessibilityType
	public int getAccessible() {
		return accessible;
	}

	@Nullable
	protected Integer getOriginalIdHash() {
		return originalIdHash;
	}

	@Nullable
	public String getOriginalIdHashString() {
		return originalIdHash == null ? null : String.valueOf(originalIdHash);
	}

	@Deprecated
	@NonNull
	public String getOriginalIdHashStringOrDefault() {
		return originalIdHash == null ? EMPTY : String.valueOf(originalIdHash);
	}

	public boolean isSameOriginalId(@Nullable String cleanedOriginalIdHash) {
		if (cleanedOriginalIdHash == null) return false;
		if (this.originalIdHash == null) return false;
		return this.originalIdHash.toString().equals(cleanedOriginalIdHash);

	}

	@Nullable
	public String getTimeZoneIdOrNull() {
		if (TextUtils.isEmpty(this.timeZoneId)) return null;
		return this.timeZoneId;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Stop)) return false;
		final Stop stop = (Stop) o;
		return id == stop.id
				&& code.equals(stop.code)
				&& name.equals(stop.name)
				&& Double.compare(lat, stop.lat) == 0
				&& Double.compare(lng, stop.lng) == 0
				&& accessible == stop.accessible
				&& Objects.equals(originalIdHash, stop.originalIdHash)
				&& Objects.equals(timeZoneId, stop.timeZoneId)
				;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				id,
				code,
				name,
				lat,
				lng,
				accessible,
				originalIdHash,
				timeZoneId
		);
	}
}
