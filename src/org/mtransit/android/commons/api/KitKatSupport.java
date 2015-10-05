package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class KitKatSupport extends JellyBeanSupportMR2 {

	private static final String TAG = KitKatSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public KitKatSupport() {
	}

	@Override
	public boolean isCharacterAlphabetic(int codePoint) {
		return Character.isAlphabetic(codePoint);
	}
}
