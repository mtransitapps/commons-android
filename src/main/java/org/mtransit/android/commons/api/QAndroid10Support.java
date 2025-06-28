package org.mtransit.android.commons.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@SuppressWarnings("unused")
@RequiresApi(Build.VERSION_CODES.Q)
public class QAndroid10Support extends PieSupport {

	private static final String LOG_TAG = QAndroid10Support.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}
}
