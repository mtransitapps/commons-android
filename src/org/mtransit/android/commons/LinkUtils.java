package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public final class LinkUtils implements MTLog.Loggable {

	private static final String TAG = LinkUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String NO_LABEL = null;

	public static boolean open(@NonNull Activity activity, Uri uri, String label, int... intentFlags) {
		if (uri == null) {
			return false;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		if (intentFlags != null) {
			for (int intentFlag : intentFlags) {
				intent.addFlags(intentFlag);
			}
		}
		return open(activity, intent, label);
	}

	public static boolean open(@NonNull Activity activity, Intent intent, String label) {
		if (intent == null) {
			return false;
		}
		if (intent.resolveActivity(activity.getPackageManager()) == null) {
			ToastUtils.makeTextAndShowCentered(activity, activity.getString(R.string.opening_failed_and_uri, intent.getData()));
			return false;
		}
		activity.startActivity(intent);
		if (label == null) { // no toast
		} else if (TextUtils.isEmpty(label)) { // unknown app
			ToastUtils.makeTextAndShowCentered(activity, R.string.opening_unknown);
		} else { // known app
			ToastUtils.makeTextAndShowCentered(activity, activity.getString(R.string.opening_and_label, label));
		}
		return true;
	}
}
