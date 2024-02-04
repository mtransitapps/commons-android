package org.mtransit.android.commons

import android.database.Cursor
import org.mtransit.commons.sql.fromSQL

fun Cursor.optNotNull(columnIndex: Int) = columnIndex.takeIf { it >= 0 }?.takeIf { isNull(it) }

// region Boolean

fun Cursor.getBoolean(columnName: String) = this.getInt(getColumnIndexOrThrow(columnName)).fromSQL()

// endregion

// region Double

fun Cursor.getDouble(columnName: String) = this.getDouble(getColumnIndexOrThrow(columnName))

// endregion

// region Int

fun Cursor.optInt(columnIndex: Int, fallback: Int? = null): Int? {
    return optNotNull(columnIndex)?.let { getInt(it) } ?: fallback
}

fun Cursor.optInt(columnName: String, fallback: Int? = null) = this.optInt(getColumnIndex(columnName), fallback)

fun Cursor.optIntNN(columnIndex: Int, fallback: Int): Int {
    return optNotNull(columnIndex)?.let { getInt(it) } ?: fallback
}

fun Cursor.optIntNN(columnName: String, fallback: Int) = this.optIntNN(getColumnIndex(columnName), fallback)

fun Cursor.getInt(columnName: String) = this.getInt(getColumnIndexOrThrow(columnName))

// endregion

// region Long

fun Cursor.getLong(columnName: String) = this.getLong(getColumnIndexOrThrow(columnName))

// endregion

// region String

fun Cursor.getString(columnName: String) = this.getString(getColumnIndexOrThrow(columnName)).orEmpty()

fun Cursor.optString(columnIndex: Int, fallback: String? = null): String? {
    return optNotNull(columnIndex)?.let { getString(it) } ?: fallback
}

fun Cursor.optString(columnName: String, fallback: String? = null) = this.optString(getColumnIndex(columnName), fallback)

fun Cursor.optStringNN(columnIndex: Int, fallback: String): String {
    return optNotNull(columnIndex)?.let { getString(it) } ?: fallback
}

fun Cursor.optStringNN(columnName: String, fallback: String) = this.optStringNN(getColumnIndex(columnName), fallback)

// endregion