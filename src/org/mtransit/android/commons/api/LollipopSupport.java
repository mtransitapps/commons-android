package org.mtransit.android.commons.api;

import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopSupport extends KitKatSupport {

	private static final String LOG_TAG = LollipopSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public LollipopSupport() {
		super();
	}

	@NonNull
	@Override
	public Locale localeForLanguageTag(@NonNull String languageTag) {
		return Locale.forLanguageTag(languageTag);
	}
}
