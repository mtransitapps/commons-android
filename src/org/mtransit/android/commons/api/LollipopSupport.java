package org.mtransit.android.commons.api;

import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopSupport extends KitKatSupport {

	private static final String TAG = LollipopSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public LollipopSupport() {
	}

	@Override
	public Locale localeForLanguageTag(String languageTag) {
		return Locale.forLanguageTag(languageTag);
	}

}
