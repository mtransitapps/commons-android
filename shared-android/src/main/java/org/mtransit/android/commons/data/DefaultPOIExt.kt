package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.DefaultPOI.LOG_TAG
import org.mtransit.android.commons.data.POI.ItemViewType
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_AUTHORITY = "authority"
private const val JSON_ID = "id"
private const val JSON_NAME = "name"
private const val JSON_LAT = "lat"
private const val JSON_LNG = "lng"
private const val JSON_DATA_SOURCE_TYPE_ID = "dst"
private const val JSON_TYPE = "type"
private const val JSON_STATUS_TYPE = "statusType"
private const val JSON_ACTION_TYPE = "actionsType"
private const val JSON_SCORE_OPT = "scoreOpt"

fun JsonObject.toPOI(): POI {
    when (val type = this.getTypeFromJson()) {
        POI.ITEM_VIEW_TYPE_BASIC_POI -> return this.toDefaultPOI(type)
        POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP -> return this.toRouteDirectionStop()
        POI.ITEM_VIEW_TYPE_MODULE, POI.ITEM_VIEW_TYPE_TEXT_MESSAGE -> {
            MTLogK.w(LOG_TAG, "Unexpected POI type '$type'! (using default) (json: $this)")
            return this.toDefaultPOI(type)
        }

        else -> {
            MTLogK.w(LOG_TAG, "Unexpected POI type '$type'! (using default) (json: $this)")
            return this.toDefaultPOI(type)
        }
    }
}

@ItemViewType
private fun JsonObject.getTypeFromJson() = jsonRunCatching(LOG_TAG) {
    getValue(JSON_TYPE).jsonPrimitive.int
}.getOrDefault(POI.ITEM_VIEW_TYPE_BASIC_POI)

private fun JsonObject.toDefaultPOI(
    @ItemViewType type: Int = getTypeFromJson()
) = jsonRunCatching(LOG_TAG) {
    DefaultPOI(
        getAuthorityFromJson(),
        getIdFromJson(),
        getDSTypeIdFromJson(),
        type,
        getValue(JSON_STATUS_TYPE).jsonPrimitive.int,
        this[JSON_ACTION_TYPE]?.jsonPrimitiveOrNull?.intOrNull ?: POI.ITEM_ACTION_TYPE_NONE,
        getLatFromJson(),
        getLngFromJson(),
        getNameFromJson()
    ).apply {
        applyTo(this)
    }
}.getOrThrow()

fun JsonObject.applyTo(defaultPOI: POI) {
    this[JSON_SCORE_OPT]?.let { defaultPOI.setScore(it.jsonPrimitive.int) }
}

fun JsonObject.getIdFromJson(): Int {
    return getValue(JSON_ID).jsonPrimitive.int
}

fun JsonObject.getAuthorityFromJson(): String {
    return getValue(JSON_AUTHORITY).jsonPrimitive.content
}

fun JsonObject.getDSTypeIdFromJson(): @DataSourceTypeId.DataSourceType Int {
    return getValue(JSON_DATA_SOURCE_TYPE_ID).jsonPrimitive.int
}

fun JsonObject.getLatFromJson(): Double {
    return getValue(JSON_LAT).jsonPrimitive.double
}

fun JsonObject.getLngFromJson(): Double {
    return getValue(JSON_LNG).jsonPrimitive.double
}

fun JsonObject.getNameFromJson(): String {
    return getValue(JSON_NAME).jsonPrimitive.content
}

fun DefaultPOI.toJsonK() = jsonRunCatching {
    buildJsonObject { toJsonCommon(this@toJsonK) }
}.getOrThrow()

fun JsonObjectBuilder.toJsonCommon(defaultPOI: DefaultPOI) {
    putK(JSON_AUTHORITY, defaultPOI.authority)
    putK(JSON_ID, defaultPOI.id)
    putK(JSON_NAME, defaultPOI.getName())
    putK(JSON_LAT, defaultPOI.lat)
    putK(JSON_LNG, defaultPOI.lng)
    putK(JSON_DATA_SOURCE_TYPE_ID, defaultPOI.dataSourceTypeId)
    putK(JSON_TYPE, defaultPOI.type)
    putK(JSON_STATUS_TYPE, defaultPOI.statusType)
    putK(JSON_ACTION_TYPE, defaultPOI.actionsType)
    defaultPOI.score?.let { putK(JSON_SCORE_OPT, it) }
}
