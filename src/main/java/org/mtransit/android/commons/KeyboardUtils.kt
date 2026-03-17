package org.mtransit.android.commons

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

@Suppress("unused")
class KeyboardUtils : MTLog.Loggable {

    override fun getLogTag() = LOG_TAG

    companion object {
        private val LOG_TAG: String = KeyboardUtils::class.java.simpleName

        @JvmStatic
        fun showKeyboard(activity: Activity?, view: View?) {
            view ?: return
            val imm = activity?.getSystemService<InputMethodManager>() ?: return
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }

        @JvmStatic
        fun hideKeyboard(activity: Activity?, view: View?) {
            view ?: return
            val imm = activity?.getSystemService<InputMethodManager>() ?: return
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}