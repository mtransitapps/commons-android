package org.mtransit.android.commons

object KeysIds : MTLog.Loggable {

    private val LOG_TAG: String = KeysIds::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    const val GOOGLE_PLACES_API_KEY = "google_places_api_key" // poi

    const val TWITTER_BEARER_TOKEN = "twitter_bearer_token" // news
    const val YOUTUBE_API_KEY = "youtube_api_key" // news

    const val GTFS_REAL_TIME_URL_TOKEN = "gtfs_real_time_agency_url_token" // status
    const val GTFS_REAL_TIME_URL_SECRET = "gtfs_real_time_agency_url_secret" // status

    // custom
    const val CA_WINNIPEG_TRANSIT_API = "ca_winnipeg_transit_api_key" // news & status
}