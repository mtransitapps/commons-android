package org.mtransit.android.commons;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class NotificationUtils {

	public static final String CHANNEL_ID_DB = "db_channel";

	public static void setProgressAndNotify(@NonNull NotificationManagerCompat nm, @NonNull NotificationCompat.Builder nb, int nId, int max, int progress) {
		nb.setProgress(max, progress, false);
		nm.notify(nId, nb.build());
	}

	public static void createNotificationChannel(@NonNull Context context, @NonNull String channelId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getString(R.string.db_channel_name);
			String description = context.getString(R.string.db_channel_description);
			int importance = NotificationManager.IMPORTANCE_LOW;
			NotificationChannel channel = new NotificationChannel(channelId, name, importance);
			channel.setDescription(description);
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);
			}
		}
	}
}
