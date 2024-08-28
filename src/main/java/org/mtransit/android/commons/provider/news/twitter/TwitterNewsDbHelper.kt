package org.mtransit.android.commons.provider.news.twitter

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.provider.NewsProvider.NewsDbHelper

class TwitterNewsDbHelper(
    val context: Context,
    dbName: String = DB_NAME,
    dbVersion: Int = getDbVersion(context)
) : NewsDbHelper(context, dbName, dbVersion) {

    companion object {
        private val LOG_TAG: String = TwitterNewsDbHelper::class.java.simpleName

        /**
         * Override if multiple [TwitterNewsDbHelper] implementations in same app.
         */
        private const val DB_NAME = "news_twitter.db"

        /**
         * Override if multiple [TwitterNewsDbHelper] implementations in same app.
         */
        const val PREF_KEY_AGENCY_LAST_UPDATE_MS = "pTwitterNewsLastUpdate"

        /**
         * Override if multiple [TwitterNewsDbHelper] implementations in same app.
         */
        const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pTwitterNewsLastUpdateLang"

        /**
         * Override if multiple [TwitterNewsDbHelper] implementations in same app.
         */
        private const val PREF_KEY_AGENCY_USER_NAME_ID = "pTwitterNewsUserNameId_"

        fun getPREF_KEY_AGENCY_USER_NAME_ID(userName: String): String {
            return PREF_KEY_AGENCY_USER_NAME_ID + userName
        }

        /**
         * Override if multiple [TwitterNewsDbHelper] implementations in same app.
         */
        private const val PREF_KEY_AGENCY_USER_NAME_SINCE_ID = "pTwitterNewsUserNameSinceId_"

        fun getPREF_KEY_AGENCY_USER_NAME_SINCE_ID(userName: String): String {
            return PREF_KEY_AGENCY_USER_NAME_SINCE_ID + userName
        }

        private const val T_TWITTER_NEWS = T_NEWS

        private val T_TWITTER_NEWS_SQL_CREATE = getSqlCreateBuilder(T_TWITTER_NEWS).build()

        private val T_TWITTER_NEWS_SQL_DROP =
            SqlUtils.getSQLDropIfExistsQuery(T_TWITTER_NEWS)

        private var dbVersion = -1

        /**
         * Override if multiple [TwitterNewsDbHelper] in same app.
         */
        fun getDbVersion(context: Context): Int {
            if (dbVersion < 0) {
                dbVersion = context.resources.getInteger(R.integer.twitter_db_version)
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
        db.execSQL(T_TWITTER_NEWS_SQL_DROP)
        PreferenceUtils.savePrefLclSync(
            context,
            PREF_KEY_AGENCY_LAST_UPDATE_MS,
            0L,
        )
        PreferenceUtils.savePrefLclSync(
            context,
            PREF_KEY_AGENCY_LAST_UPDATE_LANG,
            StringUtils.EMPTY,
        )
        initAllDbTables(db)
    }

    private fun initAllDbTables(db: SQLiteDatabase) {
        db.execSQL(T_TWITTER_NEWS_SQL_CREATE)
    }
}