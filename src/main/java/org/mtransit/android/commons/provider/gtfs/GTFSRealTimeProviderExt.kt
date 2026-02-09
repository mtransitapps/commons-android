package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import okhttp3.Request
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Stop
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.MT_HASH_SECRET_AND_DATE
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAGENCY_URL_HEADER_NAMES
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAGENCY_URL_HEADER_VALUES
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAGENCY_URL_TOKEN
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.isUSE_URL_HASH_SECRET_AND_DATE
import java.net.URL

val GTFSRealTimeProvider.routeIdCleanupPattern get() = getRouteIdCleanupPattern(requireContextCompat())
val GTFSRealTimeProvider.tripIdCleanupPattern get() = getTripIdCleanupPattern(requireContextCompat())
val GTFSRealTimeProvider.stopIdCleanupPattern get() = getStopIdCleanupPattern(requireContextCompat())

val GTFSRealTimeProvider.agencyTag get() = getAgencyTag(requireContextCompat())

fun Stop.getStopTag(provider: GTFSRealTimeProvider) = provider.getStopTag(this)

fun Route.getRouteTypeTag(provider: GTFSRealTimeProvider) = provider.getRouteTypeTag(this)
fun Route.getRouteTag(provider: GTFSRealTimeProvider) = provider.getRouteTag(this)
fun Direction.getDirectionTag(provider: GTFSRealTimeProvider) = provider.getDirectionTag(this)

fun RouteDirection.getRouteTypeTag(provider: GTFSRealTimeProvider) = this.route.getRouteTypeTag(provider)
fun RouteDirection.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
fun RouteDirection.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)

fun RouteDirectionStop.getRouteTypeTag(provider: GTFSRealTimeProvider) = this.route.getRouteTypeTag(provider) ?: this.dataSourceTypeId
fun RouteDirectionStop.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
fun RouteDirectionStop.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)
fun RouteDirectionStop.getStopTag(provider: GTFSRealTimeProvider) = this.stop.getStopTag(provider)

fun RouteDirectionStop.getTargetUUIDs(
    provider: GTFSRealTimeProvider,
    includeAgencyTag: Boolean = false,
    includeRouteType: Boolean = false,
    includeStopTags: Boolean = false
) = buildMap {
    if (includeAgencyTag) put(getAgencyTagTargetUUID(provider.agencyTag), authority)
    if (includeRouteType) getAgencyRouteTypeTagTargetUUID(provider.agencyTag, getRouteTypeTag(provider))?.let { put(it, authority) }
    put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.uuid)
    getAgencyRouteDirectionTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider))?.let { put(it, routeDirectionUUID) }
    if (includeStopTags) {
        getAgencyRouteDirectionStopTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider), getStopTag(provider))?.let {
            put(it, uuid)
        }
        put(getAgencyStopTagTargetUUID(provider.agencyTag, getStopTag(provider)), uuid)
        put(getAgencyRouteStopTagTargetUUID(provider.agencyTag, getRouteTag(provider), getStopTag(provider)), uuid)
    }
}

fun RouteDirection.getTargetUUIDs(provider: GTFSRealTimeProvider, includeAgencyTag: Boolean = false, includeRouteType: Boolean = false) = buildMap {
    if (includeAgencyTag) put(getAgencyTagTargetUUID(provider.agencyTag), authority)
    if (includeRouteType) getAgencyRouteTypeTagTargetUUID(provider.agencyTag, getRouteTypeTag(provider))?.let { put(it, authority) }
    put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.uuid)
    getAgencyRouteDirectionTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider))?.let { put(it, uuid) }
}

fun Route.getTargetUUIDs(provider: GTFSRealTimeProvider, includeAgencyTag: Boolean = false, includeRouteType: Boolean = false) = buildMap {
    if (includeAgencyTag) put(getAgencyTagTargetUUID(provider.agencyTag), authority)
    if (includeRouteType) getAgencyRouteTypeTagTargetUUID(provider.agencyTag, getRouteTypeTag(provider))?.let { put(it, authority) }
    put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), uuid)
}

fun GTFSRealTimeProvider.makeRequest(context: Context, urlCachedString: String = "", getUrlString: (token: String) -> String): Request? {
    if (urlCachedString.isNotBlank()) {
        MTLog.i(this, "Loading from cached API (length: %d) '***'...", urlCachedString.length)
        return Request.Builder().url(URL(urlCachedString)).build()
    }
    val token = getAGENCY_URL_TOKEN(context) // use local token 1st for new/updated API URL & tokens
        .takeIf { it.isNotBlank() } ?: this.providedAgencyUrlToken
    ?: "" // compat w/ API w/o token
    var urlString = getUrlString(token)
    if (isUSE_URL_HASH_SECRET_AND_DATE(context)) {
        getHashSecretAndDate(context)?.let { hash ->
            urlString = urlString.replace(MT_HASH_SECRET_AND_DATE.toRegex(), hash.trim())
        }
    }
    if (urlString.isBlank()) {
        MTLog.w(this, "No valid URL!")
        return null
    }
    val url = URL(urlString)
    MTLog.i(this, "Loading from '%s'...", url.host)
    MTLog.d(this, "Using token '%s' (length: %d)", if (token.isEmpty()) "(none)" else "***", token.length)
    return Request.Builder()
        .url(url)
        .apply {
            val agencyUrlHeaderNames = getAGENCY_URL_HEADER_NAMES(context)
            val agencyUrlHeaderValues = getAGENCY_URL_HEADER_VALUES(context)
            if (agencyUrlHeaderNames.size != agencyUrlHeaderValues.size) return@apply
            for (i in agencyUrlHeaderNames.indices) {
                addHeader(agencyUrlHeaderNames[i], agencyUrlHeaderValues[i])
            }
        }.build()
}
