package org.mtransit.android.commons;

import java.lang.reflect.Array;

import android.util.SparseArray;

public class ArrayUtils {

	public static int getSize(SparseArray<?> sparseArray) {
		if (sparseArray == null) {
			return 0;
		}
		return sparseArray.size();
	}

	@SuppressWarnings("unchecked")
	public static <T> int getSize(T... array) {
		if (array == null) {
			return 0;
		}
		return array.length;
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

}
