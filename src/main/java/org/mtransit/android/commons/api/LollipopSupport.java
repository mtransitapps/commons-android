package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Objects;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopSupport implements SupportUtil {

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
	public void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener);
	}

	@Override
	public void setBackground(@NonNull View view, @Nullable Drawable background) {
		view.setBackground(background);
	}

	@Nullable
	@Override
	public Display getDefaultDisplay(@NonNull Activity activity) {
		WindowManager windowManager = activity.getWindowManager();
		return windowManager == null ? null : windowManager.getDefaultDisplay();
	}
}
