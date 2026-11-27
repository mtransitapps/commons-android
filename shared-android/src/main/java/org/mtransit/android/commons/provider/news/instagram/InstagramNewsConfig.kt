package org.mtransit.android.commons.provider.news.instagram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstagramNewsConfig(
    @SerialName("color")
    val color: String?,
    @SerialName("user_names")
    val userNames: List<String>,
    @SerialName("user_names_lang")
    val userNamesLang: List<String>,
    @SerialName("user_names_colors")
    val userNamesColors: List<String>,
    @SerialName("user_names_target")
    val userNamesTarget: List<String>,
    @SerialName("user_names_severity")
    val userNamesSeverity: List<Int>,
    @SerialName("user_names_noteworthy")
    val userNamesNoteworthy: List<Long>,
    @SerialName("default_severity")
    val defaultSeverity: Int,
    @SerialName("default_noteworthy")
    val defaultNoteworthy: Long,
)
