package org.mtransit.android.commons.provider.common

import kotlinx.serialization.json.Json

// Shared json configured for JSON serialization between main app and agency modules
val sharedJson = Json {
    // decoding only
    allowTrailingComma = true // do not fail on trailing comma
    coerceInputValues = true // Falls back to default Kotlin values if org.json outputs null or mismatched types
    ignoreUnknownKeys = true // new keys might be added/updated between main app and agency module app releases
    // encoding only
    encodeDefaults = true // default values might change between main app and agency module app releases
    // parsing & encoding
    isLenient = true // allows unquoted strings or malformed formatting (if legacy org.json writers were used)
}
