package org.mtransit.android.commons.provider

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.RTCQuebecProvider.JArretParcours
import org.mtransit.android.commons.provider.RTCQuebecProvider.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull

fun String?.toJArretParcours() = this?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObjectOrNull?.toJArretParcours()
}?.getOrNull()

private const val JSON_ARRET_NON_DESSERVI = "arretNonDesservi"
private const val JSON_DESCENTE_SEULEMENT = "descenteSeulement"
private const val JSON_HORAIRES = "horaires"
private const val JSON_ARRET = "arret"
private const val JSON_PARCOURS = "parcours"
private const val JSON_ACCESSIBLE = "accessible"

private fun JsonObject.toJArretParcours() = JArretParcours(
    JArretParcours.JArret(this[JSON_ARRET]?.jsonObjectOrNull?.get(JSON_ACCESSIBLE)?.jsonPrimitiveOrNull?.booleanOrNull),
    this[JSON_ARRET_NON_DESSERVI]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
    this[JSON_DESCENTE_SEULEMENT]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
    this[JSON_HORAIRES]?.jsonArrayOrNull?.mapNotNull { it.jsonObject.toJHoraires() }.orEmpty(),
    JArretParcours.JParcours(this[JSON_PARCOURS]?.jsonObjectOrNull?.get(JSON_ACCESSIBLE)?.jsonPrimitiveOrNull?.booleanOrNull),
)

private const val JSON_DEPART = "depart"
private const val JSON_DEPART_MINUTES = "departMinutes"
private const val JSON_NTR = "ntr"
private const val JSON_ANNULE = "annule"
private const val JSON_NOM_DESTINATION = "nomDestination"

private fun JsonObject.toJHoraires() =
    this[JSON_DEPART]?.jsonPrimitiveOrNull?.contentOrNull?.let { depart ->
        JArretParcours.JHoraires(
            depart,
            this[JSON_DEPART_MINUTES]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: -1,
            this[JSON_NTR]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
            this[JSON_ANNULE]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
            this[JSON_NOM_DESTINATION]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
        )
    }
