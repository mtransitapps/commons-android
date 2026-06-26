package org.mtransit.android.commons.provider.common

import android.content.Context
import android.content.UriMatcher
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.JSONUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.commons.SecureStringUtils.enc
import org.mtransit.android.commons.optBoolean
import org.mtransit.android.commons.optLong
import java.util.concurrent.TimeUnit

interface ProviderContract : Loggable {

    companion object {
        const val PING_PATH: String = "ping"

        @JvmStatic
        val MAX_CACHE_VALIDITY_MS = TimeUnit.DAYS.toMillis(1000L)
    }

    @Suppress("PropertyName")
    val URI_MATCHER: UriMatcher

    @get:WorkerThread
    val readDB: SQLiteDatabase

    @get:WorkerThread
    val writeDB: SQLiteDatabase

    fun requireContextCompat(): Context

    abstract class Filter {

        companion object {
            private const val CACHE_ONLY_DEFAULT = false
            private const val IN_FOCUS_DEFAULT = false

            private const val JSON_CACHE_ONLY = "cacheOnly"
            private const val JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs"
            private const val JSON_IN_FOCUS = "inFocus"
            private const val JSON_PROVIDED_ENCRYPT_KEYS_MAP = "providedEncryptKeysMap"

            @Throws(JSONException::class)
            fun getCacheOnlyFromJSON(json: JSONObject) = json.optBoolean(JSON_CACHE_ONLY, null)

            @Throws(JSONException::class)
            fun getInFocusFromJSON(json: JSONObject) = json.optBoolean(JSON_IN_FOCUS, null)

            @Throws(JSONException::class)
            fun getCacheValidityInMsFromJSON(json: JSONObject) = json.optLong(JSON_CACHE_VALIDITY_IN_MS, null)

            @Throws(JSONException::class)
            fun getProvidedEncryptKeysMapFromJSON(json: JSONObject): Map<String, String>? =
                json.optJSONObject(JSON_PROVIDED_ENCRYPT_KEYS_MAP)?.let { JSONUtils.toMapOfStrings(it) }

            @JvmStatic
            @Throws(JSONException::class)
            fun toJSON(filter: Filter, json: JSONObject) = filter.apply {
                cacheOnly?.let { json.put(JSON_CACHE_ONLY, it) }
                cacheValidityInMs?.let { json.put(JSON_CACHE_VALIDITY_IN_MS, it) }
                inFocus?.let { json.put(JSON_IN_FOCUS, it) }
                providedEncryptKeysMap?.let { json.put(JSON_PROVIDED_ENCRYPT_KEYS_MAP, JSONUtils.toJSONObject(it)) }
            }

            fun toProvidedKeys(keysMap: Map<String, String>?) = keysMap?.mapNotNull {
                val enc = enc(it.value) ?: return@mapNotNull null
                it.key to enc
            }?.toMap()
        }

        abstract val cacheOnly: Boolean?
        val isCacheOnlyOrDefault: Boolean get() = this.cacheOnly ?: CACHE_ONLY_DEFAULT

        abstract val cacheValidityInMs: Long?

        abstract val inFocus: Boolean?
        val isInFocusOrDefault: Boolean get() = this.inFocus ?: IN_FOCUS_DEFAULT

        abstract val providedEncryptKeysMap: Map<String, String>?

        fun getProvidedEncryptKey(key: String) =
            this.providedEncryptKeysMap?.get(key = key)
                ?.takeIf { it.trim().isNotEmpty() }

        @Suppress("unused")
        abstract fun toJSONString(): String?

        fun toStringParts() = buildString {
            cacheOnly?.let { append("cacheOnly:").append(it).append(",") }
            cacheValidityInMs?.let { append("cacheValidityInMs:").append(MTLog.formatDuration(it)).append(",") }
            inFocus?.let { append("inFocus:").append(it).append(",") }
            providedEncryptKeysMap?.let { append("providedEncryptKeysMap:").append(it.size).append(",") } // no not print keys
        }
    }
}
