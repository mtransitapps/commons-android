package org.mtransit.android.commons.provider.common

import android.database.MatrixCursor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.intellij.lang.annotations.Language
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.SecureStringUtils

@Serializable
class ProviderConfig private constructor(
    @SerialName("version")
    val schemaVersion: Int,
    @SerialName("type")
    val type: String,
    @SerialName("authority")
    val authority: String,
    @SerialName("target_authority")
    val targetAuthority: String,
    @SerialName("params")
    val params: JsonObject,
    @SerialName("secrets")
    val encSecrets: Map<String, String>,
) {

    companion object {
        const val SCHEMA_VERSION = 1
        const val TYPE_NEWS_TWITTER = "news.twitter"
        const val TYPE_NEWS_YOUTUBE = "news.youtube"
        const val TYPE_NEWS_RSS = "news.rss"
        const val TYPE_NEWS_INSTAGRAM = "news.instagram"

        fun create(
            type: String,
            authority: String,
            targetAuthority: String,
            params: JsonObject,
            secrets: Map<String, String>,
            encoder: (clear: String) -> String? = SecureStringUtils::enc,
        ): ProviderConfig = ProviderConfig(
            schemaVersion = SCHEMA_VERSION,
            type = type,
            authority = authority,
            targetAuthority = targetAuthority,
            params = params,
            encSecrets = secrets.filterValues { it.isNotBlank() }.mapValues {
                requireNotNull(encoder(it.value)) { "Cannot encode provider config secret" }
            },
        )

        private val AUTHORITY_PREFIX = "^(org\\.mtransit\\.android\\.)".toRegex()

        @Suppress("SpellCheckingInspection")
        @Language("RegExp")
        private val PROVIDER_AUTHORITIES_SUFFIX = listOf(
            "(news\\.(rss|twitter|youtube|instagram))",

            "(gtfs(\\.realtime)?)",
            "(bike)",

            "(clever_devices)",
            "(nextbus)",
            "(oba)",

            "(transitlive_com)",
            "(rtcquebec)",
            "(stminfoapi)",
            "(winnipeg_transit)",
        )

        @Language("RegExp")
        private val PROVIDER_AUTHORITIES_SUFFIX_PREFIX = "(^|\\.)"

        private val AUTHORITY_DEBUG = "(debug\\.)".toRegex()
        private val AUTHORITY_SUFFIX = PROVIDER_AUTHORITIES_SUFFIX
            .joinToString(separator = "|", prefix = "$PROVIDER_AUTHORITIES_SUFFIX_PREFIX(", postfix = ")?$", limit = -1, truncated = "")
            .toRegex()

        private val AUTHORITY_CLEAN = "\\.".toRegex()

        fun authorityToStoragePrefix(authority: String) =
            authority
                .replaceFirst(AUTHORITY_PREFIX, "")
                .let {
                    if (Constants.IS_DEBUG_BUILD) {
                        it.replaceFirst(AUTHORITY_DEBUG, "")
                    } else {
                        it
                    }
                }
                .replaceFirst(AUTHORITY_SUFFIX, "")
                .replace(AUTHORITY_CLEAN, "_")

        fun String.toConfig() = sharedJson.decodeFromString<ProviderConfig>(this)
    }

    init {
        require(authority.isNotBlank()) { "Missing provider config authority" }
        require(type.isNotBlank()) { "Missing provider config type" }
        require(encSecrets.values.none { it.isBlank() }) { "Empty encoded provider config secret" }
    }

    fun toJson() = sharedJson.encodeToString(this)

    fun toCursor(): MatrixCursor = MatrixCursor(arrayOf(ProviderConfigContract.COLUMN_JSON), 1).apply {
        addRow(arrayOf(toJson()))
    }

    override fun toString() = "ProviderConfig(schemaVersion=$schemaVersion, type=$type, authority=$authority)"

    val authorityStoragePrefix by lazy { authorityToStoragePrefix(this.authority) }
}
