package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public final class StoreUtils {

	private static final String GOOGLE_PLAY_BASE_URI_AND_PKG = "market://details?id=%s";

	public static void viewAppPage(Activity activity, String pkg) {
		Uri uri = Uri.parse(String.format(GOOGLE_PLAY_BASE_URI_AND_PKG, pkg));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		activity.startActivity(intent);
	}

	private static final String GOOGLE_PLAY_WEB_TESTING_AND_PKG = "https://play.google.com/apps/testing/%s";

	public static void viewTestingWebPage(Activity activity, String pkg) {
		Uri uri = Uri.parse(String.format(GOOGLE_PLAY_WEB_TESTING_AND_PKG, pkg));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		activity.startActivity(intent);
	}

}
