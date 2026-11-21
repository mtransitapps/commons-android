package org.mtransit.android.commons.ui

import android.app.Application
import androidx.annotation.CallSuper
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.di.TimeProvider
import org.mtransit.commons.CommonsApp

open class MTCommonApp : Application(), MTLogK.Loggable {

    companion object {
        private val LOG_TAG: String = MTCommonApp::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    init {
        CommonsApp.setup(isAndroidPlatform = true)
    }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        TimeProvider.init(this)
    }
}
