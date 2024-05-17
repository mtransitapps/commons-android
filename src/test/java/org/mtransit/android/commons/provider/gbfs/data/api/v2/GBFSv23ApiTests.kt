package org.mtransit.android.commons.provider.gbfs.data.api.v2

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.fromJson
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSGbfsApiModel.GBFSFeedsAPiModel.FeedAPiModel.GBFSFileTypeApiModel
import org.mtransit.commons.CommonsApp
import kotlin.test.assertNotNull

// https://github.com/MobilityData/gbfs/blob/v2.3/gbfs.md
class GBFSv23ApiTests {

    @Before
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_gbfs_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1640887163,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.3\",\n" +
                "  \"data\": {\n" +
                "    \"en\": {\n" +
                "      \"feeds\": [\n" +
                "        {\n" +
                "          \"name\": \"system_information\",\n" +
                "          \"url\": \"https://www.example.com/gbfs/1/en/system_information\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"station_information\",\n" +
                "          \"url\": \"https://www.example.com/gbfs/1/en/station_information\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"fr\" : {\n" +
                "      \"feeds\": [\n" +
                "        {\n" +
                "          \"name\": \"system_information\",\n" +
                "          \"url\": \"https://www.example.com/gbfs/1/fr/system_information\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"station_information\",\n" +
                "          \"url\": \"https://www.example.com/gbfs/1/fr/station_information\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"

        val result: GBFSGbfsApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1640887163L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.3", version)
            with(data) {
                assertNotNull(this)
                assertEquals(2, this.size)
                with(this["en"]) {
                    assertNotNull(this)
                    with(feeds) {
                        assertNotNull(this)
                        assertEquals(2, this.size)
                        with(this[0]) {
                            assertEquals(GBFSFileTypeApiModel.SYSTEM_INFORMATION, name)
                            assertEquals("https://www.example.com/gbfs/1/en/system_information", url)
                        }
                        with(this[1]) {
                            assertEquals(GBFSFileTypeApiModel.STATION_INFORMATION, name)
                            assertEquals("https://www.example.com/gbfs/1/en/station_information", url)
                        }
                    }
                }
                with(this["fr"]) {
                    assertNotNull(this)
                    with(feeds) {
                        assertNotNull(this)
                        assertEquals(2, this.size)
                        with(this[0]) {
                            assertEquals(GBFSFileTypeApiModel.SYSTEM_INFORMATION, name)
                            assertEquals("https://www.example.com/gbfs/1/fr/system_information", url)
                        }
                        with(this[1]) {
                            assertEquals(GBFSFileTypeApiModel.STATION_INFORMATION, name)
                            assertEquals("https://www.example.com/gbfs/1/fr/station_information", url)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_gbfs_versions_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1640887163,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.3\",\n" +
                "  \"data\": {\n" +
                "    \"versions\": [\n" +
                "      {\n" +
                "        \"version\": \"2.0\",\n" +
                "        \"url\": \"https://www.example.com/gbfs/2/gbfs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"version\": \"2.3\",\n" +
                "        \"url\": \"https://www.example.com/gbfs/2-3/gbfs\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSGbfsVersionsApiModel = GBFSParser.gson.fromJson(string)

        assertNotNull(result)
        with(result) {
            assertEquals(1640887163L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.3", version)
            with(data) {
                assertEquals(2, versions.size)
                with(versions[0]) {
                    assertEquals("2.0", version)
                    assertEquals("https://www.example.com/gbfs/2/gbfs", url)
                }
                with(versions[1]) {
                    assertEquals("2.3", version)
                    assertEquals("https://www.example.com/gbfs/2-3/gbfs", url)
                }
            }
        }
    }
}