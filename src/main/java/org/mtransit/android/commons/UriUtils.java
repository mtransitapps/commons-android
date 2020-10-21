package org.mtransit.android.commons;

import android.net.Uri;
import androidx.annotation.NonNull;

public final class UriUtils {

	@NonNull
	public static Uri newContentUri(@NonNull String authority) {
		return Uri.parse("content://" + authority + "/");
	}
}
