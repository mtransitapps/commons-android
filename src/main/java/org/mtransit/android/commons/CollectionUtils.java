package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CollectionUtils implements MTLog.Loggable {

	private static final String LOG_TAG = CollectionUtils.class.getSimpleName();

	@NotNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private CollectionUtils() {
	}

	public static int getSize(final @Nullable Collection<?> collection) {
		if (collection == null) {
			return 0;
		}
		return collection.size();
	}

	public static int getSize(final @Nullable Map<?, ?> map) {
		if (map == null) {
			return 0;
		}
		return map.size();
	}

	public static <T> void sort(@Nullable java.util.List<T> list, @NonNull Comparator<? super T> comparator) {
		if (list == null || list.size() < 2) {
			return; // nothing to sort if null / empty / 1 element
		}
		Collections.sort(list, comparator);
	}

	@Nullable
	public static <T> T getOrNull(@Nullable List<T> list, int i) {
		try {
			if (list == null) {
				return null;
			}
			if (i < list.size()) {
				return list.get(i);
			} else {
				return null;
			}
		} catch (Exception e) {
			MTLog.d(LOG_TAG, e, "Error while reading object '%d' from '%s'!", i, list);
			return null;
		}
	}
}
