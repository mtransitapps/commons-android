package org.mtransit.android.commons

object KeysIds : MTLog.Loggable {

    private val LOG_TAG: String = KeysIds::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    const val GOOGLE_PLACES_API_KEY = "google_places_api_key" // poi

    const val TWITTER_BEARER_TOKEN = "twitter_bearer_token" // news
    const val YOUTUBE_API_KEY = "youtube_api_key" // news

    const val GTFS_REAL_TIME_URL_TOKEN = "gtfs_real_time_agency_url_token" // status
    const val GTFS_REAL_TIME_URL_SECRET = "gtfs_real_time_agency_url_secret" // status

    const val ONE_BUS_AWAY_API_KEY = "one_bus_away_api_key"

    // custom
    const val CA_SUDBURY_TRANSIT_AUTH_TOKEN = "greater_sudbury_auth_token"
    const val CA_WINNIPEG_TRANSIT_API_KEY = "ca_winnipeg_transit_api_key" // news & status
}