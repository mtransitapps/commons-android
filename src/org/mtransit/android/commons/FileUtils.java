package org.mtransit.android.commons;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;

public final class FileUtils implements MTLog.Loggable {

	private static final String TAG = FileUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static String fromFileRes(Context context, int fileResId) {
		StringBuilder resultSb = new StringBuilder();
		try {
			InputStreamReader isr = new InputStreamReader(context.getResources().openRawResource(fileResId), "UTF8");
			BufferedReader br = new BufferedReader(isr, 8192);
			String line;
			while ((line = br.readLine()) != null) {
				resultSb.append(line);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reading resource file ID '%s'!", fileResId);
		}
		return resultSb.toString();
	}

	public static void copyToPrivateFile(Context context, String fileName, InputStream inputStream) {
		FileOutputStream outputStream;
		BufferedReader br = null;
		try {
			outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			br = new BufferedReader(new InputStreamReader(inputStream, "UTF8"), 8192);
			String line;
			while ((line = br.readLine()) != null) {
				outputStream.write(line.getBytes());
			}
			outputStream.close();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while copying to file '%s'!", fileName);
		}
	}

}
