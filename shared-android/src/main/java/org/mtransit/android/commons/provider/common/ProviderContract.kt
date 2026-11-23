package org.mtransit.android.commons.provider.common

import android.content.Context
import android.content.UriMatcher
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.SecureStringUtils
import org.mtransit.android.commons.toDurationLog
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import org.mtransit.commons.model.Secret
import kotlin.time.Duration.Companion.days

interface ProviderContract : MTLogK.Loggable {

    companion object {
        const val PING_PATH: String = "ping"

        @JvmStatic
        val MAX_CACHE_VALIDITY_MS = 1000L.days.inWholeMilliseconds
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

            fun JsonObject.getCacheOnlyFromJson() = this[JSON_CACHE_ONLY]?.jsonPrimitiveOrNull?.booleanOrNull

            fun JsonObject.getInFocusFromJson() = this[JSON_IN_FOCUS]?.jsonPrimitiveOrNull?.booleanOrNull

            fun JsonObject.getCacheValidityInMsFromJson() = this[JSON_CACHE_VALIDITY_IN_MS]?.jsonPrimitiveOrNull?.longOrNull

            fun JsonObject.getProvidedEncryptKeysMapFromJson(): Secret<Map<String, String>>? =
                this[JSON_PROVIDED_ENCRYPT_KEYS_MAP]?.jsonObjectOrNull?.mapNotNull { (key, jsonElement) ->
                    jsonElement.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotBlank() }?.let { key to it }
                }?.toMap()?.let { Secret(it) }

            fun JsonObjectBuilder.toJsonCommon(filter: Filter) = filter.apply {
                cacheOnly?.let { put(JSON_CACHE_ONLY, it) }
                cacheValidityInMs?.let { put(JSON_CACHE_VALIDITY_IN_MS, it) }
                inFocus?.let { put(JSON_IN_FOCUS, it) }
                providedEncryptKeysMap?.let {
                    put(
                        JSON_PROVIDED_ENCRYPT_KEYS_MAP,
                        buildJsonObject {
                            it.data.forEach { (key, value) -> put(key, value) }
                        }
                    )
                }
            }

            fun toProvidedKeys(keysMap: Map<String, String>?) = keysMap?.mapNotNull { (key, clear) ->
                val enc = SecureStringUtils.enc(clear) ?: return@mapNotNull null
                key to enc
            }?.toMap()?.let { Secret(it) }
        }

        abstract val cacheOnly: Boolean?
        val isCacheOnlyOrDefault: Boolean get() = this.cacheOnly ?: CACHE_ONLY_DEFAULT

        abstract val cacheValidityInMs: Long?

        abstract val inFocus: Boolean?
        val isInFocusOrDefault: Boolean get() = this.inFocus ?: IN_FOCUS_DEFAULT

        abstract val providedEncryptKeysMap: Secret<Map<String, String>>?

        fun getProvidedEncryptKey(key: String) =
            this.providedEncryptKeysMap?.data?.get(key = key)
                ?.takeIf { it.trim().isNotEmpty() }

        @Suppress("unused")
        @Throws(SerializationException::class)
        abstract fun toJsonString(): String?

        fun toStringParts() = buildString {
            cacheOnly?.let { append("cacheOnly:").append(it).append(",") }
            cacheValidityInMs?.let { append("cacheValidityInMs:").append(it.toDurationLog()).append(",") }
            inFocus?.let { append("inFocus:").append(it).append(",") }
            providedEncryptKeysMap?.let { append("providedEncryptKeysMap:").append(it.data.size).append(",") } // no not print keys
        }
    }
}
