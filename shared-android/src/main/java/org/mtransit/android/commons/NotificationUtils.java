package org.mtransit.android.commons;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class NotificationUtils {

	public static final String CHANNEL_ID_DB = "db_channel";

	@SuppressLint("MissingPermission")
	@RequiresPermission(
			"android.permission.POST_NOTIFICATIONS"
	)
	public static void setProgressAndNotify(@NonNull NotificationManagerCompat nm, @NonNull NotificationCompat.Builder nb, int nId, int max, int progress) {
		nb.setProgress(max, progress, false);
		nm.notify(nId, nb.build());
	}

	public static void createNotificationChannel(@NonNull Context context, @NonNull String channelId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManagerCompat.from(context).createNotificationChannel(
					new NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
							.setName(context.getString(R.string.db_channel_name))
							.setDescription(context.getString(R.string.db_channel_description))
							.build()
			);
		}
	}
}
