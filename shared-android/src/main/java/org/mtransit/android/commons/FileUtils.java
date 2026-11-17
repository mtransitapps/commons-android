package org.mtransit.android.commons;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class FileUtils implements MTLog.Loggable {

	private static final String LOG_TAG = FileUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final String UTF_8 = "UTF-8";
	public static final String ISO_8859_1 = "ISO-8859-1";

	@NonNull
	public static Charset getUTF8() {
		return StandardCharsets.UTF_8;
	}

	@NonNull
	public static Charset getISO_8859_1() {
		return StandardCharsets.ISO_8859_1;
	}

	@NonNull
	public static String fromFileRes(@NonNull Context context, int fileResId) {
		StringBuilder resultSb = new StringBuilder();
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(context.getResources().openRawResource(fileResId), getUTF8());
			br = new BufferedReader(isr, 8192);
			String line;
			while ((line = br.readLine()) != null) {
				resultSb.append(line);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while reading resource file ID '%s'!", fileResId);
		} finally {
			closeQuietly(br);
			closeQuietly(isr);
		}
		return resultSb.toString();
	}

	public static void copyToPrivateFile(@NonNull Context context,
										 @NonNull String fileName,
										 @NonNull String string) {
		copyToPrivateFile(context, fileName, new ByteArrayInputStream(string.getBytes()));
	}

	public static void copyToPrivateFile(@NonNull Context context,
										 @NonNull String fileName,
										 @NonNull InputStream inputStream) {
		copyToPrivateFile(context, fileName, inputStream, getUTF8());
	}

	public static void copyToPrivateFile(@NonNull Context context,
										 @NonNull String fileName,
										 @NonNull InputStream inputStream,
										 @NonNull String charsetName) {
		copyToPrivateFile(context, fileName, inputStream, Charset.forName(charsetName));
	}

	public static void copyToPrivateFile(@NonNull Context context,
										 @NonNull String fileName,
										 @NonNull InputStream inputStream,
										 @NonNull Charset charset) {
		FileOutputStream outputStream = null;
		BufferedReader br = null;
		try {
			outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			br = new BufferedReader(new InputStreamReader(inputStream, charset), 8192);
			String line;
			while ((line = br.readLine()) != null) {
				outputStream.write(line.getBytes());
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while copying to file '%s'!", fileName);
		} finally {
			closeQuietly(outputStream);
			closeQuietly(br);
		}
	}

	public static void closeQuietly(@Nullable Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			MTLog.d(LOG_TAG, e, "Error while closing '%s'!", closeable);
		}
	}

	@NonNull
	public static String getString(@NonNull InputStream inputStream) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream, getUTF8()), 8192);
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while reading json!");
		} finally {
			closeQuietly(reader);
		}
		return sb.toString();
	}

	private static final Pattern IMG_URL = Pattern.compile("(^.*(\\.png|\\.jpg|\\.jpeg|\\.gif)$)", Pattern.CASE_INSENSITIVE);

	public static boolean isImageURL(@Nullable String url) {
		if (url == null) {
			return false;
		}
		return IMG_URL.matcher(url).matches();
	}
}
