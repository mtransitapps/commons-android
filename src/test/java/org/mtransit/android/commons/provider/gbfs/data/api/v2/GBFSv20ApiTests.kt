package org.mtransit.android.commons.provider.gbfs.data.api.v2

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.fromJson
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSGbfsApiModel.GBFSFeedsAPiModel.FeedAPiModel.GBFSFileTypeApiModel
import org.mtransit.commons.CommonsApp
import kotlin.test.assertNotNull

// https://github.com/MobilityData/gbfs/blob/v2.1/gbfs.md
class GBFSv20ApiTests {

    @Before
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_gbfs_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1434054678,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.0\",\n" +
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
            assertEquals(1434054678L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.0", version)
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
                "  \"last_updated\": 1434054678,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.0\",\n" +
                "  \"data\": {\n" +
                "    \"versions\": [\n" +
                "      {\n" +
                "        \"version\":\"1.0\",\n" +
                "        \"url\":\"https://www.example.com/gbfs/1/gbfs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"version\":\"2.0\",\n" +
                "        \"url\":\"https://www.example.com/gbfs/2/gbfs\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSGbfsVersionsApiModel = GBFSParser.gson.fromJson(string)

        assertNotNull(result)
        with(result) {
            assertEquals(1434054678L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.0", version)
            with(data) {
                assertEquals(2, versions.size)
                with(versions[0]) {
                    assertEquals("1.0", version)
                    assertEquals("https://www.example.com/gbfs/1/gbfs", url)
                }
                with(versions[1]) {
                    assertEquals("2.0", version)
                    assertEquals("https://www.example.com/gbfs/2/gbfs", url)
                }
            }
        }
    }

    @Test
    fun test_system_information_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\":1611598155,\n" +
                "  \"ttl\":1800,\n" +
                "  \"version\": \"2.0\",\n" +
                "  \"data\":{\n" +
                "    \"phone_number\":\"1-800-555-1234\",\n" +
                "    \"name\":\"Example Ride\",\n" +
                "    \"operator\":\"Example Sharing, Inc\",\n" +
                "    \"start_date\":\"2010-06-10\",\n" +
                "    \"purchase_url\":\"https://www.exampleride.org\",\n" +
                "    \"timezone\":\"US/Central\",\n" +
                "    \"license_url\":\"https://exampleride.org/data-license.html\",\n" +
                "    \"short_name\":\"Example Ride\",\n" +
                "    \"email\":\"customerservice@exampleride.org\",\n" +
                "    \"url\":\"http://www.exampleride.org\",\n" +
                "    \"feed_contact_email\": datafeed@exampleride.org,\n" +
                "    \"system_id\":\"example_ride\",\n" +
                "    \"language\":\"en\"\n" +
                "  }\n" +
                "}"

        val result: GBFSSystemInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1611598155L, lastUpdated)
            assertEquals(1800, ttlInSec)
            assertEquals("2.0", version)
            with(data) {
                assertEquals("1-800-555-1234", phoneNumber)
                assertEquals("Example Ride", name)
                assertEquals("Example Sharing, Inc", operator)
                assertEquals("2010-06-10", startDate)
                assertEquals("https://www.exampleride.org", purchaseUrl)
                assertEquals("US/Central", timezone)
                assertEquals("https://exampleride.org/data-license.html", licenseUrl)
                assertEquals("Example Ride", shortName)
                assertEquals("customerservice@exampleride.org", email)
                assertEquals("http://www.exampleride.org", url)
                assertEquals("datafeed@exampleride.org", feedContactEmail)
                assertEquals("example_ride", systemId)
                assertEquals("en", language)
            }
        }
    }
}