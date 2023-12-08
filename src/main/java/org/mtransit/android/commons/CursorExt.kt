package org.mtransit.android.commons

import android.database.Cursor
import org.mtransit.commons.sql.fromSQL

fun Cursor.getBoolean(columnName: String) = this.getInt(getColumnIndexOrThrow(columnName)).fromSQL()

fun Cursor.getDouble(columnName: String) = this.getDouble(getColumnIndexOrThrow(columnName))

fun Cursor.optInt(columnIndex: Int, fallback: Int?): Int? {
    return columnIndex.takeIf { it >= 0 }?.let { getInt(it) } ?: fallback
}

fun Cursor.optInt(columnName: String, fallback: Int?) = this.optInt(getColumnIndex(columnName), fallback)

fun Cursor.optIntNN(columnIndex: Int, fallback: Int): Int {
    return columnIndex.takeIf { it >= 0 }?.let { getInt(it) } ?: fallback
}

fun Cursor.optIntNN(columnName: String, fallback: Int) = this.optIntNN(getColumnIndex(columnName), fallback)

fun Cursor.getInt(columnName: String) = this.getInt(getColumnIndexOrThrow(columnName))

fun Cursor.getLong(columnName: String) = this.getLong(getColumnIndexOrThrow(columnName))

fun Cursor.getString(columnName: String) = this.getString(getColumnIndexOrThrow(columnName)).orEmpty()