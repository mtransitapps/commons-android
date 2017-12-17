package org.mtransit.android.commons.api;

import java.util.Locale;

import org.mtransit.android.commons.MTLog;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.view.View;
import android.view.ViewTreeObserver;

public interface SupportUtil extends MTLog.Loggable {

	void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener);

	void setBackground(View view, Drawable background);

	Locale localeForLanguageTag(String languageTag);

	Drawable getResourcesDrawable(Resources resources, int id, Resources.Theme theme);

	@ColorInt
	int getColor(Resources resources, int id, Resources.Theme theme);

	boolean isCharacterAlphabetic(int codePoint);
}
