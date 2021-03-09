package org.mtransit.android.commons;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ResourceUtils implements MTLog.Loggable {

	private static final String LOG_TAG = ResourceUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static float getDimension(@Nullable Context context, int unit, int value) {
		if (context == null) {
			return value;
		}
		return TypedValue.applyDimension(unit, value, context.getResources().getDisplayMetrics());
	}

	public static float convertSPtoPX(@Nullable Context context, int sp) {
		return getDimension(context, TypedValue.COMPLEX_UNIT_SP, sp);
	}

	public static float convertDPtoPX(@Nullable Context context, int sp) {
		return getDimension(context, TypedValue.COMPLEX_UNIT_DIP, sp);
	}

	public static int convertPXtoDP(@NonNull Context context, int px) {
		return (int) (px / context.getResources().getDisplayMetrics().density);
	}

	@NonNull
	public static List<Pattern> getRegexPatternArray(@NonNull Context context, @ArrayRes int id) {
		return getRegexPatternArray(context, id, 0);
	}

	@NonNull
	public static List<Pattern> getRegexPatternArray(@NonNull Context context, @ArrayRes int id, int flags) {
		List<Pattern> regexList = new ArrayList<>();
		final java.util.List<String> regexStrings = Arrays.asList(context.getResources().getStringArray(id));
		for (int c = 0; c < regexStrings.size(); c++) {
			final String regex = regexStrings.get(c);
			Pattern newPattern = null;
			try {
				newPattern = Pattern.compile(regex, flags);
			} catch (Exception e) {
				MTLog.w(LOG_TAG, e, "Error while compiling pattern '%s' for %d configuration!", regex, c);
			}
			regexList.add(newPattern);
		}
		return regexList;
	}
}
