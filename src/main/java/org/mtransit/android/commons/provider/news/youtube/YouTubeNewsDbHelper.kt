package org.mtransit.android.commons.provider.news.youtube

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.provider.news.NewsProvider.NewsDbHelper

class YouTubeNewsDbHelper(
    val context: Context,
    dbName: String = DB_NAME,
    dbVersion: Int = getDbVersion(context)
) : NewsDbHelper(context, dbName, dbVersion) {

    companion object {
        private val LOG_TAG: String = YouTubeNewsDbHelper::class.java.simpleName

        /**
         * Override if multiple [YouTubeNewsDbHelper] implementations in same app.
         */
        private const val DB_NAME = "youtube.db"

        private const val T_YOUTUBE_NEWS = T_NEWS

        private val T_YOUTUBE_NEWS_SQL_CREATE = getSqlCreateBuilder(T_YOUTUBE_NEWS).build()

        private val T_YOUTUBE_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_YOUTUBE_NEWS)

        private var dbVersion = -1

        /**
         * Override if multiple [YouTubeNewsDbHelper] in same app.
         */
        fun getDbVersion(context: Context): Int {
            if (dbVersion < 0) {
                dbVersion = context.resources.getInteger(R.integer.youtube_db_version)
                dbVersion++ // add news articles images URLs do DB -> FORCE DB update
            }
            return dbVersion
        }
    }

    override fun getLogTag() = LOG_TAG

    override fun getDbName(): String {
        return DB_NAME
    }

    override fun onCreateMT(db: SQLiteDatabase) {
        initAllDbTables(db)
    }

    override fun onUpgradeMT(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(T_YOUTUBE_NEWS_SQL_DROP)
        YouTubeStorage.saveLastUpdateMs(context, 0L)
        YouTubeStorage.saveLastUpdateLang(context, StringUtils.EMPTY)
        initAllDbTables(db)
    }

    private fun initAllDbTables(db: SQLiteDatabase) {
        db.execSQL(T_YOUTUBE_NEWS_SQL_CREATE)
    }
}