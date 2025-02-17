package org.mtransit.android.commons.di

import android.app.Application
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import org.mtransit.android.commons.MTLog

object TimeProvider : MTLog.Loggable {

    private val LOG_TAG: String = TimeProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private var trustedTimeClient: TrustedTimeClient? = null

    fun init(app: Application) {
        TrustedTime.createClient(app)
            .addOnSuccessListener { client ->
                trustedTimeClient = client
            }
            .addOnFailureListener { exception ->
                MTLog.w(LOG_TAG, exception, "Error while initializing TrustedTimeClient!")
            }
    }

    @JvmStatic
    fun currentTimeMillis(): Long {
        return trustedTimeClient?.computeCurrentUnixEpochMillis() ?: System.currentTimeMillis()
    }
}
