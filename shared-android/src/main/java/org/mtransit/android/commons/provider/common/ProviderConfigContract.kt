package org.mtransit.android.commons.provider.common

import android.content.UriMatcher

interface ProviderConfigContract : ProviderContract {

    companion object {
        const val CONFIG_PATH = "config"
        const val COLUMN_JSON = "config_json"
        const val MIME_TYPE = "vnd.android.cursor.item/vnd.org.mtransit.provider.config"

        fun append(uriMatcher: UriMatcher, authority: String) {
            uriMatcher.addURI(authority, CONFIG_PATH, ContentProviderConstants.CONFIG)
        }
    }

    fun getProviderConfig(): ProviderConfig
}
