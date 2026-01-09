package org.mtransit.android.commons.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.mtransit.android.commons.MTLog

class GenericReceiver : BroadcastReceiver(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = GenericReceiver::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    override fun onReceive(context: Context?, intent: Intent?) {
        // DO NOTHING
    }
}
