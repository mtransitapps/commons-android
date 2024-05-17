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

    @Test
    fun test_system_information_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1640887163,\n" +
                "  \"ttl\": 1800,\n" +
                "  \"version\": \"2.3\",\n" +
                "  \"data\": {\n" +
                "    \"system_id\": \"example_city_name\",\n" +
                "    \"language\": \"en\",\n" +
                "    \"name\": \"Example Bike Rental\",\n" +
                "    \"short_name\": \"Example Bike\",\n" +
                "    \"operator\": \"Example Sharing, Inc\",\n" +
                "    \"url\": \"https://www.example.com\",\n" +
                "    \"purchase_url\": \"https://www.example.com\",\n" +
                "    \"start_date\": \"2010-06-10\",\n" +
                "    \"phone_number\": \"1-800-555-1234\",\n" +
                "    \"email\": \"customerservice@example.com\",\n" +
                "    \"feed_contact_email\": \"datafeed@example.com\",\n" +
                "    \"timezone\": \"America/Chicago\",\n" +
                "    \"license_url\": \"https://www.example.com/data-license.html\",\n" +
                "    \"terms_url\": \"https://www.example.com/terms\",\n" +
                "    \"terms_last_updated\": \"2021-06-21\",\n" +
                "    \"privacy_url\": \"https://www.example.com/privacy-policy\",\n" +
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
                "      \n" +
                "  }\n" +
                "}"

        val result: GBFSSystemInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1640887163L, lastUpdated)
            assertEquals(1800, ttlInSec)
            assertEquals("2.3", version)
            with(data) {
                assertEquals("example_city_name", systemId)
                assertEquals("en", language)
                assertEquals("Example Bike Rental", name)
                assertEquals("Example Bike", shortName)
                assertEquals("Example Sharing, Inc", operator)
                assertEquals("2010-06-10", startDate)
                assertEquals("https://www.example.com", url)
                assertEquals("https://www.example.com", purchaseUrl)
                assertEquals("1-800-555-1234", phoneNumber)
                assertEquals("customerservice@example.com", email)
                assertEquals("datafeed@example.com", feedContactEmail)
                assertEquals("America/Chicago", timezone)
                assertEquals("https://www.example.com/data-license.html", licenseUrl)
                assertEquals("https://www.example.com/terms", termsUrl)
                assertEquals("2021-06-21", termsLastUpdated)
                assertEquals("https://www.example.com/privacy-policy", privacyUrl)
                assertEquals("2019-01-13", privacyLastUpdated)
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
                    assertEquals("2021-06-15", brandLastModified)
                    assertEquals("https://www.example.com/assets/brand_image.svg", brandImageUrl)
                    assertEquals("https://www.example.com/assets/brand_image_dark.svg", brandImageUrlDark)
                    assertEquals("#C2D32C", color)
                    assertEquals("https://www.example.com/assets/brand.pdf", brandTermsUrl)
                }
            }
        }
    }
}