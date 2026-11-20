package org.mtransit.android.commons.provider.common

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderConfigTest {

    @Test
    fun test_authorityToUUID() {
        listOf(
            "news.rss",
            "news.twitter",
            "news.youtube",
            "news.instagram",

            "bike",
            "gtfs",

            "gtfs.realtime",

            "clever_devices",
            "nextbus",
            "oba",

            "transitlive_com",
            "rtcquebec",
            "stminfoapi",
            "winnipeg_transit",
        ).forEach { kind ->
            listOf("debug.", "").forEach { debug ->
                println("$debug$kind")
                "org.mtransit.android.$debug$kind".let {
                    ProviderConfig.authorityToStoragePrefix(it)
                }.let { result ->
                    assertEquals("", result)
                }
                "org.mtransit.android.ca_montreal_stm_bus.$debug$kind".let {
                    ProviderConfig.authorityToStoragePrefix(it)
                }.let { result ->
                    assertEquals("ca_montreal_stm_bus", result)
                }
                "com.another.published.xx_aaa_bbb_bus.$debug$kind".let {
                    ProviderConfig.authorityToStoragePrefix(it)
                }.let { result ->
                    assertEquals("com_another_published_xx_aaa_bbb_bus", result)
                }
            }
        }
    }

    @Test
    fun test_authorityToUUID_unknown() {
        val kind = "news.future"
        listOf("", "debug.").forEach { debug ->
            println("$debug$kind")
            "org.mtransit.android.$debug$kind".let {
                ProviderConfig.authorityToStoragePrefix(it)
            }.let { result ->
                assertEquals("news_future", result)
            }
            "org.mtransit.android.ca_montreal_stm_bus.$debug$kind".let {
                ProviderConfig.authorityToStoragePrefix(it)
            }.let { result ->
                assertEquals("ca_montreal_stm_bus_news_future", result)
            }
            "com.another.published.xx_aaa_bbb_bus.$debug$kind".let {
                ProviderConfig.authorityToStoragePrefix(it)
            }.let { result ->
                assertEquals("com_another_published_xx_aaa_bbb_bus_news_future", result)
            }
        }
    }
}
