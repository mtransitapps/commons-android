package org.mtransit.android.commons;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

@SuppressWarnings("unused")
public final class RegexUtils implements MTLog.Loggable {

	private static final String LOG_TAG = RegexUtils.class.getSimpleName();

	@NotNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static String extractMatcherGroup(@NotNull Matcher matcher, int group) {
		if (matcher.find()) {
			if (matcher.groupCount() > group) {
				return matcher.group(group);
			}
		}
		return null;
	}
}
