package org.mtransit.android.commons.provider.gbfs.data.api.v3

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSv3Parser
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSFileTypeApiModel
import org.mtransit.commons.CommonsApp
import java.util.Date
import kotlin.test.assertNotNull

class GBFSv3Tests {

    @Before
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_gbfs_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 7,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"feeds\": [\n" +
                "      {\n" +
                "        \"name\": \"system_information\",\n" +
                "        \"url\": \"https://www.example.com/gbfs/1/system_information\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"station_information\",\n" +
                "        \"url\": \"https://www.example.com/gbfs/1/station_information\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n"

        val result: GBFSGbfsApiModel = GBFSv3Parser.gson.fromJson(string, GBFSGbfsApiModel::class.java)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(7, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                assertEquals(2, feeds.size)
                with(feeds[0]) {
                    assertEquals(GBFSFileTypeApiModel.SYSTEM_INFORMATION, name)
                    assertEquals("https://www.example.com/gbfs/1/system_information", url)
                }
                with(feeds[1]) {
                    assertEquals(GBFSFileTypeApiModel.STATION_INFORMATION, name)
                    assertEquals("https://www.example.com/gbfs/1/station_information", url)
                }
            }
        }
    }

    @Test
    fun test_manifest_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\":1,\n" +
                "  \"version\":\"3.0\",\n" +
                "  \"data\":{\n" +
                "    \"datasets\":[\n" +
                "      {\n" +
                "        \"system_id\":\"example_berlin\",\n" +
                "        \"versions\":[\n" +
                "          {\n" +
                "            \"version\":\"2.0\",\n" +
                "            \"url\":\"https://berlin.example.com/gbfs/2/gbfs\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"version\":\"3.0\",\n" +
                "            \"url\":\"https://berlin.example.com/gbfs/3/gbfs\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"system_id\":\"example_paris\",\n" +
                "        \"versions\":[\n" +
                "          {\n" +
                "            \"version\":\"2.0\",\n" +
                "            \"url\":\"https://paris.example.com/gbfs/2/gbfs\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"version\":\"3.0\",\n" +
                "            \"url\":\"https://paris.example.com/gbfs/3/gbfs\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSManifestApiModel = GBFSv3Parser.gson.fromJson(string, GBFSManifestApiModel::class.java)

        assertNotNull(result)
        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(1, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                assertEquals(2, datasets.size)
                with(datasets[0]) {
                    assertEquals("example_berlin", systemId)
                    assertEquals(2, versions.size)
                    with(versions[0]) {
                        assertEquals("2.0", version)
                        assertEquals("https://berlin.example.com/gbfs/2/gbfs", url)
                    }
                    with(versions[1]) {
                        assertEquals("3.0", version)
                        assertEquals("https://berlin.example.com/gbfs/3/gbfs", url)
                    }
                }
                with(datasets[1]) {
                    assertEquals("example_paris", systemId)
                    assertEquals(2, versions.size)
                    with(versions[0]) {
                        assertEquals("2.0", version)
                        assertEquals("https://paris.example.com/gbfs/2/gbfs", url)
                    }
                    with(versions[1]) {
                        assertEquals("3.0", version)
                        assertEquals("https://paris.example.com/gbfs/3/gbfs", url)
                    }
                }
            }
        }
    }

    @Test
    fun test_gbfs_versions_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 2,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"versions\": [\n" +
                "      {\n" +
                "        \"version\": \"2.0\",\n" +
                "        \"url\": \"https://www.example.com/gbfs/2/gbfs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"version\": \"3.0\",\n" +
                "        \"url\": \"https://www.example.com/gbfs/3/gbfs\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSGbfsVersionsApiModel = GBFSv3Parser.gson.fromJson(string, GBFSGbfsVersionsApiModel::class.java)

        assertNotNull(result)
        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(2, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                assertEquals(2, versions.size)
                with(versions[0]) {
                    assertEquals("2.0", version)
                    assertEquals("https://www.example.com/gbfs/2/gbfs", url)
                }
                with(versions[1]) {
                    assertEquals("3.0", version)
                    assertEquals("https://www.example.com/gbfs/3/gbfs", url)
                }
            }
        }
    }

    @Test
    fun test_system_information_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 1800,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"system_id\": \"example_cityname\",\n" +
                "    \"languages\": [\"en\"],\n" +
                "    \"name\": [\n" +
                "      {\n" +
                "        \"text\": \"Example Bike Rental\",\n" +
                "        \"language\": \"en\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"short_name\": [\n" +
                "      {\n" +
                "        \"text\": \"Example Bike\",\n" +
                "        \"language\": \"en\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"operator\": [\n" +
                "      {\n" +
                "        \"text\": \"Example Sharing, Inc\",\n" +
                "        \"language\": \"en\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"opening_hours\": \"Apr 1-Nov 3 00:00-24:00\",\n" +
                "    \"start_date\": \"2010-06-10\",\n" +
                "    \"url\": \"https://www.example.com\",\n" +
                "    \"purchase_url\": \"https://www.example.com\",\n" +
                "    \"phone_number\": \"+18005551234\",\n" +
                "    \"email\": \"customerservice@example.com\",\n" +
                "    \"feed_contact_email\": \"datafeed@example.com\",\n" +
                "    \"timezone\": \"America/Chicago\",\n" +
                "    \"license_url\": \"https://www.example.com/data-license.html\",\n" +
                "    \"terms_url\": [\n" +
                "      {\n" +
                "         \"text\": \"https://www.example.com/en/terms\",\n" +
                "         \"language\": \"en\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"terms_last_updated\": \"2021-06-21\",\n" +
                "    \"privacy_url\": [\n" +
                "      {\n" +
                "         \"text\": \"https://www.example.com/en/privacy-policy\",\n" +
                "         \"language\": \"en\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"privacy_last_updated\": \"2019-01-13\",\n" +
                "    \"rental_apps\": {\n" +
                "      \"android\": {\n" +
                "        \"discovery_uri\": \"com.example.android://\",\n" +
                "        \"store_uri\": \"https://play.google.com/store/apps/details?id=com.example.android\"\n" +
                "      },\n" +
                "      \"ios\": {\n" +
                "        \"store_uri\": \"https://apps.apple.com/app/apple-store/id123456789\",\n" +
                "        \"discovery_uri\": \"com.example.ios://\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"brand_assets\": {\n" +
                "        \"brand_last_modified\": \"2021-06-15\",\n" +
                "        \"brand_image_url\": \"https://www.example.com/assets/brand_image.svg\",\n" +
                "        \"brand_image_url_dark\": \"https://www.example.com/assets/brand_image_dark.svg\",\n" +
                "        \"color\": \"#C2D32C\",\n" +
                "        \"brand_terms_url\": \"https://www.example.com/assets/brand.pdf\"\n" +
                "      }\n" +
                "\n" +
                "  }\n" +
                "}"

        val result: GBFSSystemInformationApiModel = GBFSv3Parser.gson.fromJson(string, GBFSSystemInformationApiModel::class.java)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(1800, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                assertEquals("example_cityname", systemId)
                assertEquals(1, languages.size)
                assertEquals("en", languages[0])
                assertEquals(1, name.size)
                with(name[0]) {
                    assertEquals("Example Bike Rental", text)
                    assertEquals("en", language)
                }
                with(shortName) {
                    assertNotNull(this)
                    assertEquals(1, this.size)
                    with(this[0]) {
                        assertEquals("Example Bike", text)
                        assertEquals("en", language)
                    }
                }
                with(operator) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("Example Sharing, Inc", text)
                        assertEquals("en", language)
                    }
                }
                assertEquals("Apr 1-Nov 3 00:00-24:00", openingHours)
                assertEquals(Date(1276142400_000L), startDate)
                assertEquals("https://www.example.com", url)
                assertEquals("https://www.example.com", purchaseUrl)
                assertEquals("+18005551234", phoneNumber)
                assertEquals("customerservice@example.com", email)
                assertEquals("datafeed@example.com", feedContactEmail)
                assertEquals("America/Chicago", timezone)
                assertEquals("https://www.example.com/data-license.html", licenseUrl)
                with(termsUrl) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("https://www.example.com/en/terms", text)
                        assertEquals("en", language)
                    }
                }
                assertEquals(Date(1624248000_000L), termsLastUpdated)
                with(privacyUrl) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("https://www.example.com/en/privacy-policy", text)
                        assertEquals("en", language)
                    }
                }
                assertEquals(Date(1547355600_000L), privacyLastUpdated)
                with(rentalApps) {
                    assertNotNull(this)
                    with(android) {
                        assertNotNull(this)
                        assertEquals("com.example.android://", discoveryUri)
                        assertEquals("https://play.google.com/store/apps/details?id=com.example.android", storeUri)
                    }
                    with(ios) {
                        assertNotNull(this)
                        assertEquals("https://apps.apple.com/app/apple-store/id123456789", storeUri)
                        assertEquals("com.example.ios://", discoveryUri)
                    }
                }
                with(brandAssets) {
                    assertNotNull(this)
                    assertEquals(Date(1623729600_000L), brandLastModified)
                    assertEquals("https://www.example.com/assets/brand_image.svg", brandImageUrl)
                    assertEquals("https://www.example.com/assets/brand_image_dark.svg", brandImageUrlDark)
                    assertEquals("#C2D32C", color)
                    assertEquals("https://www.example.com/assets/brand.pdf", brandTermsUrl)
                }
            }
        }
    }
}