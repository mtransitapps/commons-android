package org.mtransit.android.commons.provider.news

import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.collection.ArrayMap
import org.mtransit.android.commons.provider.common.ContentProviderConstants
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.ProviderConfigContract
import org.mtransit.commons.FeatureFlags

abstract class ConfigurableNewsProvider : NewsProvider(), ProviderConfigContract {

    @Volatile
    protected var importedProviderConfig: ProviderConfig? = null
        private set

    private val uriMatcher: UriMatcher by lazy {
        getNewUriMatcher(authority).also {
            @Suppress("SimplifyBooleanWithConstants")
            if (FeatureFlags.F_EXPORT_PROVIDER_CONFIG || importedProviderConfig != null) {
                ProviderConfigContract.append(it, authority)
            }
        }
    }

    override val URI_MATCHER: UriMatcher get() = uriMatcher

    override val newsProjectionMap: ArrayMap<String, String> get() = getNewNewsProjectionMap(authority)

    fun configureProviderConfig(config: ProviderConfig) {
        check(importedProviderConfig == null) { "Provider runtime already configured" }
        require(config.schemaVersion == ProviderConfig.SCHEMA_VERSION) { "Unsupported provider config version" }
        require(config.targetAuthority.isNotBlank()) { "Missing news config target authority" }
        configureNewsConfig(config)
        importedProviderConfig = config
    }

    protected abstract fun configureNewsConfig(config: ProviderConfig)

    private var dbHelper: NewsDbHelper? = null

    fun getDBHelper(): NewsDbHelper =
        dbHelper ?: getNewDbHelper(requireContextCompat()).also { dbHelper = it }

    override val readDB: SQLiteDatabase get() = getDBHelper().readableDatabase

    override val writeDB: SQLiteDatabase get() = getDBHelper().writableDatabase

    open fun closeConfiguredRuntime() {
        dbHelper?.close()
        dbHelper = null
    }

    override fun queryMT(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (URI_MATCHER.match(uri) == ContentProviderConstants.CONFIG) {
            return getProviderConfig().toCursor()
        }
        return super.queryMT(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun getTypeMT(uri: Uri): String? {
        if (URI_MATCHER.match(uri) == ContentProviderConstants.CONFIG) {
            return ProviderConfigContract.MIME_TYPE
        }
        return super.getTypeMT(uri)
    }
}
