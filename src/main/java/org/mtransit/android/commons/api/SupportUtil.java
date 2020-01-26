package org.mtransit.android.commons.api;

import java.util.Locale;

import org.mtransit.android.commons.MTLog;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

public interface SupportUtil extends MTLog.Loggable {

	void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener);

	void setBackground(@NonNull View view, @Nullable Drawable background);

	@NonNull
	Locale localeForLanguageTag(@NonNull String languageTag);

	boolean isCharacterAlphabetic(int codePoint);

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	<T> T requireNonNull(@Nullable T obj, @NonNull String message);
}
