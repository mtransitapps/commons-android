package org.mtransit.android.commons

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

@Suppress("unused")
class KeyboardUtils : MTLog.Loggable {

    override fun getLogTag(): String {
        return TAG
    }

    companion object {
        private val TAG = KeyboardUtils::class.java.simpleName

        @JvmStatic
        fun showKeyboard(activity: Activity?, view: View?) {
            if (activity == null || view == null) {
                return
            }
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }

        @JvmStatic
        fun hideKeyboard(activity: Activity?, view: View?) {
            if (activity == null || view == null) {
                return
            }
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}