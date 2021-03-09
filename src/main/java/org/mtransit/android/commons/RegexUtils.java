package org.mtransit.android.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;

@SuppressWarnings("unused")
public final class RegexUtils implements MTLog.Loggable {

	private static final String LOG_TAG = RegexUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static String extractMatcherGroup(@NonNull Matcher matcher, int group) {
		if (matcher.find()) {
			if (matcher.groupCount() > group) {
				return matcher.group(group);
			}
		}
		return null;
	}
}
