package org.mtransit.android.commons.api;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;

import java.util.Locale;

public interface SupportUtil extends MTLog.Loggable {

	void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener);

	void setBackground(@NonNull View view, @Nullable Drawable background);

	@NonNull
	Locale localeForLanguageTag(@NonNull String languageTag);

	boolean isCharacterAlphabetic(int codePoint);

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	<T> T requireNonNull(@Nullable T obj, @NonNull String message);

	@Nullable
	Display getDefaultDisplay(@NonNull Activity activity);

	boolean equals(@Nullable Object a, @Nullable Object b);
}
