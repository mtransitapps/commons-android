package org.mtransit.android.commons

import android.util.Base64

object SecureStringUtils {

    private val LOG_TAG: String = SecureStringUtils::class.java.simpleName

    @JvmStatic
    fun enc(clearString: String?): String? {
        return try {
            clearString?.takeIf { it.isNotBlank() }?.let {
                Base64.encode(it.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
            }?.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while encoding string!")
            null
        }
    }

    @JvmStatic
    fun dec(encString: String?): String? {
        return try {
            encString?.takeIf { it.isNotBlank() }?.let {
                Base64.decode(encString, Base64.DEFAULT)
            }?.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while decoding string!")
            null
        }
    }
}