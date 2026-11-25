package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.data.AvailabilityPercent.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_STATUS_MSG_ID = "statusMsgId"
private const val JSON_VALUE1 = "value1"
private const val JSON_VALUE1_EMPTY_RES = "value1EmptyRes"
private const val JSON_VALUE1_QUANTITY_RES = "value1QuantityRes"
private const val JSON_VALUE1_COLOR = "value1Color"
private const val JSON_VALUE1_COLOR_BG = "value1ColorBg"
private const val JSON_VALUE1_SUB_VALUE_DEFAULT_EMPTY_RES = "value1SubValueDefaultEmptyRes"
private const val JSON_VALUE1_SUB_VALUE_DEFAULT_QUANTITY_RES = "value1SubValueDefaultQuantityRes"
private const val JSON_VALUE1_SUB_VALUE1 = "value1SubValue1"
private const val JSON_VALUE1_SUB_VALUE1_EMPTY_RES = "value1SubValue1EmptyRes"
private const val JSON_VALUE1_SUB_VALUE1_QUANTITY_RES = "value1SubValue1QuantityRes"
private const val JSON_VALUE1_SUB_VALUE1_COLOR = "value1SubValue1Color"
private const val JSON_VALUE1_SUB_VALUE1_COLOR_BG = "value1SubValue1ColorBg"
private const val JSON_VALUE2 = "value2"
private const val JSON_VALUE2_EMPTY_RES = "value2EmptyRes"
private const val JSON_VALUE2_QUANTITY_RES = "value2QuantityRes"
private const val JSON_VALUE2_COLOR = "value2Color"
private const val JSON_VALUE2_COLOR_BG = "value2ColorBg"

fun String?.toAvailabilityPercentOrNull(status: POIStatus) = this?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject.toAvailabilityPercent(status)
}?.getOrNull()

private fun JsonObject.toAvailabilityPercent(status: POIStatus) = AvailabilityPercent(status).apply {
    this.statusMsgId = getValue(JSON_STATUS_MSG_ID).jsonPrimitive.int
    this.value1 = getValue(JSON_VALUE1).jsonPrimitive.int
    this.value1EmptyRes = getValue(JSON_VALUE1_EMPTY_RES).jsonPrimitive.content
    this.value1QuantityRes = getValue(JSON_VALUE1_QUANTITY_RES).jsonPrimitive.content
    this.value1Color = getValue(JSON_VALUE1_COLOR).jsonPrimitive.int
    this.value1ColorBg = getValue(JSON_VALUE1_COLOR_BG).jsonPrimitive.int
    this.value1SubValueDefaultEmptyRes = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE_DEFAULT_EMPTY_RES]?.jsonPrimitiveOrNull?.contentOrNull
    this.value1SubValueDefaultQuantityRes = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE_DEFAULT_QUANTITY_RES]?.jsonPrimitiveOrNull?.contentOrNull
    this.value1SubValue1 = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE1]?.jsonPrimitiveOrNull?.intOrNull
    this.value1SubValue1EmptyRes = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE1_EMPTY_RES]?.jsonPrimitiveOrNull?.contentOrNull
    this.value1SubValue1QuantityRes = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE1_QUANTITY_RES]?.jsonPrimitiveOrNull?.contentOrNull
    this.value1SubValue1Color = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE1_COLOR]?.jsonPrimitiveOrNull?.intOrNull
    this.value1SubValue1ColorBg = this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE1_COLOR_BG]?.jsonPrimitiveOrNull?.intOrNull
    this.value2 = getValue(JSON_VALUE2).jsonPrimitive.int
    this.value2EmptyRes = getValue(JSON_VALUE2_EMPTY_RES).jsonPrimitive.content
    this.value2QuantityRes = getValue(JSON_VALUE2_QUANTITY_RES).jsonPrimitive.content
    this.value2Color = getValue(JSON_VALUE2_COLOR).jsonPrimitive.int
    this.value2ColorBg = getValue(JSON_VALUE2_COLOR_BG).jsonPrimitive.int
}

fun AvailabilityPercent.toJson() = jsonRunCatching {
    buildJsonObject {
        toJsonCommon(this@toJson)
        putK(JSON_STATUS_MSG_ID, this@toJson.statusMsgId)
        putK(JSON_VALUE1, this@toJson.value1)
        putK(JSON_VALUE1_EMPTY_RES, this@toJson.value1EmptyRes)
        putK(JSON_VALUE1_QUANTITY_RES, this@toJson.value1QuantityRes)
        putK(JSON_VALUE1_COLOR, this@toJson.value1Color)
        putK(JSON_VALUE1_COLOR_BG, this@toJson.value1ColorBg)
        if (this@toJson.value1SubValueDefaultEmptyRes != null
            && this@toJson.value1SubValueDefaultQuantityRes != null
            && this@toJson.value1SubValue1 != null
            && this@toJson.value1SubValue1EmptyRes != null
            && this@toJson.value1SubValue1QuantityRes != null
            && this@toJson.value1SubValue1Color != null
            && this@toJson.value1SubValue1ColorBg != null
        ) {
            putK(JSON_VALUE1_SUB_VALUE_DEFAULT_EMPTY_RES, this@toJson.value1SubValueDefaultEmptyRes)
            putK(JSON_VALUE1_SUB_VALUE_DEFAULT_QUANTITY_RES, this@toJson.value1SubValueDefaultQuantityRes)
            putK(JSON_VALUE1_SUB_VALUE1, this@toJson.value1SubValue1)
            putK(JSON_VALUE1_SUB_VALUE1_EMPTY_RES, this@toJson.value1SubValue1EmptyRes)
            putK(JSON_VALUE1_SUB_VALUE1_QUANTITY_RES, this@toJson.value1SubValue1QuantityRes)
            putK(JSON_VALUE1_SUB_VALUE1_COLOR, this@toJson.value1SubValue1Color)
            putK(JSON_VALUE1_SUB_VALUE1_COLOR_BG, this@toJson.value1SubValue1ColorBg)
        }
        putK(JSON_VALUE2, this@toJson.value2)
        putK(JSON_VALUE2_EMPTY_RES, this@toJson.value2EmptyRes)
        putK(JSON_VALUE2_QUANTITY_RES, this@toJson.value2QuantityRes)
        putK(JSON_VALUE2_COLOR, this@toJson.value2Color)
        putK(JSON_VALUE2_COLOR_BG, this@toJson.value2ColorBg)
    }
}.getOrThrow()

fun AvailabilityPercent.toJsonString() = sharedJson.encodeToString(this.toJson())
