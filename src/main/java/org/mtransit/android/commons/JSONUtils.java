package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unused")
public class JSONUtils implements MTLog.Loggable {

	private static final String LOG_TAG = JSONUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static int optInt(@Nullable JSONObject jsonObject, @NonNull String name, int fallback) {
		return jsonObject == null ? fallback : jsonObject.optInt(name, fallback);
	}

	@Nullable
	public static Integer optInt(@NonNull JSONObject jsonObject, @NonNull String name) {
		return optInt(jsonObject, name, null);
	}

	@Nullable
	public static Integer optInt(@NonNull JSONObject jsonObject, @NonNull String name, @Nullable Integer fallback) {
		if (jsonObject.has(name)) {
			return jsonObject.optInt(name);
		}
		return fallback;
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

	@Nullable
	public static Boolean optBoolean(@NonNull JSONObject jsonObject, @NonNull String name) {
		return optBoolean(jsonObject, name, null);
	}

	@Nullable
	public static Boolean optBoolean(@NonNull JSONObject jsonObject, @NonNull String name, @Nullable Boolean fallback) {
		if (jsonObject.has(name)) {
			return jsonObject.optBoolean(name);
		}
		return fallback;
	}

	@NonNull
	public static JSONObject toJSONObject(@Nullable Map<String, String> map) {
		final JSONObject jsonObject = new JSONObject();
		if (map != null) {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				try {
					jsonObject.put(entry.getKey(), entry.getValue());
				} catch (Exception e) {
					MTLog.w(LOG_TAG, e, "Error while adding entry to JSON object '%s' > '%s'", entry.getKey(), entry.getValue());
				}
			}
		}
		return jsonObject;
	}

	@NonNull
	public static Map<String, String> toMapOfStrings(@NonNull JSONObject jsonObject) {
		final Map<String, String> map = new HashMap<>();
		try {
			if (jsonObject.length() > 0) {
				final Iterator<String> keys = jsonObject.keys();
				while (keys.hasNext()) {
					final String key = keys.next();
					map.put(key, jsonObject.getString(key));
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while parsing JSON object '%s'", jsonObject);
		}
		return map;
	}

	private JSONUtils() {
		// utility class
	}
}
