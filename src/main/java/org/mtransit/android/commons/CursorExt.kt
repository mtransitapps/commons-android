@file:Suppress("unused")

package org.mtransit.android.commons

import android.database.Cursor
import org.mtransit.commons.sql.fromSQL

fun Cursor.optNotNull(columnIndex: Int) = columnIndex.takeIf { it >= 0 }?.takeIf { !isNull(it) }

// region Boolean

fun Cursor.getBoolean(columnName: String) = this.getInt(getColumnIndexOrThrow(columnName)).fromSQL()

// endregion

// region Double

fun Cursor.getDouble(columnName: String) = this.getDouble(getColumnIndexOrThrow(columnName))

// endregion

// region Float

fun Cursor.getFloat(columnName: String) = this.getFloat(getColumnIndexOrThrow(columnName))

fun Cursor.optFloat(columnIndex: Int, fallback: Float? = null) =
    optNotNull(columnIndex)?.let { getFloat(it) } ?: fallback

fun Cursor.optFloat(columnName: String, fallback: Float? = null) =
    this.optFloat(getColumnIndex(columnName), fallback)

// endregion

// region Int

fun Cursor.optInt(columnIndex: Int, fallback: Int? = null) =
    optNotNull(columnIndex)?.let { getInt(it) } ?: fallback

fun Cursor.optInt(columnName: String, fallback: Int? = null) =
    this.optInt(getColumnIndex(columnName), fallback)

fun Cursor.optIntNN(columnIndex: Int, fallback: Int) =
    optNotNull(columnIndex)?.let { getInt(it) } ?: fallback

fun Cursor.optIntNN(columnName: String, fallback: Int) = this.optIntNN(getColumnIndex(columnName), fallback)

fun Cursor.getInt(columnName: String) = this.getInt(getColumnIndexOrThrow(columnName))

// endregion

// region Long

fun Cursor.getLong(columnName: String) = this.getLong(getColumnIndexOrThrow(columnName))

fun Cursor.optLong(columnIndex: Int, fallback: Long? = null) =
    optNotNull(columnIndex)?.let { getLong(it) } ?: fallback

fun Cursor.optLong(columnName: String, fallback: Long? = null) =
    this.optLong(getColumnIndex(columnName), fallback)

// endregion

// region String

fun Cursor.getString(columnName: String) = this.getString(getColumnIndexOrThrow(columnName)).orEmpty()

fun Cursor.optString(columnIndex: Int, fallback: String? = null) =
    optNotNull(columnIndex)?.let { getString(it) } ?: fallback

fun Cursor.optString(columnName: String, fallback: String? = null) = this.optString(getColumnIndex(columnName), fallback)

fun Cursor.optStringNN(columnIndex: Int, fallback: String): String =
    optNotNull(columnIndex)?.let { getString(it) } ?: fallback

fun Cursor.optStringNN(columnName: String, fallback: String) = this.optStringNN(getColumnIndex(columnName), fallback)

// endregion