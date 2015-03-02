package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public final class LinkUtils implements MTLog.Loggable {

	private static final String TAG = LinkUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String NO_LABEL = null;

	public static void open(Activity activity, Uri uri, String label, boolean www) {
		if (uri == null) {
			return;
		}
		open(activity, new Intent(Intent.ACTION_VIEW, uri), label, www);
	}

	public static void open(Activity activity, Intent intent, String label, boolean www) {
		if (intent == null) {
			return;
		}
		if (intent.resolveActivity(activity.getPackageManager()) == null) {
			ToastUtils.makeTextAndShowCentered(activity, activity.getString(R.string.opening_failed_and_uri, intent.getData()));
			return;
		}
		activity.startActivity(intent);
		if (label == null) { // no toast
		} else if (TextUtils.isEmpty(label)) { // unknown app
			ToastUtils.makeTextAndShowCentered(activity, R.string.opening_unknown);
		} else { // known app
			ToastUtils.makeTextAndShowCentered(activity, activity.getString(R.string.opening_and_label, label));
		}
	}
}
