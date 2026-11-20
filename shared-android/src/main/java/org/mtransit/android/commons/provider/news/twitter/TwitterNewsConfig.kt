package org.mtransit.android.commons.provider.news.twitter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitterNewsConfig(
    @SerialName("color")
    val color: String?,
    @SerialName("screen_names")
    val screenNames: List<String>,
    @SerialName("screen_names_id")
    val screenNamesId: List<String>,
    @SerialName("screen_names_lang")
    val screenNamesLang: List<String>,
    @SerialName("screen_names_colors")
    val screenNamesColors: List<String>,
    @SerialName("screen_names_target")
    val screenNamesTarget: List<String>,
    @SerialName("screen_names_severity")
    val screenNamesSeverity: List<Int>,
    @SerialName("screen_names_noteworthy")
    val screenNamesNoteworthy: List<Long>,
    @SerialName("default_severity")
    val defaultSeverity: Int,
    @SerialName("default_noteworthy")
    val defaultNoteworthy: Long,
)
