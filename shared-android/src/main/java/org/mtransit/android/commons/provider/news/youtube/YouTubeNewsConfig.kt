package org.mtransit.android.commons.provider.news.youtube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeNewsConfig(
    @SerialName("color")
    val color: String?,
    @SerialName("channels_username")
    val channelsUsername: List<String>,
    @SerialName("channels_id")
    val channelsId: List<String>,
    @SerialName("channels_handle")
    val channelsHandle: List<String>,
    @SerialName("channels_author_url")
    val channelsAuthorUrl: List<String>,
    @SerialName("channels_lang")
    val channelsLang: List<String>,
    @SerialName("channels_colors")
    val channelsColors: List<String>,
    @SerialName("channels_target")
    val channelsTarget: List<String>,
    @SerialName("channels_severity")
    val channelsSeverity: List<Int>,
    @SerialName("channels_noteworthy")
    val channelsNoteworthy: List<Long>,
    @SerialName("default_severity")
    val defaultSeverity: Int,
    @SerialName("default_noteworthy")
    val defaultNoteworthy: Long,
)
