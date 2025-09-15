package org.mtransit.android.commons.provider.news.twitter

import android.annotation.SuppressLint
import android.os.Build
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.commons.CommonsApp
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// 2023-07-17T13:34:13+02:00 | yyyy-MM-dd'T'HH:mm:ss:XXX
class TwitterDateAdapter :
    JsonDeserializer<Date?>,
    JsonSerializer<Date?>,
    MTLog.Loggable {

    override fun getLogTag(): String = LOG_TAG

    companion object {
        private val LOG_TAG = TwitterDateAdapter::class.java.simpleName

        // 025-09-02T22:00:01.000Z
        // (added in v2.3)-
        // ISO 8601 notation
        @SuppressLint("ObsoleteSdkInt")
        private val DATE_TIME_FORMAT: String =
            if (CommonsApp.isAndroid == false || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) "yyyy-MM-dd'T'HH:mm:ssXXX" else
                "yyyy-MM-dd'T'HH:mm:ssZZZZZ" // 'X' only supported API Level 24+ #ISO_8601

        private val DATE_TIME_FORMATTER = ThreadSafeDateFormatter(
            SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        )
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        json ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return Date.from(Instant.parse(json.asString))
            } catch (e: DateTimeParseException) {
                MTLog.d(this, e, "Error while parsing ${json.asString} with java.time.Instant!")
            }
            try {
                return Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(json.asString)))
            } catch (e: DateTimeParseException) {
                MTLog.d(this, e, "Error while parsing ${json.asString} with DateTimeFormatter.ISO_INSTANT!")
            }
            try {
                return Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(json.asString)))
            } catch (e: DateTimeParseException) {
                MTLog.d(this, e, "Error while parsing ${json.asString} with DateTimeFormatter.ISO_DATE_TIME!")
            }
        }
        try {
            return DATE_TIME_FORMATTER.parseThreadSafe(json.asString)
        } catch (e: ParseException) {
            MTLog.d(this, e, "Error while parsing ${json.asString} with '$DATE_TIME_FORMAT'!")
        }
        MTLog.w(this, "Error while parsing $json!")
        return null
    }

    override fun serialize(
        src: Date?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        src ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(src.toInstant()))
            } catch (e: DateTimeParseException) {
                MTLog.d(this, e, "Error while formatting $src with DateTimeFormatter.ISO_INSTANT!")
            }
            try {
                return JsonPrimitive(DateTimeFormatter.ISO_DATE_TIME.format(src.toInstant()))
            } catch (e: DateTimeParseException) {
                MTLog.d(this, e, "Error while formatting $src with DateTimeFormatter.ISO_DATE_TIME!")
            }
        }
        try {
            return JsonPrimitive(DATE_TIME_FORMATTER.formatThreadSafe(src))
        } catch (e: ParseException) {
            MTLog.d(this, e, "Error while formatting $src with '$DATE_TIME_FORMAT'!")
        }
        MTLog.w(this, "Error while formatting $src!")
        return null
    }
}