package org.mtransit.android.commons;

import android.net.Uri;

public class UriUtils {

	public static Uri newContentUri(String authority) {
		return Uri.parse("content://" + authority + "/");
	}

}
