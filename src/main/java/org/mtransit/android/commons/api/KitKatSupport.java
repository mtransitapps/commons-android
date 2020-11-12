package org.mtransit.android.commons.api;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class KitKatSupport extends JellyBeanSupportMR2 {

	private static final String LOG_TAG = KitKatSupport.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("WeakerAccess")
	public KitKatSupport() {
		super();
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
}
