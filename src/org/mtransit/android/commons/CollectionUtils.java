package org.mtransit.android.commons;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CollectionUtils {

	private CollectionUtils() {
	}

	public static int getSize(Collection<?> collection) {
		if (collection == null) {
			return 0;
		}
		return collection.size();
	}

	public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
		if (list == null || list.size() < 2) {
			return; // nothing to sort if null / empty / 1 element
		}
		Collections.sort(list, comparator);
	}
}
