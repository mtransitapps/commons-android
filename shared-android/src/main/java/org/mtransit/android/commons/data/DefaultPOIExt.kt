@file:Suppress("TooManyFunctions")

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

@Suppress("LongParameterList")
@JvmOverloads
fun makeDefaultPOI(
    authority: String,
    id: Int,
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int,
    @ItemViewType type: Int,
    @POI.ItemStatusType statusType: Int,
    @POI.ItemActionType actionsType: Int,
    lat: Double,
    lng: Double,
    name: String,
    score: Int? = null,
    @Accessibility.AccessibilityType accessible: Int = Accessibility.DEFAULT
) = DefaultPOI(
    authority,
    id,
    dataSourceTypeId,
    type,
    statusType,
    actionsType,
    lat,
    lng,
    name
).apply {
    optDefaultPOIInit(
        score = score,
        accessible = accessible
    )
}

fun <T : DefaultPOI> T.optDefaultPOIInit(
    score: Int? = null,
    @Accessibility.AccessibilityType accessible: Int = Accessibility.DEFAULT,
) = this.apply {
    this.scoreOpt = score
    this.accessible = accessible
}

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

fun JsonObject.toPOIOrThrow(): POI {
    when (val type = getTypeFromJsonOrDefault()) {
        POI.ITEM_VIEW_TYPE_BASIC_POI -> return toDefaultPOIOrThrow(type)
        POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP -> return toRouteDirectionStop()
        POI.ITEM_VIEW_TYPE_MODULE,
        POI.ITEM_VIEW_TYPE_TEXT_MESSAGE -> {
            MTLogK.w(LOG_TAG, "Unexpected POI type '$type'! (using default) (json: $this)")
            return toDefaultPOIOrThrow(type)
        }

        else -> {
            MTLogK.w(LOG_TAG, "Unexpected POI type '$type'! (using default) (json: $this)")
            return toDefaultPOIOrThrow(type)
        }
    }
}

@ItemViewType
private fun JsonObject.getTypeFromJsonOrDefault() = jsonRunCatching(LOG_TAG) {
    getValue(JSON_TYPE).jsonPrimitive.int
}.getOrDefault(POI.ITEM_VIEW_TYPE_BASIC_POI)

private fun JsonObject.toDefaultPOIOrThrow(
    @ItemViewType type: Int = getTypeFromJsonOrDefault()
) = jsonRunCatching(LOG_TAG) {
    makeDefaultPOI(
        authority = getAuthorityFromJson(),
        id = getIdFromJson(),
        dataSourceTypeId = getDSTypeIdFromJson(),
        type = type,
        statusType = getValue(JSON_STATUS_TYPE).jsonPrimitive.int,
        actionsType = this[JSON_ACTION_TYPE]?.jsonPrimitiveOrNull?.intOrNull ?: POI.ITEM_ACTION_TYPE_NONE,
        lat = getLatFromJson(),
        lng = getLngFromJson(),
        name = getNameFromJson(),
        score = getScoreFromJson(),
    )
}.getOrThrow()

fun JsonObject.getScoreFromJson(): Int? = this[JSON_SCORE_OPT]?.jsonPrimitiveOrNull?.intOrNull

fun JsonObject.getIdFromJson() = getValue(JSON_ID).jsonPrimitive.int

fun JsonObject.getAuthorityFromJson() = getValue(JSON_AUTHORITY).jsonPrimitive.content

fun JsonObject.getDSTypeIdFromJson(): @DataSourceTypeId.DataSourceType Int = getValue(JSON_DATA_SOURCE_TYPE_ID).jsonPrimitive.int

fun JsonObject.getLatFromJson() = getValue(JSON_LAT).jsonPrimitive.double

fun JsonObject.getLngFromJson() = getValue(JSON_LNG).jsonPrimitive.double

fun JsonObject.getNameFromJson() = getValue(JSON_NAME).jsonPrimitive.content

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
