package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public class JSONUtils {

	@Nullable
	public static Integer optInt(@NonNull JSONObject jsonObject, @NonNull String name) {
		if (jsonObject.has(name)) {
			return jsonObject.optInt(name);
		}
		return null;
	}

	@Nullable
	public static String optString(@NonNull JSONObject jsonObject, @NonNull String name) {
		if (jsonObject.has(name)) {
			return jsonObject.optString(name);
		}
		return null;
	}

	private JSONUtils() {
		// utility class
	}
}
