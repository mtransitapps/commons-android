package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@TargetApi(Build.VERSION_CODES.N)
public class NougatSupport extends MarshmallowSupport {

	private static final String LOG_TAG = NougatSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public NougatSupport() {
		super();
	}

	@NonNull
	@Override
	public <K, V> V getOrDefault(@NonNull ConcurrentHashMap<K, V> map, @NonNull K key, @NonNull V defaultValue) {
		final V value = map.getOrDefault(key, defaultValue);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
}
