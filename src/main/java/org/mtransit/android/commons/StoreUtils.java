package org.mtransit.android.commons;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public final class StoreUtils implements MTLog.Loggable {

	private static final String LOG_TAG = StoreUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String HTTP_SCHEME = "http";
	private static final String HTTPS_SCHEME = "https";
	private static final String MARKET_SCHEME = "market";
	private static final String ANDROID_MARKET_WWW_AUTHORITY = "market.android.com"; // old
	private static final String GOOGLE_PLAY_STORE_WWW_AUTHORITY = "play.google.com";
	private static final String GOOGLE_PLAY_STORE_BASE_URI_AND_PKG = MARKET_SCHEME + "://details?id=%s";
	private static final String GOOGLE_PLAY_STORE_BASE_WWW_URI_AND_PKG = HTTPS_SCHEME + "://play.google.com/store/apps/details?id=%s";
	private static final String GOOGLE_PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";

	public static void viewAppPage(@NonNull Activity activity, @NonNull String pkg, @NonNull String label) {
		int[] flags = new int[]{ //
				Intent.FLAG_ACTIVITY_NEW_TASK, // make sure it does NOT open in the stack of your activity
				Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED, // task re-parenting if needed
				Intent.FLAG_ACTIVITY_CLEAR_TOP, // make sure it opens on app page even if already open in search result
		};
		boolean success = LinkUtils.open(activity, Uri.parse(String.format(GOOGLE_PLAY_STORE_BASE_URI_AND_PKG, pkg)), label, flags);
		if (!success) {
			LinkUtils.open(activity, Uri.parse(String.format(GOOGLE_PLAY_STORE_BASE_WWW_URI_AND_PKG, pkg)), label, flags);
		}
	}

	public static void viewSubscriptionPage(@NonNull Activity activity, @NonNull String sku, @NonNull String pkg, @Nullable String label) {
		LinkUtils.open(activity, Uri.parse(String.format(GOOGLE_PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL, sku, pkg)), label);
	}

	public static boolean isStoreIntent(@NonNull String url) {
		return isStoreIntent(Uri.parse(url));
	}

	public static boolean isStoreIntent(@NonNull Uri uri) {
		if (MARKET_SCHEME.equals(uri.getScheme())) {
			return true;
		} else if (HTTPS_SCHEME.equals(uri.getScheme())
				|| HTTP_SCHEME.equals(uri.getScheme())) {
			//noinspection RedundantIfStatement
			if (GOOGLE_PLAY_STORE_WWW_AUTHORITY.equals(uri.getAuthority())
					|| ANDROID_MARKET_WWW_AUTHORITY.equals(uri.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}
