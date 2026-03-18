package org.mtransit.android.commons.provider.ca.info.stm

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.secsToInstant
import kotlin.time.Instant

data class EtatServiceResponse(
    @SerializedName("header")
    val header: Header?,
    @SerializedName("alerts")
    val alerts: List<Alert>?,
) {
    data class Header(
        @SerializedName("timestamp")
        val timestampInSec: Long?, // in sec
    )

    data class Alert(
        @SerializedName("active_periods")
        val activePeriods: ActivePeriods?,
        @SerializedName("cause")
        val cause: String?,
        @SerializedName("effect")
        val effect: String?,
        @SerializedName("informed_entities")
        val informedEntities: List<InformedEntity>?,
        @SerializedName("header_texts")
        val headerTexts: List<TranslatedText>?,
        @SerializedName("description_texts")
        val descriptionTexts: List<TranslatedText>?,
    ) {
        data class ActivePeriods(
            @SerializedName("start")
            val startInSec: Int?,
            @SerializedName("end")
            val endInSec: Int?,
        )
        data class InformedEntity(
            @SerializedName("route_short_name")
            val routeShortName: String?,
            @SerializedName("direction_id")
            val directionId: String?,
            @SerializedName("stop_code")
            val stopCode: String?,
        )
        data class TranslatedText(
            @SerializedName("language")
            val language: String?,
            @SerializedName("text")
            val text: String?,
        )
    }
}

val EtatServiceResponse.Header.timestamp: Instant? get() = timestampInSec?.secsToInstant()
val EtatServiceResponse.Alert.ActivePeriods.start: Instant? get() = startInSec?.secsToInstant()
val EtatServiceResponse.Alert.ActivePeriods.end: Instant? get() = endInSec?.secsToInstant()
