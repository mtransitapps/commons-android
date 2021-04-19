package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

@SuppressWarnings("unused")
public class JSONUtils {

	public static int optInt(@Nullable JSONObject jsonObject, @NonNull String name, int fallback) {
		return jsonObject == null ? fallback : jsonObject.optInt(name, fallback);
	}

	@Nullable
	public static Integer optInt(@NonNull JSONObject jsonObject, @NonNull String name) {
		if (jsonObject.has(name)) {
			return jsonObject.optInt(name);
		}
		return null;
	}

	@NonNull
	public static String optString(@Nullable JSONObject jsonObject, @NonNull String name, @NonNull String fallback) {
		return jsonObject == null ? fallback : jsonObject.optString(name, fallback);
	}

	@Nullable
	public static String optString(@NonNull JSONObject jsonObject, @NonNull String name) {
		if (jsonObject.has(name)) {
			return jsonObject.optString(name);
		}
		return null;
	}

	public static boolean optBoolean(@Nullable JSONObject jsonObject, @NonNull String name, boolean fallback) {
		return jsonObject == null ? fallback : jsonObject.optBoolean(name, fallback);
	}

	private JSONUtils() {
		// utility class
	}
}
