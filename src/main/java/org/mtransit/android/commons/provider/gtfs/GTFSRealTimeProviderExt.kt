package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import okhttp3.Request
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import java.net.URL

fun GTFSRealTimeProvider.makeRequest(context: Context, urlCachedString: String = "", getUrlString: (token: String) -> String): Request? {
    if (urlCachedString.isNotBlank()) {
        MTLog.i(this, "Loading from cached API (length: %d) '***'...", urlCachedString.length)
        return Request.Builder().url(URL(urlCachedString)).build()
    }
    val token = GTFSRealTimeProvider.getAGENCY_URL_TOKEN(context) // use local token 1st for new/updated API URL & tokens
        .takeIf { it.isNotBlank() } ?: this.providedAgencyUrlToken
    ?: "" // compat w/ API w/o token
    var urlString = getUrlString(token)
    if (GTFSRealTimeProvider.isUSE_URL_HASH_SECRET_AND_DATE(context)) {
        getHashSecretAndDate(context)?.let { hash ->
            urlString = urlString.replace(GTFSRealTimeProvider.MT_HASH_SECRET_AND_DATE.toRegex(), hash.trim())
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
            val agencyUrlHeaderNames = GTFSRealTimeProvider.getAGENCY_URL_HEADER_NAMES(context)
            val agencyUrlHeaderValues = GTFSRealTimeProvider.getAGENCY_URL_HEADER_VALUES(context)
            if (agencyUrlHeaderNames.size != agencyUrlHeaderValues.size) return@apply
            for (i in agencyUrlHeaderNames.indices) {
                addHeader(agencyUrlHeaderNames[i], agencyUrlHeaderValues[i])
            }
        }.build()
}
