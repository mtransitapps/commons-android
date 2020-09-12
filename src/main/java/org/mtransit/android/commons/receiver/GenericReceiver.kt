package org.mtransit.android.commons.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.mtransit.android.commons.MTLog

class GenericReceiver : BroadcastReceiver(), MTLog.Loggable {

    override fun getLogTag(): String {
        return GenericReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // DO NOTHING
    }
}