package org.mtransit.android.commons.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import org.mtransit.android.commons.BuildConfig
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.R

class InvisibleActivity : Activity(),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = InvisibleActivity::class.java.simpleName

        @Suppress("unused")
        val CLASS_NAME: String = InvisibleActivity::class.java.canonicalName ?: "org.mtransit.android.commons.ui.InvisibleActivity"
    }

    override fun getLogTag() = LOG_TAG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            setContentView(R.layout.activity_invisible)
            findViewById<View>(android.R.id.content)?.rootView?.setBackgroundColor(Color.CYAN)
        }
        finish()
    }
}