package org.mtransit.android.commons

import android.net.Uri
import androidx.core.net.toUri

fun buildReferrer(
    campaignSource: String? = null,
    campaignMedium: String? = null,
    campaignTerm: String? = null,
    campaignContent: String? = null,
    campaignName: String? = null,
): String? = Uri.Builder().apply {
    campaignSource?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("utm_source", it) }
    campaignMedium?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("utm_medium", it) }
    campaignTerm?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("utm_term", it) }
    campaignContent?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("utm_content", it) }
    campaignName?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("utm_campaign", it) }
}.build().encodedQuery?.takeIf { it.isNotEmpty() }

fun String.appendReferrer(referrer: String?): Uri =
    toUri().buildUpon()
        .apply {
            referrer?.let { appendQueryParameter("referrer", it) }
        }.build()
