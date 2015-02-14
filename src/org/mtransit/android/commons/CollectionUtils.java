package org.mtransit.android.commons;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public final class CollectionUtils {

	private CollectionUtils() {
	}

	public static int getSize(Collection<?> collection) {
		if (collection == null) {
			return 0;
		}
		return collection.size();
	}

	public static int getSize(Map<?, ?> map) {
		if (map == null) {
			return 0;
		}
		return map.size();
	}

	public static <T> void sort(java.util.List<T> list, Comparator<? super T> comparator) {
		if (list == null || list.size() < 2) {
			return; // nothing to sort if null / empty / 1 element
		}
		Collections.sort(list, comparator);
	}
}
