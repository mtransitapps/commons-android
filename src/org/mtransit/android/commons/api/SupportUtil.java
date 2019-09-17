package org.mtransit.android.commons.api;

import java.util.Locale;

import org.mtransit.android.commons.MTLog;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

@SuppressWarnings("DeprecatedIsStillUsed")
public interface SupportUtil extends MTLog.Loggable {

	void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener);

	void setBackground(@NonNull View view, @Nullable Drawable background);

	@NonNull
	Locale localeForLanguageTag(@NonNull String languageTag);

	/**
	 * @deprecated use {@link androidx.core.content.res.ResourcesCompat#getDrawable(Resources, int, Resources.Theme)}
	 */
	@Nullable
	@Deprecated
	Drawable getResourcesDrawable(@NonNull Resources resources, @DrawableRes int id, @Nullable Resources.Theme theme);

	/**
	 * @deprecated use {@link androidx.core.content.res.ResourcesCompat#getColor(Resources, int, Resources.Theme)}
	 */
	@Deprecated
	@ColorInt
	int getColor(@NonNull Resources resources, @ColorRes int id, @Nullable Resources.Theme theme);

	boolean isCharacterAlphabetic(int codePoint);

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	<T> T requireNonNull(@Nullable T obj, @NonNull String message);
}
