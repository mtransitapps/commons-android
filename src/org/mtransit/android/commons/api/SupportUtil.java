package org.mtransit.android.commons.api;

import java.util.Locale;

import org.mtransit.android.commons.MTLog;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

@SuppressWarnings("DeprecatedIsStillUsed")
public interface SupportUtil extends MTLog.Loggable {

	void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener);

	void setBackground(@NonNull View view, @Nullable Drawable background);

	@NonNull
	Locale localeForLanguageTag(@NonNull String languageTag);

	/**
	 * @deprecated use {@link android.support.v4.content.res.ResourcesCompat#getDrawable(Resources, int, Resources.Theme)}
	 */
	@Deprecated
	Drawable getResourcesDrawable(@NonNull Resources resources, @DrawableRes int id, @Nullable Resources.Theme theme);

	/**
	 * @deprecated use {@link android.support.v4.content.res.ResourcesCompat#getColor(Resources, int, Resources.Theme)}
	 */
	@Deprecated
	@ColorInt
	int getColor(@NonNull Resources resources, @ColorRes int id, @Nullable Resources.Theme theme);

	boolean isCharacterAlphabetic(int codePoint);

	@NonNull
	<T> T requireNonNull(@Nullable T obj, @NonNull String message);
}
