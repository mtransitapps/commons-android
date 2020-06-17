package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.N)
public class NougatSupport extends MarshmallowSupport {

	private static final String LOG_TAG = NougatSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public NougatSupport() {
		super();
	}

	@NonNull
	@Override
	public Spanned fromHtml(@NonNull String source) {
		return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
	}
}
