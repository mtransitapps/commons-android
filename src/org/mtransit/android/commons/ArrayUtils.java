package org.mtransit.android.commons;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import android.util.SparseArray;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ArrayUtils {

	public static int getSize(@Nullable SparseArray<?> sparseArray) {
		if (sparseArray == null) {
			return 0;
		}
		return sparseArray.size();
	}

	public static boolean containsKey(@Nullable SparseArray<?> sparseArray, int key) {
		return sparseArray != null && sparseArray.indexOfKey(key) >= 0;
	}

	public static boolean containsKey(@Nullable SparseArrayCompat<?> sparseArray, int key) {
		return sparseArray != null && sparseArray.indexOfKey(key) >= 0;
	}

	@NonNull
	public static <C> ArrayList<C> asArrayList(@Nullable SparseArray<C> sparseArray) {
		ArrayList<C> arrayList = new ArrayList<>(sparseArray == null ? 0 : sparseArray.size());
		if (sparseArray != null) {
			for (int i = 0; i < sparseArray.size(); i++) {
				arrayList.add(sparseArray.valueAt(i));
			}
		}
		return arrayList;
	}

	@NonNull
	public static <C> ArrayList<C> asArrayList(@Nullable SparseArrayCompat<C> sparseArray) {
		ArrayList<C> arrayList = new ArrayList<>(sparseArray == null ? 0 : sparseArray.size());
		if (sparseArray != null) {
			for (int i = 0; i < sparseArray.size(); i++) {
				arrayList.add(sparseArray.valueAt(i));
			}
		}
		return arrayList;
	}

	@SuppressWarnings("unchecked")
	public static <T> int getSize(@Nullable T... array) {
		if (array == null) {
			return 0;
		}
		return array.length;
	}

	@NonNull
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> asArrayList(@Nullable T... array) {
		ArrayList<T> result = new ArrayList<>();
		if (array != null) {
			Collections.addAll(result, array);
		}
		return result;
	}

	@Nullable
	public static String[] addAll(@Nullable String[] array1, @Nullable String[] array2) {
		if (array1 == null) {
			return clone(array2);
		} else if (array2 == null) {
			return clone(array1);
		}
		return addAllNonNull(array1, array2);
	}

	@NonNull
	public static String[] addAllNonNull(@NonNull String[] array1, @NonNull String[] array2) {
		String[] joinedArray = (String[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}

	@Nullable
	public static String[] clone(@Nullable Object[] array) {
		if (array == null) {
			return null;
		}
		return (String[]) array.clone();
	}

	@NonNull
	public static List<Integer> asIntegerList(@Nullable int[] intArray) {
		ArrayList<Integer> result = new ArrayList<>();
		if (intArray != null) {
			for (int integer : intArray) {
				result.add(integer);
			}
		}
		return result;
	}

	@NonNull
	public static ArrayList<Integer> asIntegerList(@Nullable String[] stringArray) {
		ArrayList<Integer> result = new ArrayList<>();
		if (stringArray != null) {
			for (String string : stringArray) {
				result.add(Integer.valueOf(string));
			}
		}
		return result;
	}

	@NonNull
	public static ArrayList<Long> asLongList(@Nullable String[] stringArray) {
		ArrayList<Long> result = new ArrayList<>();
		if (stringArray != null) {
			for (String string : stringArray) {
				result.add(Long.valueOf(string));
			}
		}
		return result;
	}

	@NonNull
	public static ArrayList<Boolean> asBooleanList(@Nullable String[] stringArray) {
		ArrayList<Boolean> result = new ArrayList<>();
		if (stringArray != null) {
			for (String string : stringArray) {
				result.add(Boolean.valueOf(string));
			}
		}
		return result;
	}
}
