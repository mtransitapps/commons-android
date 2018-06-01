package org.mtransit.android.commons;

import java.util.regex.Matcher;

import android.support.annotation.Nullable;

public final class RegexUtils implements MTLog.Loggable {

	private static final String LOG_TAG = RegexUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static String extractMatcherGroup(Matcher matcher, int group) {
		if (matcher.find()) {
			if (matcher.groupCount() > group) {
				return matcher.group(group);
			}
		}
		return null;
	}
}
