package org.mtransit.android.commons.provider.news.rss

import android.content.Context
import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.R
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.sharedJson
import java.io.ByteArrayInputStream

@Serializable
data class RssNewsConfig(
    @SerialName("color")
    val color: String,
    @SerialName("feeds")
    val feeds: List<String>,
    @SerialName("feeds_author_icon")
    val feedsAuthorIcon: List<String>,
    @SerialName("feeds_author_name")
    val feedsAuthorName: List<String>,
    @SerialName("feeds_author_url")
    val feedsAuthorUrl: List<String>,
    @SerialName("feeds_label")
    val feedsLabel: List<String>,
    @SerialName("feeds_lang")
    val feedsLang: List<String>,
    @SerialName("feeds_colors")
    val feedsColors: List<String>,
    @SerialName("feeds_target")
    val feedsTarget: List<String>,
    @SerialName("feeds_severity")
    val feedsSeverity: List<Int>,
    @SerialName("feeds_noteworthy")
    val feedsNoteworthy: List<Long>,
    @SerialName("feeds_ignore_guid")
    val feedsIgnoreGuid: List<Boolean>,
    @SerialName("feeds_ignore_link")
    val feedsIgnoreLink: List<Boolean>,
    @SerialName("feeds_date_link_fallback_regex")
    val feedsDateLinkFallbackRegex: List<String>,
    @SerialName("feeds_date_link_fallback_format")
    val feedsDateLinkFallbackFormat: List<String>,
    @SerialName("default_author_name")
    val defaultAuthorName: String?,
    @SerialName("default_severity")
    val defaultSeverity: Int,
    @SerialName("default_noteworthy")
    val defaultNoteworthy: Long,
    @SerialName("time_zone")
    val timeZone: String,
    @SerialName("copy_to_file_instead_of_streaming")
    val copyToFileInsteadOfStreaming: Boolean,
    @SerialName("encoding")
    val encoding: String,
    @SerialName("file_cleaning_regex")
    val fileCleaningRegex: List<String>,
    @SerialName("file_cleaning_replacement")
    val fileCleaningReplacement: List<String>,
    @SerialName("use_custom_ssl_certificate")
    val useCustomSslCertificate: Boolean,
    @SerialName("custom_ssl_certificate")
    val customSslCertificate: String?,
) {
    companion object {

        @JvmStatic
        fun fromProviderConfig(config: ProviderConfig): RssNewsConfig {
            require(config.type == ProviderConfig.TYPE_NEWS_RSS) { "Unsupported RSS config type" }
            return sharedJson.decodeFromJsonElement(config.params)
        }

        @JvmStatic
        fun fromResources(context: Context): RssNewsConfig {
            val resources = context.resources
            val useCustomSslCertificate = resources.getBoolean(R.bool.rss_use_custom_ssl_certificate)
            return RssNewsConfig(
                color = RssNewProviderUtils.pickColor(context, -1),
                feeds = resources.getStringArray(R.array.rss_feeds).toList(),
                feedsAuthorIcon = resources.getStringArray(R.array.rss_feeds_author_icon).toList(),
                feedsAuthorName = resources.getStringArray(R.array.rss_feeds_author_name).toList(),
                feedsAuthorUrl = resources.getStringArray(R.array.rss_feeds_author_url).toList(),
                feedsLabel = resources.getStringArray(R.array.rss_feeds_label).toList(),
                feedsLang = resources.getStringArray(R.array.rss_feeds_lang).toList(),
                feedsColors = resources.getStringArray(R.array.rss_feeds_colors).toList(),
                feedsTarget = resources.getStringArray(R.array.rss_feeds_target).toList(),
                feedsSeverity = resources.getIntArray(R.array.rss_feeds_severity).toList(),
                feedsNoteworthy = resources.getStringArray(R.array.rss_feeds_noteworthy).map { it.toLong() },
                feedsIgnoreGuid = resources.getStringArray(R.array.rss_feeds_ignore_guid).map { it.toBoolean() },
                feedsIgnoreLink = resources.getStringArray(R.array.rss_feeds_ignore_link).map { it.toBoolean() },
                feedsDateLinkFallbackRegex = resources.getStringArray(R.array.rss_feeds_date_link_fallback_regex).toList(),
                feedsDateLinkFallbackFormat = resources.getStringArray(R.array.rss_feeds_date_link_fallback_format).toList(),
                defaultAuthorName = RssNewProviderUtils.pickAuthorName(context, -1),
                defaultSeverity = RssNewProviderUtils.pickSeverity(context, -1),
                defaultNoteworthy = RssNewProviderUtils.pickNoteworthy(context, -1),
                timeZone = AgencyUtils.getAgencyTimeZoneId(context),
                copyToFileInsteadOfStreaming = resources.getBoolean(R.bool.rss_copy_to_file_instead_of_streaming),
                encoding = RssNewProviderUtils.pickEncoding(context),
                fileCleaningRegex = resources.getStringArray(R.array.rss_file_cleaning_regex).toList(),
                fileCleaningReplacement = resources.getStringArray(R.array.rss_file_cleaning_replacement).toList(),
                useCustomSslCertificate = useCustomSslCertificate,
                customSslCertificate = if (useCustomSslCertificate) {
                    resources.openRawResource(R.raw.rss_custom_ssl_certificate).use {
                        Base64.encodeToString(it.readBytes(), Base64.NO_WRAP)
                    }
                } else {
                    null
                },
            )
        }
    }

    init {
        require(!useCustomSslCertificate || !customSslCertificate.isNullOrBlank()) { "Missing RSS SSL certificate" }
    }

    fun toProviderConfig(authority: String, targetAuthority: String) = ProviderConfig.create(
        type = ProviderConfig.TYPE_NEWS_RSS,
        authority = authority,
        targetAuthority = targetAuthority,
        params = sharedJson.encodeToJsonElement(this).jsonObject,
        secrets = emptyMap(),
    )

    fun openCustomSslCertificate() = ByteArrayInputStream(
        Base64.decode(requireNotNull(customSslCertificate) { "Missing RSS SSL certificate" }, Base64.DEFAULT)
    )
}
