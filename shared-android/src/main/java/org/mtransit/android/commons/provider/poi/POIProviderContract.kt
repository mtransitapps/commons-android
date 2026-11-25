package org.mtransit.android.commons.provider.poi

import android.annotation.SuppressLint
import android.app.SearchManager
import android.database.Cursor
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import androidx.collection.ArrayMap
import androidx.collection.SimpleArrayMap
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.ContentProviderConstants
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import org.mtransit.commons.model.Secret
import org.mtransit.commons.toAny
import org.mtransit.commons.toJsonElement
import java.util.Locale
import kotlin.time.Duration.Companion.days

interface POIProviderContract : ProviderContract {

    companion object {
        const val POI_PATH: String = "poi"

        const val POI_FILTER_EXTRA_AVOID_LOADING: String = "avoidLoading"

        const val POI_FILTER_EXTRA_SORT_ORDER: String = "sortOrder"

        @JvmStatic
        val DEFAULT_POI_MAX_VALIDITY_IN_MS = ProviderContract.MAX_CACHE_VALIDITY_MS

        @JvmStatic
        val DEFAULT_POI_VALIDITY_IN_MS = 1.days.inWholeMilliseconds

        @JvmStatic
        val PROJECTION_POI_ALL_COLUMNS: Array<String>? = null // null = return all columns

        @JvmStatic
        val PROJECTION_POI = arrayOf(
            Columns.T_POI_K_UUID_META,
            Columns.T_POI_K_DST_ID_META,
            Columns.T_POI_K_ID,
            Columns.T_POI_K_NAME,
            Columns.T_POI_K_LAT,
            Columns.T_POI_K_LNG,
            Columns.T_POI_K_ACCESSIBLE,
            Columns.T_POI_K_TYPE,
            Columns.T_POI_K_STATUS_TYPE,
            Columns.T_POI_K_ACTIONS_TYPE,
        )

        @JvmStatic
        val PROJECTION_POI_SEARCH_SUGGEST = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
    }

    val poiMaxValidityInMs: Long get() = DEFAULT_POI_MAX_VALIDITY_IN_MS

    val poiValidityInMs: Long get() = DEFAULT_POI_VALIDITY_IN_MS

    fun getPOI(poiFilter: Filter?): Cursor?

    fun getPOIFromDB(poiFilter: Filter?): Cursor?

    val poiProjectionMap: ArrayMap<String, String>

    val poiProjection: Array<String>

    val pOITable: String

    fun getSearchSuggest(query: String?): Cursor?

    val searchSuggestTable: String?

    val searchSuggestProjectionMap: ArrayMap<String, String>?

    object Columns {
        const val T_POI_K_ID: String = BaseColumns._ID
        const val T_POI_K_UUID_META: String = "uuid"
        const val T_POI_K_DST_ID_META: String = "dst"
        const val T_POI_K_NAME: String = "name"
        const val T_POI_K_LAT: String = "lat"
        const val T_POI_K_LNG: String = "lng"
        const val T_POI_K_ACCESSIBLE: String = "a11y"
        const val T_POI_K_TYPE: String = "type"
        const val T_POI_K_STATUS_TYPE: String = "statustype"
        const val T_POI_K_ACTIONS_TYPE: String = "actionstype"

        const val T_POI_K_SCORE_META_OPT: String = "score" // optional

        @JvmStatic
        @Suppress("unused")
        fun getFkColumnName(key: String) = "fk_$key"
    }

    @Suppress("unused")
    data class Filter
    @Discouraged("use static methods instead")
    constructor(
        override val cacheOnly: Boolean? = null,
        override val cacheValidityInMs: Long? = null,
        override val inFocus: Boolean? = null,
        override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
        val lat: Double? = null,
        val lng: Double? = null,
        val aroundDiff: Double? = null,
        private val minLat: Double? = null,
        private val maxLat: Double? = null,
        private val minLng: Double? = null,
        private val maxLng: Double? = null,
        private val optLoadedMinLat: Double? = null,
        private val optLoadedMaxLat: Double? = null,
        private val optLoadedMinLng: Double? = null,
        private val optLoadedMaxLng: Double? = null,
        private val uuids: Collection<String>? = null,
        private val extras: SimpleArrayMap<String, Any> = SimpleArrayMap(),
        private val sqlSelection: String? = null,
        val searchKeywords: List<String>? = null,
    ) : ProviderContract.Filter(), MTLogK.Loggable {

        companion object {
            private val LOG_TAG = POIProviderContract::class.java.getSimpleName() + ">" + Filter::class.java.getSimpleName()

            fun getNewEmptyFilter() = getNewSqlSelectionFilter("") // need empty SQL selection to be valid

            @SuppressLint("DiscouragedApi")
            fun getNewSqlSelectionFilter(sqlSelection: String) = Filter(sqlSelection = sqlSelection)

            fun getNewSearchFilter(searchKeyword: String) = getNewSearchFilter(listOf(searchKeyword))

            @SuppressLint("DiscouragedApi")
            fun getNewSearchFilter(searchKeywords: List<String>) = Filter(searchKeywords = searchKeywords)

            fun getNewUUIDFilter(uuid: String): Filter {
                return getNewUUIDsFilter(listOf(uuid))
            }

            @SuppressLint("DiscouragedApi")
            fun getNewUUIDsFilter(uuids: Collection<String>) = Filter(uuids = uuids)

            @SuppressLint("DiscouragedApi")
            fun getNewAroundFilter(lat: Double, lng: Double, aroundDiff: Double) = Filter(lat = lat, lng = lng, aroundDiff = aroundDiff)

            @SuppressLint("DiscouragedApi")
            fun getNewAreaFilter(
                minLat: Double,
                maxLat: Double,
                minLng: Double,
                maxLng: Double,
                optLoadedMinLat: Double?,
                optLoadedMaxLat: Double?,
                optLoadedMinLng: Double?,
                optLoadedMaxLng: Double?
            ) = Filter(
                minLat = minLat, maxLat = maxLat, minLng = minLng, maxLng = maxLng,
                optLoadedMinLat = optLoadedMinLat, optLoadedMaxLat = optLoadedMaxLat, optLoadedMinLng = optLoadedMinLng, optLoadedMaxLng = optLoadedMaxLng
            )

            fun isUUIDFilter(poiFilter: Filter?) = poiFilter?.uuids?.isNotEmpty() == true

            fun isAreaFilter(poiFilter: Filter?) = poiFilter?.let { it.lat != null && it.lng != null && it.aroundDiff != null } == true

            fun isAreasFilter(poiFilter: Filter?) = poiFilter?.let { it.minLat != null && it.maxLat != null && it.minLng != null && it.maxLng != null } == true

            @JvmStatic
            fun isSearchKeywords(poiFilter: Filter?) = poiFilter?.searchKeywords?.isNotEmpty() == true

            fun isSQLSelection(poiFilter: Filter?) = poiFilter?.sqlSelection != null

            @JvmStatic
            fun getSearchSelection(searchKeyword: String?, searchableLikeColumns: Array<String>?, searchableEqualColumns: Array<String>?) =
                getSearchSelection(searchKeyword?.let { listOf(searchKeyword) }, searchableLikeColumns, searchableEqualColumns)

            @JvmStatic
            fun getSearchSelection(searchKeywords: List<String>?, searchableLikeColumns: Array<String>?, searchableEqualColumns: Array<String>?) = buildString {
                if (searchKeywords.isNullOrEmpty() || searchKeywords[0].isEmpty()) {
                    throw UnsupportedOperationException("SQL search selection needs at least 1 keyword (${searchKeywords?.size})!")
                }
                if (searchableLikeColumns?.isNotEmpty() != true && searchableEqualColumns?.isNotEmpty() != true) {
                    throw UnsupportedOperationException(
                        "SQL search selection needs at least 1 searchable columns (${searchableLikeColumns?.size}|${searchableEqualColumns?.size})!"
                    )
                }
                searchKeywords
                    .filter { it.isNotEmpty() }
                    .forEach { searchKeyword ->
                        searchKeyword.lowercase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON_REGEX).filter { it.isNotEmpty() }
                            .forEach { keyword ->
                                if (isNotEmpty()) append(SqlUtils.AND)
                                append(SqlUtils.P1)
                                var c = 0
                                c = getSearchSelectionLikeColumns(searchableLikeColumns, keyword, c)
                                @Suppress("AssignedValueIsNeverRead")
                                c = getSearchSelectionEqualColumns(searchableEqualColumns, keyword, c)
                                append(SqlUtils.P2)
                            }
                    }
            }

            private fun StringBuilder.getSearchSelectionEqualColumns(searchableEqualColumns: Array<String>?, keyword: String, c: Int): Int {
                var c = c
                searchableEqualColumns?.forEach { searchableColumn ->
                    if (searchableColumn.isEmpty()) return@forEach
                    if (c > 0) append(SqlUtils.OR)
                    append(SqlUtils.getWhereEqualsString(searchableColumn, keyword))
                    c++
                }
                return c
            }

            private fun StringBuilder.getSearchSelectionLikeColumns(searchableLikeColumns: Array<String>?, keyword: String, c: Int): Int {
                var c = c
                searchableLikeColumns?.forEach { searchableColumn ->
                    if (searchableColumn.isEmpty()) return@forEach
                    if (c > 0) append(SqlUtils.OR)
                    append(SqlUtils.getLikeContains(searchableColumn, keyword))
                    c++
                }
                return c
            }

            @JvmStatic
            fun getSearchSelectionScore(searchKeywords: List<String>?, searchableLikeColumns: Array<String>?, searchableEqualColumns: Array<String>?) =
                buildString {
                    if (searchKeywords.isNullOrEmpty() || searchKeywords[0].isEmpty()) {
                        throw UnsupportedOperationException("SQL search selection score needs at least 1 keyword (${searchKeywords?.size})!")
                    }
                    if (searchableLikeColumns?.isNotEmpty() != true && searchableEqualColumns?.isNotEmpty() != true) {
                        throw UnsupportedOperationException(
                            "SQL search selection score needs at least 1 searchable columns (${searchableLikeColumns?.size}|${searchableEqualColumns?.size})!"
                        )
                    }
                    var c = 0
                    searchKeywords
                        .filter { it.isNotEmpty() }
                        .forEach { searchKeyword ->
                            searchKeyword.lowercase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON_REGEX)
                                .filter { it.isNotEmpty() }
                                .forEach { keyword ->
                                    c = getSearchSelectionLikeColumns(searchableLikeColumns, keyword, c)
                                    c = getSearchSelectionEqualColumns(searchableEqualColumns, keyword, c)
                                }
                        }
                }

            private const val PLUS = " + "

            private fun StringBuilder.getSearchSelectionScoreEqualColumns(searchableEqualColumns: Array<String>?, keyword: String, c: Int): Int {
                var c = c
                searchableEqualColumns?.forEach { searchableColumn ->
                    if (searchableColumn.isEmpty()) return@forEach
                    if (c > 0) append(PLUS)
                    append(SqlUtils.P1).append(SqlUtils.getWhereEqualsString(searchableColumn, keyword)).append(SqlUtils.P2).append("*2")
                    c++
                }
                return c
            }

            private fun StringBuilder.getSearchSelectionScoreLikeColumns(searchableLikeColumns: Array<String>?, keyword: String, c: Int): Int {
                var c = c
                searchableLikeColumns?.forEach { searchableColumn ->
                    if (searchableColumn.isEmpty()) return@forEach
                    if (c > 0) append(PLUS)
                    append(SqlUtils.P1).append(SqlUtils.getLikeContains(searchableColumn, keyword)).append(SqlUtils.P2)
                    c++
                }
                return c
            }

            @JvmStatic
            fun String?.toFilterOrNull() = this?.jsonRunCatching(LOG_TAG) {
                sharedJson.parseToJsonElement(this).jsonObject.toFilter()
            }?.getOrThrow()

            private fun JsonObject.toFilter(): Filter? {
                var lat: Double?
                var lng: Double?
                var aroundDiff: Double?
                var minLat: Double?
                var maxLat: Double?
                var minLng: Double?
                var maxLng: Double?
                var optLoadedMinLat: Double?
                var optLoadedMaxLat: Double?
                var optLoadedMinLng: Double?
                var optLoadedMaxLng: Double?
                try {
                    lat = getValue(JSON_LAT).jsonPrimitive.double
                    lng = getValue(JSON_LNG).jsonPrimitive.double
                    aroundDiff = getValue(JSON_AROUND_DIFF).jsonPrimitive.double
                } catch (_: RuntimeException) { // expected
                    lat = null
                    lng = null
                    aroundDiff = null
                }
                try {
                    minLat = getValue(JSON_MIN_LAT).jsonPrimitive.double
                    maxLat = getValue(JSON_MAX_LAT).jsonPrimitive.double
                    minLng = getValue(JSON_MIN_LNG).jsonPrimitive.double
                    maxLng = getValue(JSON_MAX_LNG).jsonPrimitive.double
                    optLoadedMinLat = this[JSON_OPT_LOADED_MIN_LAT]?.jsonPrimitiveOrNull?.double
                    optLoadedMaxLat = this[JSON_OPT_LOADED_MAX_LAT]?.jsonPrimitiveOrNull?.double
                    optLoadedMinLng = this[JSON_OPT_LOADED_MIN_LNG]?.jsonPrimitiveOrNull?.double
                    optLoadedMaxLng = this[JSON_OPT_LOADED_MAX_LNG]?.jsonPrimitiveOrNull?.double
                } catch (_: RuntimeException) { // expected
                    minLat = null
                    maxLat = null
                    minLng = null
                    maxLng = null
                    optLoadedMinLat = null
                    optLoadedMaxLat = null
                    optLoadedMinLng = null
                    optLoadedMaxLng = null
                }
                //noinspection DiscouragedApi
                return Filter(
                    cacheOnly = getCacheOnlyFromJson(),
                    cacheValidityInMs = getCacheValidityInMsFromJson(),
                    inFocus = getInFocusFromJson(),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                    lat = lat,
                    lng = lng,
                    aroundDiff = aroundDiff,
                    minLat = minLat,
                    maxLat = maxLat,
                    minLng = minLng,
                    maxLng = maxLng,
                    optLoadedMinLat = optLoadedMinLat,
                    optLoadedMaxLat = optLoadedMaxLat,
                    optLoadedMinLng = optLoadedMinLng,
                    optLoadedMaxLng = optLoadedMaxLng,
                    uuids = this[JSON_UUIDS]?.jsonArrayOrNull?.mapNotNull { it.jsonPrimitiveOrNull?.contentOrNull },
                    extras = SimpleArrayMap<String, Any>().apply {
                        this@toFilter[JSON_EXTRAS]?.jsonArrayOrNull?.let { jExtras ->
                            jExtras.forEach { jExtra ->
                                val key = runCatching {
                                    jExtra.jsonObject.getValue(JSON_EXTRAS_KEY).jsonPrimitive.content
                                }.onFailure {
                                    MTLogK.w(LOG_TAG, "Error while parsing extra key JSON object '$jExtra'", it)
                                }.getOrNull() ?: return@forEach
                                val value = jExtra.jsonObject[JSON_EXTRAS_VALUE]?.toAny() ?: return@forEach
                                put(key, value)
                            }
                        }
                    },
                    sqlSelection = this[JSON_SQL_SELECTION]?.jsonPrimitiveOrNull?.contentOrNull,
                    searchKeywords = this[JSON_SEARCH_KEYWORDS]?.jsonArrayOrNull?.mapNotNull { it.jsonPrimitiveOrNull?.contentOrNull },
                ).takeIf {
                    (it.lat != null && it.lng != null && it.aroundDiff != null)
                        || (it.minLat != null && it.maxLat != null && it.minLng != null && it.maxLng != null)
                        || (it.uuids?.isNotEmpty() == true)
                        || (it.searchKeywords?.isNotEmpty() == true)
                        || (it.sqlSelection != null)
                }
            }

            private const val JSON_LAT = "lat"
            private const val JSON_LNG = "lng"
            private const val JSON_AROUND_DIFF = "aroundDiff"
            private const val JSON_MIN_LAT = "minLat"
            private const val JSON_MAX_LAT = "maxLat"
            private const val JSON_MIN_LNG = "minLng"
            private const val JSON_MAX_LNG = "maxLng"
            private const val JSON_OPT_LOADED_MIN_LAT = "optLoadedMinLat"
            private const val JSON_OPT_LOADED_MAX_LAT = "optLoadedMaxLat"
            private const val JSON_OPT_LOADED_MIN_LNG = "optLoadedMinLng"
            private const val JSON_OPT_LOADED_MAX_LNG = "optLoadedMaxLng"
            private const val JSON_UUIDS = "uuids"
            private const val JSON_SEARCH_KEYWORDS = "searchKeywords"
            private const val JSON_SQL_SELECTION = "sqlSelection"
            private const val JSON_EXTRAS = "extras"
            private const val JSON_EXTRAS_KEY = "key"
            private const val JSON_EXTRAS_VALUE = "value"

            @JvmStatic
            fun Filter.toJson() = jsonRunCatching {
                buildJsonObject {
                    if (isAreaFilter(this@toJson)) {
                        put(JSON_LAT, this@toJson.lat)
                        put(JSON_LNG, this@toJson.lng)
                        put(JSON_AROUND_DIFF, this@toJson.aroundDiff)
                    } else if (isAreasFilter(this@toJson)) {
                        put(JSON_MIN_LAT, this@toJson.minLat)
                        put(JSON_MAX_LAT, this@toJson.maxLat)
                        put(JSON_MIN_LNG, this@toJson.minLng)
                        put(JSON_MAX_LNG, this@toJson.maxLng)
                        this@toJson.optLoadedMinLat?.let { put(JSON_OPT_LOADED_MIN_LAT, it) }
                        this@toJson.optLoadedMaxLat?.let { put(JSON_OPT_LOADED_MAX_LAT, it) }
                        this@toJson.optLoadedMinLng?.let { put(JSON_OPT_LOADED_MIN_LNG, it) }
                        this@toJson.optLoadedMaxLng?.let { put(JSON_OPT_LOADED_MAX_LNG, it) }
                    } else if (isUUIDFilter(this@toJson) && this@toJson.uuids != null) {
                        putJsonArray(JSON_UUIDS) { addAll(this@toJson.uuids) }
                    } else if (isSearchKeywords(this@toJson) && this@toJson.searchKeywords != null) {
                        putJsonArray(JSON_SEARCH_KEYWORDS) { addAll(this@toJson.searchKeywords) }
                    } else if (isSQLSelection(this@toJson)) {
                        put(JSON_SQL_SELECTION, this@toJson.sqlSelection)
                    } else {
                        error("Empty POI filter '${this@toJson}' converted to JSON!")
                    }
                    toJsonCommon(this@toJson)
                    putJsonArray(JSON_EXTRAS) {
                        for (i in 0..<this@toJson.extras.size()) {
                            val key = this@toJson.extras.keyAt(i)
                            val jsonElement = runCatching {
                                this@toJson.extras.valueAt(i).toJsonElement()
                            }.onFailure {
                                MTLogK.w(LOG_TAG, "Error while parsing '$key' JSON object '${this@toJson}'", it.cause)
                            }.getOrNull() ?: continue
                            add(
                                buildJsonObject {
                                    put(JSON_EXTRAS_KEY, key)
                                    put(JSON_EXTRAS_VALUE, jsonElement)
                                }
                            )
                        }
                    }
                }
            }.getOrThrow()
        }

        override fun getLogTag() = LOG_TAG

        override fun toString(): String {
            val sb = StringBuilder(Filter::class.java.getSimpleName()).append('[')
            if (isAreaFilter(this)) {
                sb.append("lat:").append(this.lat).append(',')
                sb.append("lng:").append(this.lng).append(',')
                sb.append("aroundDiff:").append(this.aroundDiff).append(',')
            } else if (isAreasFilter(this)) {
                sb.append("minLat:").append(this.minLat).append(',')
                sb.append("maxLat:").append(this.maxLat).append(',')
                sb.append("minLng:").append(this.minLng).append(',')
                sb.append("maxLng:").append(this.maxLng).append(',')
                if (optLoadedMinLat != null) sb.append("optLoadedMinLat:").append(this.optLoadedMinLat).append(',')
                if (optLoadedMaxLat != null) sb.append("optLoadedMaxLat:").append(this.optLoadedMaxLat).append(',')
                if (optLoadedMinLng != null) sb.append("optLoadedMinLng:").append(this.optLoadedMinLng).append(',')
                if (optLoadedMaxLng != null) sb.append("optLoadedMaxLng:").append(this.optLoadedMaxLng).append(',')
            } else if (isUUIDFilter(this)) {
                sb.append("uuids:").append(this.uuids).append(',')
            } else if (isSearchKeywords(this)) {
                sb.append("searchKeywords:").append(this.searchKeywords).append(',')
            } else if (isSQLSelection(this)) {
                sb.append("sqlSelection:").append(this.sqlSelection).append(',')
            }
            sb.append("extras:").append(this.extras).append(',')
            sb.append(super.toStringParts())
            sb.append(']')
            return sb.toString()
        }

        fun getSqlSelection(
            uuidTableColumn: String,
            latTableColumn: String,
            lngTableColumn: String,
            searchableLikeColumns: Array<String>,
            searchableEqualColumns: Array<String>
        ): String? {
            if (isAreaFilter(this) && this.lat != null && this.lng != null && this.aroundDiff != null) {
                return LocationUtils.genAroundWhere(this.lat, this.lng, latTableColumn, lngTableColumn, this.aroundDiff)
            } else if (isAreasFilter(this)) {
                val sb = StringBuilder()
                if (this.minLat != null && this.maxLat != null && this.minLng != null && this.maxLng != null) {
                    sb.append(SqlUtils.P1)
                    sb.append(SqlUtils.getBetween(latTableColumn, this.minLat, this.maxLat))
                    sb.append(SqlUtils.AND)
                    sb.append(SqlUtils.getBetween(lngTableColumn, this.minLng, this.maxLng))
                    sb.append(SqlUtils.P2)
                }
                if (this.optLoadedMinLat != null && this.optLoadedMaxLat != null && this.optLoadedMinLng != null && this.optLoadedMaxLng != null) {
                    if (sb.isNotEmpty()) sb.append(SqlUtils.AND)
                    sb.append(SqlUtils.NOT)
                    sb.append(SqlUtils.P1)
                    sb.append(SqlUtils.getBetween(latTableColumn, this.optLoadedMinLat, this.optLoadedMaxLat))
                    sb.append(SqlUtils.AND)
                    sb.append(SqlUtils.getBetween(lngTableColumn, this.optLoadedMinLng, this.optLoadedMaxLng))
                    sb.append(SqlUtils.P2)
                }
                return sb.toString()
            } else if (isUUIDFilter(this)) {
                return SqlUtils.getWhereInString(uuidTableColumn, this.uuids)
            } else if (isSearchKeywords(this) && searchKeywords != null) {
                return getSearchSelection(this.searchKeywords, searchableLikeColumns, searchableEqualColumns)
            } else if (isSQLSelection(this)) {
                return this.sqlSelection
            } else {
                throw UnsupportedOperationException("SQL selection impossible!")
            }
        }

        override fun toJsonString() = this.toJson().toString()

        fun getExtraBoolean(key: String, defaultValue: Boolean): Boolean {
            val value = this.extras[key] ?: return defaultValue
            return value as Boolean
        }

        fun getExtraString(key: String, defaultValue: String?): String? {
            val value = this.extras[key] ?: return defaultValue
            return value as String
        }

        fun getExtraDouble(key: String, defaultValue: Double?): Double? {
            val value = this.extras[key] ?: return defaultValue
            return value as Double
        }
    }
}
