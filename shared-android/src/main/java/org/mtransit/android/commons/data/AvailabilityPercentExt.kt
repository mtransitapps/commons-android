package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.AvailabilityPercent.LOG_TAG
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

fun POIStatus.toAvailabilityPercent(jsonString: String) = try {
    this.toAvailabilityPercent(sharedJson.parseToJsonElement(jsonString).jsonObject)
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON string '$jsonString'", se)
    throw se
}

fun POIStatus.toAvailabilityPercent(jsonObject: JsonObject) = try {
    AvailabilityPercent(this).apply {
        statusMsgId = jsonObject.getValue(JSON_STATUS_MSG_ID).jsonPrimitive.int
        value1 = jsonObject.getValue(JSON_VALUE1).jsonPrimitive.int
        value1EmptyRes = jsonObject.getValue(JSON_VALUE1_EMPTY_RES).jsonPrimitive.content
        value1QuantityRes = jsonObject.getValue(JSON_VALUE1_QUANTITY_RES).jsonPrimitive.content
        value1Color = jsonObject.getValue(JSON_VALUE1_COLOR).jsonPrimitive.int
        value1ColorBg = jsonObject.getValue(JSON_VALUE1_COLOR_BG).jsonPrimitive.int
        value1SubValueDefaultEmptyRes = jsonObject.get(JSON_VALUE1_SUB_VALUE_DEFAULT_EMPTY_RES)?.jsonPrimitiveOrNull?.contentOrNull
        value1SubValueDefaultQuantityRes = jsonObject.get(JSON_VALUE1_SUB_VALUE_DEFAULT_QUANTITY_RES)?.jsonPrimitiveOrNull?.contentOrNull
        value1SubValue1 = jsonObject.get(JSON_VALUE1_SUB_VALUE1)?.jsonPrimitiveOrNull?.intOrNull
        value1SubValue1EmptyRes = jsonObject.get(JSON_VALUE1_SUB_VALUE1_EMPTY_RES)?.jsonPrimitiveOrNull?.contentOrNull
        value1SubValue1QuantityRes = jsonObject.get(JSON_VALUE1_SUB_VALUE1_QUANTITY_RES)?.jsonPrimitiveOrNull?.contentOrNull
        value1SubValue1Color = jsonObject.get(JSON_VALUE1_SUB_VALUE1_COLOR)?.jsonPrimitiveOrNull?.intOrNull
        value1SubValue1ColorBg = jsonObject.get(JSON_VALUE1_SUB_VALUE1_COLOR_BG)?.jsonPrimitiveOrNull?.intOrNull
        value2 = jsonObject.getValue(JSON_VALUE2).jsonPrimitive.int
        value2EmptyRes = jsonObject.getValue(JSON_VALUE2_EMPTY_RES).jsonPrimitive.content
        value2QuantityRes = jsonObject.getValue(JSON_VALUE2_QUANTITY_RES).jsonPrimitive.content
        value2Color = jsonObject.getValue(JSON_VALUE2_COLOR).jsonPrimitive.int
        value2ColorBg = jsonObject.getValue(JSON_VALUE2_COLOR_BG).jsonPrimitive.int
    }
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$jsonObject'", se)
    throw se
}

fun AvailabilityPercent.toJson() = try {
    buildJsonObject {
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
} catch (se: SerializationException) {
    MTLogK.w(this, "Error while making JSON object '$this'", se)
    throw se
}
