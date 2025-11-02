package org.mtransit.android.commons.api;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
@SuppressLint("ObsoleteSdkInt") // Always >= 24 (minSDK)
@RequiresApi(Build.VERSION_CODES.N)
public class NougatSupport implements SupportUtil {

	private static final String LOG_TAG = NougatSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public boolean isCharacterAlphabetic(int codePoint) {
		return Character.isAlphabetic(codePoint);
	}

	@NonNull
	@Override
	public <T> T requireNonNull(@Nullable T obj, @NonNull String message) {
		return Objects.requireNonNull(obj, message);
	}

	@Override
	public boolean equals(@Nullable Object a, @Nullable Object b) {
		return Objects.equals(a, b);
	}

	@Override
	public void setBackground(@NonNull View view, @Nullable Drawable background) {
		view.setBackground(background);
	}

	@Nullable
	@Override
	public Display getDefaultDisplay(@NonNull Activity activity) {
		final WindowManager windowManager = activity.getWindowManager();
		return windowManager == null ? null : windowManager.getDefaultDisplay();
	}
}
