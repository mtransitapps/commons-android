package org.mtransit.android.commons.api;

import java.util.Locale;

import org.mtransit.android.commons.MTLog;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class IceCreamSandwichSupport implements SupportUtil {

	private static final String LOG_TAG = IceCreamSandwichSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public IceCreamSandwichSupport() {
		super();
	}

	@Override
	public void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeGlobalOnLayoutListener(onGlobalLayoutListener);
	}

	@Override
	public void setBackground(@NonNull View view, @Nullable Drawable background) {
		view.setBackgroundDrawable(background);
	}

	private static final String LANG_SPLIT = "-";

	@NonNull
	@Override
	public Locale localeForLanguageTag(@NonNull String languageTag) {
		try {
			if (!TextUtils.isEmpty(languageTag)) {
				String[] split = languageTag.split(LANG_SPLIT);
				if (split.length == 1) {
					return new Locale(split[0]);
				} else if (split.length == 2) {
					return new Locale(split[0], split[1]);
				} else if (split.length == 3) {
					return new Locale(split[0], split[1], split[2]);
				}
			}
			MTLog.w(this, "Unexpected language tag '%s'!", languageTag);
			return Locale.ENGLISH; // default
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing locale language tag '%s'!", languageTag);
			return Locale.ENGLISH; // default
		}
	}

	@Nullable
	@Override
	public Drawable getResourcesDrawable(@NonNull Resources resources, @DrawableRes int id, @Nullable Resources.Theme theme) {
		return ResourcesCompat.getDrawable(resources, id, theme);
	}

	@ColorInt
	@Override
	public int getColor(@NonNull Resources resources, @ColorRes int id, @Nullable Resources.Theme theme) {
		return ResourcesCompat.getColor(resources, id, theme);
	}

	@Override
	public boolean isCharacterAlphabetic(int codePoint) {
		return Character.isLetter(codePoint); // almost the same
	}

	@NonNull
	@Override
	public <T> T requireNonNull(@Nullable T obj, @NonNull String message) {
		if (obj == null) {
			throw new NullPointerException(message);
		}
		return obj;
	}
}
