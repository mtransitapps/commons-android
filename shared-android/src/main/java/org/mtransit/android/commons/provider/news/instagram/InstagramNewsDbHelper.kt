package org.mtransit.android.commons.provider.news.instagram

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.provider.news.NewsProvider.NewsDbHelper

class InstagramNewsDbHelper(
    private val context: Context,
    private val storage: InstagramStorage,
    private val dbNamePrefix: String? = null,
    private val dbName: String = dbNamePrefix?.let { "${it}_$DB_NAME" } ?: DB_NAME,
    dbVersion: Int = getDbVersion(context)
) : NewsDbHelper(context, dbName, dbVersion) {

    companion object {
        private val LOG_TAG: String = InstagramNewsDbHelper::class.java.simpleName

        /**
         * Override if multiple [InstagramNewsDbHelper] implementations in same app.
         */
        private const val DB_NAME = "news_instagram.db"

        private const val T_INSTAGRAM_NEWS = T_NEWS

        private val T_INSTAGRAM_NEWS_SQL_CREATE = getSqlCreateBuilder(T_INSTAGRAM_NEWS).build()

        private val T_INSTAGRAM_NEWS_SQL_DROP =
            SqlUtils.getSQLDropIfExistsQuery(T_INSTAGRAM_NEWS)

        private var dbVersion = -1

        /**
         * Override if multiple [InstagramNewsDbHelper] in same app.
         */
        fun getDbVersion(context: Context): Int {
            if (dbVersion < 0) {
                dbVersion = context.resources.getInteger(R.integer.instagram_db_version)
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
        db.execSQL(T_INSTAGRAM_NEWS_SQL_DROP)
        initAllDbTables(db)
    }

    private fun initAllDbTables(db: SQLiteDatabase) {
        db.execSQL(T_INSTAGRAM_NEWS_SQL_CREATE)
        storage.saveLastUpdate(null, null)
    }
}
