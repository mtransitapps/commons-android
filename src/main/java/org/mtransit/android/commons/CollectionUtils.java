package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public final class CollectionUtils implements MTLog.Loggable {

	private static final String LOG_TAG = CollectionUtils.class.getSimpleName();

	@NonNull
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

	@NonNull
	public static <T> List<T> removeDuplicates(@NonNull List<T> list) {
		return new ArrayList<>(new LinkedHashSet<>(list));
	}

	public static <T> int removeIf(@NonNull List<T> list, @NonNull Predicate<? super T> filter) {
		int removed = 0;
		final Iterator<T> each = list.listIterator();
		while (each.hasNext()) {
			if (filter.test(each.next())) {
				each.remove();
				removed++;
			}
		}
		return removed;
	}
}
