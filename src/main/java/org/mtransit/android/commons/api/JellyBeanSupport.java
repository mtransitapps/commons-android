package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;

import java.util.Locale;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class JellyBeanSupport implements SupportUtil {

	private static final String LOG_TAG = JellyBeanSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public JellyBeanSupport() {
		super();
	}

	@Override
	public void removeOnGlobalLayoutListener(@NonNull ViewTreeObserver viewTreeObserver, @NonNull ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener);
	}

	@Override
	public void setBackground(@NonNull View view, @Nullable Drawable background) {
		view.setBackground(background);
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

	@Nullable
	@Override
	public Display getDefaultDisplay(@NonNull Activity activity) {
		WindowManager windowManager = activity.getWindowManager();
		return windowManager == null ? null : windowManager.getDefaultDisplay();
	}

	@Override
	public boolean equals(@Nullable Object a, @Nullable Object b) {
		return (a == b) || (a != null && a.equals(b));
	}
}
