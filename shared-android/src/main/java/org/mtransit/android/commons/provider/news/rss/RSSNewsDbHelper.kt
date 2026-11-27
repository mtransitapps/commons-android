package org.mtransit.android.commons.provider.news.rss

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.provider.news.NewsProvider.NewsDbHelper

class RSSNewsDbHelper @JvmOverloads constructor(
    private val context: Context,
    private val storage: RssStorage,
    private val dbNamePrefix: String? = null,
    private val dbName: String = dbNamePrefix?.let { "${it}_$DB_NAME" } ?: DB_NAME,
    dbVersion: Int = getDbVersion(context),
) : NewsDbHelper(context, dbName, dbVersion) {

    companion object {
        private val LOG_TAG: String = RSSNewsDbHelper::class.java.simpleName

        /**
         * Override if multiple [RSSNewsDbHelper] implementations in same app.
         */
        private const val DB_NAME = "rss.db"

        private const val T_RSS_NEWS = T_NEWS

        private val T_RSS_NEWS_SQL_CREATE = getSqlCreateBuilder(T_RSS_NEWS).build()

        private val T_RSS_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RSS_NEWS)

        private var dbVersion = -1

        /**
         * Override if multiple [RSSNewsDbHelper] in same app.
         */
        fun getDbVersion(context: Context): Int {
            if (dbVersion < 0) {
                dbVersion = context.resources.getInteger(R.integer.rss_db_version)
                dbVersion++ // add news articles images URLs do DB -> FORCE DB update
            }
            return dbVersion
        }
    }

    override fun getLogTag() = LOG_TAG

    override fun getDbName() = dbName

    override fun onCreateMT(db: SQLiteDatabase) {
        initAllDbTables(db)
    }

    override fun onUpgradeMT(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(T_RSS_NEWS_SQL_DROP)
        initAllDbTables(db)
    }

    private fun initAllDbTables(db: SQLiteDatabase) {
        db.execSQL(T_RSS_NEWS_SQL_CREATE)
        storage.saveLastUpdate(null, null)
    }
}
