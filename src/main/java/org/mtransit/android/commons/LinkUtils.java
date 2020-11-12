package org.mtransit.android.commons;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LinkUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LinkUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static final String NO_LABEL = null;

	public static boolean open(@NonNull Context context, @Nullable Uri uri, @Nullable String label, @Nullable int... intentFlags) {
		if (uri == null) {
			return false;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		if (intentFlags != null) {
			for (int intentFlag : intentFlags) {
				intent.addFlags(intentFlag);
			}
		}
		return open(context, intent, label);
	}

	public static boolean open(@NonNull Context context, @Nullable Intent intent, @Nullable String label) {
		if (intent == null) {
			return false;
		}
		if (intent.resolveActivity(context.getPackageManager()) == null) {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.opening_failed_and_uri, intent.getData()));
			return false;
		}
		context.startActivity(intent);
		if (label == null) { // no toast
		} else if (TextUtils.isEmpty(label)) { // unknown app
			ToastUtils.makeTextAndShowCentered(context, R.string.opening_unknown);
		} else { // known app
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.opening_and_label, label));
		}
		return true;
	}
}
