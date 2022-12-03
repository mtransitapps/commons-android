package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Predicate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Deprecated
@SuppressWarnings({"UnusedReturnValue", "unused"})
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
		return org.mtransit.commons.CollectionUtils.getSize(collection);
	}

	public static int getSize(final @Nullable Map<?, ?> map) {
		return org.mtransit.commons.CollectionUtils.getSize(map);
	}

	public static <T> void sort(@Nullable java.util.List<T> list, @NonNull Comparator<? super T> comparator) {
		org.mtransit.commons.CollectionUtils.sort(list, comparator);
	}

	@Nullable
	public static <T> T getOrNull(@Nullable List<T> list, int index) {
		return org.mtransit.commons.CollectionUtils.getOrNull(list, index);
	}

	@NonNull
	public static <T> List<T> removeDuplicates(@NonNull List<T> list) {
		return org.mtransit.commons.CollectionUtils.removeDuplicatesNN(list);
	}

	public static <T> int removeIf(@NonNull List<T> list, @NonNull Predicate<? super T> filter) {
		return org.mtransit.commons.CollectionUtils.removeIfNN(list, filter::test);
	}

	private static <T> boolean equalsList(List<T> l1, List<T> l2) {
		return org.mtransit.commons.CollectionUtils.equalsList(l1, l2);
	}
}
