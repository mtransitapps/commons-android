package org.mtransit.android.commons;

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public final class CollectionUtils {

	private CollectionUtils() {
	}

	public static int getSize(@Nullable final Collection<?> collection) {
		if (collection == null) {
			return 0;
		}
		return collection.size();
	}

	public static int getSize(@Nullable final Map<?, ?> map) {
		if (map == null) {
			return 0;
		}
		return map.size();
	}

	public static <T> void sort(@Nullable java.util.List<T> list, @Nullable Comparator<? super T> comparator) {
		if (comparator == null) {
			return; // no comparator to do the sort
		}
		if (list == null || list.size() < 2) {
			return; // nothing to sort if null / empty / 1 element
		}
		Collections.sort(list, comparator);
	}
}
