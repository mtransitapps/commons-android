package org.mtransit.android.commons.provider.poi

import android.annotation.SuppressLint
import android.app.SearchManager
import android.database.Cursor
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import androidx.collection.ArrayMap
import androidx.collection.SimpleArrayMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.optDouble
import org.mtransit.android.commons.provider.common.ContentProviderConstants
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.commons.model.Secret
import java.util.Locale

interface POIProviderContract : ProviderContract {

    companion object {
        const val POI_PATH: String = "poi"

        const val POI_FILTER_EXTRA_AVOID_LOADING: String = "avoidLoading"

        const val POI_FILTER_EXTRA_SORT_ORDER: String = "sortOrder"

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

    val poiMaxValidityInMs: Long

    val poiValidityInMs: Long

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
    data class Filter @Discouraged("use static methods instead") constructor(
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
    ) : ProviderContract.Filter(), MTLog.Loggable {

        companion object {
            private val LOG_TAG = POIProviderContract::class.java.getSimpleName() + ">" + Filter::class.java.getSimpleName()

            @SuppressLint("DiscouragedApi")
            fun getNewEmptyFilter() = Filter()

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
                minLat: Double, maxLat: Double, minLng: Double, maxLng: Double,
                optLoadedMinLat: Double?, optLoadedMaxLat: Double?, optLoadedMinLng: Double?, optLoadedMaxLng: Double?
            ) = Filter(
                minLat = minLat, maxLat =  maxLat, minLng =  minLng, maxLng =  maxLng,
                optLoadedMinLat =  optLoadedMinLat, optLoadedMaxLat =  optLoadedMaxLat, optLoadedMinLng =  optLoadedMinLng, optLoadedMaxLng =  optLoadedMaxLng
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
                    throw UnsupportedOperationException("SQL search selection needs at least 1 searchable columns (${searchableLikeColumns?.size}|${searchableEqualColumns?.size})!")
                }
                for (searchKeyword in searchKeywords) {
                    if (searchKeyword.isEmpty()) continue
                    val keywords =
                        searchKeyword.lowercase().split(ContentProviderConstants.SEARCH_SPLIT_ON.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (keyword in keywords) {
                        if (keyword.isEmpty()) continue
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
                        throw UnsupportedOperationException("SQL search selection score needs at least 1 searchable columns (${searchableLikeColumns?.size}|${searchableEqualColumns?.size})!")
                    }
                    var c = 0
                    for (searchKeyword in searchKeywords) {
                        if (searchKeyword.isEmpty()) continue
                        val keywords = searchKeyword.lowercase(Locale.ENGLISH).split(ContentProviderConstants.SEARCH_SPLIT_ON.toRegex())
                        for (keyword in keywords) {
                            if (searchKeyword.isEmpty()) continue
                            c = getSearchSelectionScoreLikeColumns(searchableLikeColumns, keyword, c)
                            c = getSearchSelectionScoreEqualColumns(searchableEqualColumns, keyword, c)
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
            fun fromJSONString(jsonString: String?): Filter? {
                try {
                    return jsonString?.let { fromJSON(JSONObject(it)) }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '$jsonString'")
                    return null
                }
            }

            private fun fromJSON(json: JSONObject): Filter? {
                try {
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
                        lat = json.getDouble(JSON_LAT)
                        lng = json.getDouble(JSON_LNG)
                        aroundDiff = json.getDouble(JSON_AROUND_DIFF)
                    } catch (jsone: JSONException) {
                        lat = null
                        lng = null
                        aroundDiff = null
                    }
                    try {
                        minLat = json.getDouble(JSON_MIN_LAT)
                        maxLat = json.getDouble(JSON_MAX_LAT)
                        minLng = json.getDouble(JSON_MIN_LNG)
                        maxLng = json.getDouble(JSON_MAX_LNG)
                        optLoadedMinLat = json.optDouble(JSON_OPT_LOADED_MIN_LAT, null)
                        optLoadedMaxLat = json.optDouble(JSON_OPT_LOADED_MAX_LAT, null)
                        optLoadedMinLng = json.optDouble(JSON_OPT_LOADED_MIN_LNG, null)
                        optLoadedMaxLng = json.optDouble(JSON_OPT_LOADED_MAX_LNG, null)
                    } catch (jsone: JSONException) {
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
                        cacheOnly = getCacheOnlyFromJSON(json),
                        cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                        inFocus = getInFocusFromJSON(json),
                        providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
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
                        uuids = json.optJSONArray(JSON_UUIDS)?.let { jUUIDs ->
                            buildList {
                                for (i in 0..<jUUIDs.length()) {
                                    add(jUUIDs.getString(i))
                                }
                            }
                        },
                        extras = json.getJSONArray(JSON_EXTRAS).let { jExtras ->
                            val extras = SimpleArrayMap<String, Any>()
                            for (i in 0..<jExtras.length()) {
                                val jExtra = jExtras.getJSONObject(i)
                                val key = jExtra.getString(JSON_EXTRAS_KEY)
                                val value = jExtra.get(JSON_EXTRAS_VALUE)
                                extras.put(key, value)
                            }
                            extras
                        },
                        sqlSelection = json.optString(JSON_SQL_SELECTION),
                        searchKeywords = json.optJSONArray(JSON_SEARCH_KEYWORDS)?.let { jSearchKeywords ->
                            buildList {
                                for (i in 0..<jSearchKeywords.length()) {
                                    add(jSearchKeywords.getString(i))
                                }
                            }
                        },
                    ).takeIf {
                        (it.lat != null && it.lng != null && it.aroundDiff != null)
                                || (it.minLat != null && it.maxLat != null && it.minLng != null && it.maxLng != null)
                                || (it.uuids?.isNotEmpty() == true)
                                || (it.searchKeywords?.isNotEmpty() == true)
                                || (it.sqlSelection != null)
                    }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '$json'")
                    return null
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

            private fun toJSONString(statusFilter: Filter) = toJSON(statusFilter)?.toString()

            @JvmStatic
            fun toJSON(poiFilter: Filter?): JSONObject? {
                poiFilter ?: return null
                try {
                    val json = JSONObject()
                    if (isAreaFilter(poiFilter)) {
                        json.put(JSON_LAT, poiFilter.lat)
                        json.put(JSON_LNG, poiFilter.lng)
                        json.put(JSON_AROUND_DIFF, poiFilter.aroundDiff)
                    } else if (isAreasFilter(poiFilter)) {
                        json.put(JSON_MIN_LAT, poiFilter.minLat)
                        json.put(JSON_MAX_LAT, poiFilter.maxLat)
                        json.put(JSON_MIN_LNG, poiFilter.minLng)
                        json.put(JSON_MAX_LNG, poiFilter.maxLng)
                        if (poiFilter.optLoadedMinLat != null) {
                            json.put(JSON_OPT_LOADED_MIN_LAT, poiFilter.optLoadedMinLat)
                        }
                        if (poiFilter.optLoadedMaxLat != null) {
                            json.put(JSON_OPT_LOADED_MAX_LAT, poiFilter.optLoadedMaxLat)
                        }
                        if (poiFilter.optLoadedMinLng != null) {
                            json.put(JSON_OPT_LOADED_MIN_LNG, poiFilter.optLoadedMinLng)
                        }
                        if (poiFilter.optLoadedMaxLng != null) {
                            json.put(JSON_OPT_LOADED_MAX_LNG, poiFilter.optLoadedMaxLng)
                        }
                    } else if (isUUIDFilter(poiFilter) && poiFilter.uuids != null) {
                        val jUUIDs = JSONArray()
                        for (uuid in poiFilter.uuids) {
                            jUUIDs.put(uuid)
                        }
                        json.put(JSON_UUIDS, jUUIDs)
                    } else if (isSearchKeywords(poiFilter) && poiFilter.searchKeywords != null) {
                        val jSearchKeywords = JSONArray()
                        for (searchKeyword in poiFilter.searchKeywords) {
                            jSearchKeywords.put(searchKeyword)
                        }
                        json.put(JSON_SEARCH_KEYWORDS, jSearchKeywords)
                    } else if (isSQLSelection(poiFilter)) {
                        json.put(JSON_SQL_SELECTION, poiFilter.sqlSelection)
                    } else {
                        MTLog.w(LOG_TAG, "Empty POI filter '$poiFilter' converted to JSON!")
                    }
                    val jExtras = JSONArray()
                    toJSON(poiFilter, json)
                    for (i in 0..<poiFilter.extras.size()) {
                        val jExtra = JSONObject()
                        jExtra.put(JSON_EXTRAS_KEY, poiFilter.extras.keyAt(i))
                        jExtra.put(JSON_EXTRAS_VALUE, poiFilter.extras.valueAt(i))
                        jExtras.put(jExtra)
                    }
                    json.put(JSON_EXTRAS, jExtras)
                    return json
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '$poiFilter'")
                    return null
                }
            }
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

        fun addExtra(key: String, value: Any) {
            // TODO CRASH SimpleArrayMap ClassCastException: String cannot be cast to Object[]
            this.extras.put(key, value)
        }

        fun getSqlSelection(
            uuidTableColumn: String, latTableColumn: String,
            lngTableColumn: String, searchableLikeColumns: Array<String>,
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

        override fun toJSONString() = toJSONString(this)

        fun getExtraBoolean(key: String, defaultValue: Boolean): Boolean {
            val value = this.extras.get(key) ?: return defaultValue
            return value as Boolean
        }

        fun getExtraString(key: String, defaultValue: String?): String? {
            val value = this.extras.get(key) ?: return defaultValue
            return value as String
        }

        fun getExtraDouble(key: String, defaultValue: Double?): Double? {
            val value = this.extras.get(key) ?: return defaultValue
            return value as Double
        }
    }
}
