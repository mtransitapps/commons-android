package org.mtransit.android.commons

import android.net.Uri
import androidx.core.net.toUri

internal fun String.appendReferrerCampaign(
    campaignSource: String? = null,
    campaignMedium: String? = null,
    campaignTerm: String? = null,
    campaignContent: String? = null,
    campaignName: String? = null,
) = toUri().buildUpon()
    .apply {
        buildList {
            campaignSource?.takeIf { it.isNotBlank() }?.let { add("utm_source=${Uri.encode(it)}") }
            campaignMedium?.takeIf { it.isNotBlank() }?.let { add("utm_medium=${Uri.encode(it)}") }
            campaignTerm?.takeIf { it.isNotBlank() }?.let { add("utm_term=${Uri.encode(it)}") }
            campaignContent?.takeIf { it.isNotBlank() }?.let { add("utm_content=${Uri.encode(it)}") }
            campaignName?.takeIf { it.isNotBlank() }?.let { add("utm_campaign=${Uri.encode(it)}") }
        }.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "&")
            ?.let { referrer ->
                appendQueryParameter("referrer", referrer)
            }
    }.build()
