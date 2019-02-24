package org.mtransit.android.commons;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.support.annotation.NonNull;

public final class FileUtils implements MTLog.Loggable {

	private static final String TAG = FileUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String UTF_8 = "UTF-8";
	public static final String ISO_8859_1 = "ISO-8859-1";

	@NonNull
	public static String fromFileRes(@NonNull Context context, int fileResId) {
		StringBuilder resultSb = new StringBuilder();
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(context.getResources().openRawResource(fileResId), UTF_8);
			br = new BufferedReader(isr, 8192);
			String line;
			while ((line = br.readLine()) != null) {
				resultSb.append(line);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reading resource file ID '%s'!", fileResId);
		} finally {
			closeQuietly(br);
			closeQuietly(isr);
		}
		return resultSb.toString();
	}

	public static void copyToPrivateFile(Context context, String fileName, @NonNull String string) {
		copyToPrivateFile(context, fileName, new ByteArrayInputStream(string.getBytes()));
	}

	public static void copyToPrivateFile(Context context, String fileName, InputStream inputStream) {
		copyToPrivateFile(context, fileName, inputStream, UTF_8);
	}

	public static void copyToPrivateFile(Context context, String fileName, InputStream inputStream, String charsetName) {
		FileOutputStream outputStream = null;
		BufferedReader br = null;
		try {
			outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			br = new BufferedReader(new InputStreamReader(inputStream, charsetName), 8192);
			String line;
			while ((line = br.readLine()) != null) {
				outputStream.write(line.getBytes());
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while copying to file '%s'!", fileName);
		} finally {
			closeQuietly(outputStream);
			closeQuietly(br);
		}
	}

	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			MTLog.d(TAG, e, "Error while closing '%s'!", closeable);
		}
	}

	@NonNull
	public static String getString(InputStream inputStream) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8), 8192);
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reading json!");
		} finally {
			closeQuietly(reader);
		}
		return sb.toString();
	}
}
