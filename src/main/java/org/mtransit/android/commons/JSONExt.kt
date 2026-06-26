package org.mtransit.android.commons

import org.json.JSONObject

fun JSONObject.optLong(name: String, fallback: Long? = null) =
    takeIf { it.has(name) && !it.isNull(name) }?.optLong(name) ?: fallback

fun JSONObject.optInt(name: String, fallback: Int? = null) =
    takeIf { it.has(name) && !it.isNull(name) }?.optInt(name) ?: fallback

fun JSONObject.optDouble(name: String, fallback: Double? = null) =
    takeIf { it.has(name) && !it.isNull(name) }?.optDouble(name) ?: fallback

fun JSONObject.optBoolean(name: String, fallback: Boolean? = null) =
    takeIf { it.has(name) && !it.isNull(name) }?.optBoolean(name) ?: fallback

fun JSONObject.optString(name: String, fallback: String? = null) =
    takeIf { it.has(name) && !it.isNull(name) }?.optString(name) ?: fallback

