package org.mtransit.android.commons.di

import android.app.Application
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import org.mtransit.android.commons.MTLogK

object TimeProvider : MTLogK.Loggable {

    private val LOG_TAG: String = TimeProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private var trustedTimeClient: TrustedTimeClient? = null

    fun init(app: Application) = runCatching {
        TrustedTime.createClient(app)
            .addOnSuccessListener { client ->
                trustedTimeClient = client
            }
            .addOnFailureListener { exception ->
                MTLogK.w(LOG_TAG, "Error while initializing TrustedTimeClient!", exception)
            }
    }

    @JvmStatic
    fun currentTimeMillis() = runCatching {
        trustedTimeClient?.computeCurrentUnixEpochMillis()
    }.getOrNull() ?: currentSystemTimeMillis()

    @JvmStatic
    fun currentSystemTimeMillis() = System.currentTimeMillis()
}
