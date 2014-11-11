package org.mtransit.android.commons;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

public final class NotificationUtils {

	public static void setProgressAndNotify(NotificationManager nm, NotificationCompat.Builder nb, int nId, int max, int progress) {
		nb.setProgress(max, progress, false);
		nm.notify(nId, nb.build());
	}

}
