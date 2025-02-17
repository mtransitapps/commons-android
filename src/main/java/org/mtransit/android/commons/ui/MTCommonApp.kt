package org.mtransit.android.commons.ui

import android.app.Application
import androidx.annotation.CallSuper
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.di.TimeProvider

open class MTCommonApp : Application(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = MTCommonApp::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        TimeProvider.init(this)
    }
}