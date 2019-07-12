package org.mtransit.android.commons;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.SparseArray;

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

	@NonNull
	public static <C> ArrayList<C> asArrayList(@Nullable SparseArray<C> sparseArray) {
		ArrayList<C> arrayList = new ArrayList<C>(sparseArray == null ? 0 : sparseArray.size());
		if (sparseArray != null) {
			for (int i = 0; i < sparseArray.size(); i++) {
				arrayList.add(sparseArray.valueAt(i));
			}
		}
		return arrayList;
	}

	@SuppressWarnings("unchecked")
	public static <T> int getSize(T... array) {
		if (array == null) {
			return 0;
		}
		return array.length;
	}

	@NonNull
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> asArrayList(@Nullable T... array) {
		ArrayList<T> result = new ArrayList<T>();
		if (array != null) {
			Collections.addAll(result, array);
		}
		return result;
	}

	public static String[] addAll(String[] array1, String[] array2) {
		if (array1 == null) {
			return clone(array2);
		} else if (array2 == null) {
			return clone(array1);
		}
		String[] joinedArray = (String[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}

	public static String[] clone(Object[] array) {
		if (array == null) {
			return null;
		}
		return (String[]) array.clone();
	}

	public static List<Integer> asIntegerList(int[] intArray) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (intArray != null) {
			for (int integer : intArray) {
				result.add(integer);
			}
		}
		return result;
	}

	public static ArrayList<Integer> asIntegerList(String[] stringArray) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (stringArray != null) {
			for (String string : stringArray) {
				result.add(Integer.valueOf(string));
			}
		}
		return result;
	}

	public static ArrayList<Long> asLongList(String[] stringArray) {
		ArrayList<Long> result = new ArrayList<Long>();
		if (stringArray != null) {
			for (String string : stringArray) {
				result.add(Long.valueOf(string));
			}
		}
		return result;
	}

	public static ArrayList<Boolean> asBooleanList(String[] stringArray) {
		ArrayList<Boolean> result = new ArrayList<Boolean>();
		if (stringArray != null) {
			for (String string : stringArray) {
				result.add(Boolean.valueOf(string));
			}
		}
		return result;
	}
}
