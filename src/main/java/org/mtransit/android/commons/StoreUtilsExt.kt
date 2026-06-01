package org.mtransit.android.commons

import androidx.core.net.toUri

internal fun makeUrl(
    url: String,
    campaignSource: String?,
    campaignMedium: String?,
    campaignTerm: String?,
    campaignContent: String?,
    campaignName: String?,
) = url.toUri().buildUpon()
    .apply {
        buildList {
            campaignSource?.let { add("utm_source=$it") }
            campaignMedium?.let { add("utm_medium=$it") }
            campaignTerm?.let { add("utm_term=$it") }
            campaignContent?.let { add("utm_content=$it") }
            campaignName?.let { add("utm_campaign=$it") }
        }.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "&")
            ?.let { referrer ->
                appendQueryParameter("referrer", referrer)
            }
    }.build()
