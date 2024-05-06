package org.mtransit.android.commons.provider.gbfs.data.api

import android.os.Build
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.mtransit.commons.CommonsApp
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// https://gbfs.org/specification/reference/#field-types
// 2023-07-17 | yyyy-MM-dd
// 2023-07-17T13:34:13+02:00 | yyyy-MM-dd'T'HH:mm:ss:XXX
class GBFSDateAdapter : JsonDeserializer<Date?> {

    companion object {
        // ISO 8601
        private const val DATE_FORMAT: String = "yyyy-MM-dd"
        private val DATE_FORMATTER = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)

        // (added in v2.3)-
        // ISO 8601 notation
        private val DATE_TIME_FORMAT: String =
            if (CommonsApp.isAndroid == false || Build.VERSION.SDK_INT > Build.VERSION_CODES.N) "yyyy-MM-dd'T'HH:mm:ssXXX" else
                "yyyy-MM-dd'T'HH:mm:ssZZZZZ" // 'X' only supported API Level 24+ #ISO_8601

        private val DATE_TIME_FORMATTER = SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        json ?: return null
        if (json.asString.length == DATE_FORMAT.length) {
            try {
                return DATE_FORMATTER.parse(json.asString)
            } catch (ignore: ParseException) {
                ignore.printStackTrace()
            }
        } else {
            try {
                return DATE_TIME_FORMATTER.parse(json.asString)
            } catch (ignore: ParseException) {
                ignore.printStackTrace()
            }
        }
        throw JsonParseException("DateParseException: $json")
    }
}