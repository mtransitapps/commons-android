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

fun String?.toAvailabilityPercentOrNull(status: POIStatus) = this?.takeIf { it.isNotBlank() }?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject.toAvailabilityPercent(status)
}?.getOrNull()

private fun JsonObject.toAvailabilityPercent(status: POIStatus) = AvailabilityPercent(
    status,
    AvailabilityPercentValue(
        getValue(JSON_VALUE1).jsonPrimitive.int,
        getValue(JSON_VALUE1_EMPTY_RES).jsonPrimitive.content,
        getValue(JSON_VALUE1_QUANTITY_RES).jsonPrimitive.content,
        getValue(JSON_VALUE1_COLOR).jsonPrimitive.int,
        getValue(JSON_VALUE1_COLOR_BG).jsonPrimitive.int,
    ),
    this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE_DEFAULT_EMPTY_RES]?.jsonPrimitiveOrNull?.contentOrNull,
    this@toAvailabilityPercent[JSON_VALUE1_SUB_VALUE_DEFAULT_QUANTITY_RES]?.jsonPrimitiveOrNull?.contentOrNull,
    this.toValue1SubValue1OrNull(),
    AvailabilityPercentValue(
        getValue(JSON_VALUE2).jsonPrimitive.int,
        getValue(JSON_VALUE2_EMPTY_RES).jsonPrimitive.content,
        getValue(JSON_VALUE2_QUANTITY_RES).jsonPrimitive.content,
        getValue(JSON_VALUE2_COLOR).jsonPrimitive.int,
        getValue(JSON_VALUE2_COLOR_BG).jsonPrimitive.int,
    ),
    getValue(JSON_STATUS_MSG_ID).jsonPrimitive.int
)

@Suppress("ReturnCount")
private fun JsonObject.toValue1SubValue1OrNull(): AvailabilityPercentValue? {
    return AvailabilityPercentValue(
        this[JSON_VALUE1_SUB_VALUE1]?.jsonPrimitiveOrNull?.intOrNull ?: return null,
        this[JSON_VALUE1_SUB_VALUE1_EMPTY_RES]?.jsonPrimitiveOrNull?.contentOrNull ?: return null,
        this[JSON_VALUE1_SUB_VALUE1_QUANTITY_RES]?.jsonPrimitiveOrNull?.contentOrNull ?: return null,
        this[JSON_VALUE1_SUB_VALUE1_COLOR]?.jsonPrimitiveOrNull?.intOrNull ?: return null,
        this[JSON_VALUE1_SUB_VALUE1_COLOR_BG]?.jsonPrimitiveOrNull?.intOrNull ?: return null,
    )
}

fun AvailabilityPercent.toJson() = jsonRunCatching {
    buildJsonObject {
        toJsonCommon(this@toJson)
        putK(JSON_STATUS_MSG_ID, this@toJson.statusMsgId)
        with(this@toJson.value1) {
            putK(JSON_VALUE1, value)
            putK(JSON_VALUE1_EMPTY_RES, emptyRes)
            putK(JSON_VALUE1_QUANTITY_RES, quantityRes)
            putK(JSON_VALUE1_COLOR, color)
            putK(JSON_VALUE1_COLOR_BG, colorBg)
        }
        this@toJson.value1SubValueDefaultEmptyRes?.let { putK(JSON_VALUE1_SUB_VALUE_DEFAULT_EMPTY_RES, it) }
        this@toJson.value1SubValueDefaultQuantityRes?.let { putK(JSON_VALUE1_SUB_VALUE_DEFAULT_QUANTITY_RES, it) }
        this@toJson.value1SubValue1?.let { value1SubValue1 ->
            putK(JSON_VALUE1_SUB_VALUE1, value1SubValue1.value)
            putK(JSON_VALUE1_SUB_VALUE1_EMPTY_RES, value1SubValue1.emptyRes)
            putK(JSON_VALUE1_SUB_VALUE1_QUANTITY_RES, value1SubValue1.quantityRes)
            putK(JSON_VALUE1_SUB_VALUE1_COLOR, value1SubValue1.color)
            putK(JSON_VALUE1_SUB_VALUE1_COLOR_BG, value1SubValue1.colorBg)
        }
        with(this@toJson.value2) {
            putK(JSON_VALUE2, value)
            putK(JSON_VALUE2_EMPTY_RES, emptyRes)
            putK(JSON_VALUE2_QUANTITY_RES, quantityRes)
            putK(JSON_VALUE2_COLOR, color)
            putK(JSON_VALUE2_COLOR_BG, colorBg)
        }
    }
}.getOrThrow()

fun AvailabilityPercent.toJsonStringOrNull() = jsonRunCatching {
    sharedJson.encodeToString(this.toJson())
}.getOrNull()

val AvailabilityPercent.value1Default
    get() = this.value1.copy(
        value = this.value1.value - (this.value1SubValue1?.value ?: 0),
        emptyRes = this.value1SubValueDefaultEmptyRes ?: this.value1.emptyRes,
        quantityRes = this.value1SubValueDefaultQuantityRes ?: this.value1.quantityRes,
    )
