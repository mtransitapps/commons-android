package org.mtransit.android.commons.provider.gbfs.data.api

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.fromJson
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSGbfsApiModel.GBFSFeedsApiModel.FeedAPiModel.GBFSFileTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSStationInformationApiModel.GBFSStationInformationDataApiModel.GBFSStationApiModel.GBFSParkingTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSSystemAlertsApiModel.GBFSSystemAlertsDataApiModel.GBFSAlertApiModel.GBFSAlertTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleStatusApiModel.GBFSVehicleStatusDataApiModel.GBFSVehicleStatusApiModel.GBFSVehicleEquipmentApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSFormFactorApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSPropulsionTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSReturnConstraintApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSVehicleAccessoriesApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSGeoJSONApiModel.GBFSGeoJSONTypeApiModel
import org.mtransit.commons.CommonsApp
import java.util.Date
import kotlin.test.assertNotNull

class GBFSv3ApiTests {

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

        val result: GBFSGbfsApiModel = GBFSParser.gson.fromJson(string)

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

        val result: GBFSManifestApiModel = GBFSParser.gson.fromJson(string)

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

        val result: GBFSGbfsVersionsApiModel = GBFSParser.gson.fromJson(string)

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
                "    \"system_id\": \"example_city_name\",\n" +
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

        val result: GBFSSystemInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(1800, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                assertEquals("example_city_name", systemId)
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
                assertEquals("2010-06-10", startDate)
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
                assertEquals("2021-06-21", termsLastUpdated)
                with(privacyUrl) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("https://www.example.com/en/privacy-policy", text)
                        assertEquals("en", language)
                    }
                }
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

    @Test
    fun test_vehicle_types_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"vehicle_types\": [\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"abc123\",\n" +
                "        \"form_factor\": \"bicycle\",\n" +
                "        \"propulsion_type\": \"human\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"Example Basic Bike\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"wheel_count\": 2,\n" +
                "        \"default_reserve_time\": 30,\n" +
                "        \"return_constraint\": \"any_station\",\n" +
                "        \"vehicle_assets\": {\n" +
                "          \"icon_url\": \"https://www.example.com/assets/icon_bicycle.svg\",\n" +
                "          \"icon_url_dark\": \"https://www.example.com/assets/icon_bicycle_dark.svg\",\n" +
                "          \"icon_last_modified\": \"2021-06-15\"\n" +
                "        },\n" +
                "        \"default_pricing_plan_id\": \"bike_plan_1\",\n" +
                "        \"pricing_plan_ids\": [\n" +
                "          \"bike_plan_1\",\n" +
                "          \"bike_plan_2\",\n" +
                "          \"bike_plan_3\"\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"cargo123\",\n" +
                "        \"form_factor\": \"cargo_bicycle\",\n" +
                "        \"propulsion_type\": \"human\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"Example Cargo Bike\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"description\": [\n" +
                "          {\n" +
                "            \"text\": \"Extra comfortable seat with additional suspension.\\n\\nPlease be aware of the cargo box lock: you need to press it down before pulling it up again!\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],            \n" +
                "        \"wheel_count\": 3,\n" +
                "        \"default_reserve_time\": 30,\n" +
                "        \"return_constraint\": \"roundtrip_station\",\n" +
                "        \"vehicle_assets\": {\n" +
                "          \"icon_url\": \"https://www.example.com/assets/icon_cargobicycle.svg\",\n" +
                "          \"icon_url_dark\": \"https://www.example.com/assets/icon_cargobicycle_dark.svg\",\n" +
                "          \"icon_last_modified\": \"2021-06-15\"\n" +
                "        },\n" +
                "        \"default_pricing_plan_id\": \"cargo_plan_1\",\n" +
                "        \"pricing_plan_ids\": [\n" +
                "          \"cargo_plan_1\",\n" +
                "          \"cargo_plan_2\",\n" +
                "          \"cargo_plan_3\"\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"def456\",\n" +
                "        \"form_factor\": \"scooter_standing\",\n" +
                "        \"propulsion_type\": \"electric\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"Example E-scooter V2\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"wheel_count\": 2,\n" +
                "        \"max_permitted_speed\": 25,\n" +
                "        \"rated_power\": 350,\n" +
                "        \"default_reserve_time\": 30,\n" +
                "        \"max_range_meters\": 12345,\n" +
                "        \"return_constraint\": \"free_floating\",\n" +
                "        \"vehicle_assets\": {\n" +
                "          \"icon_url\": \"https://www.example.com/assets/icon_escooter.svg\",\n" +
                "          \"icon_url_dark\": \"https://www.example.com/assets/icon_escooter_dark.svg\",\n" +
                "          \"icon_last_modified\": \"2021-06-15\"\n" +
                "        },\n" +
                "        \"default_pricing_plan_id\": \"scooter_plan_1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"car1\",\n" +
                "        \"form_factor\": \"car\",\n" +
                "        \"rider_capacity\": 5,\n" +
                "        \"cargo_volume_capacity\": 200,\n" +
                "        \"propulsion_type\": \"combustion_diesel\",\n" +
                "        \"eco_labels\": [\n" +
                "          {\n" +
                "            \"country_code\": \"FR\",\n" +
                "            \"eco_sticker\": \"critair_1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"country_code\": \"DE\",\n" +
                "            \"eco_sticker\": \"euro_2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"Four-door Sedan\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"wheel_count\": 4,\n" +
                "        \"default_reserve_time\": 0,\n" +
                "        \"max_range_meters\": 523992,\n" +
                "        \"return_constraint\": \"roundtrip_station\",\n" +
                "        \"vehicle_accessories\": [\n" +
                "          \"doors_4\",\n" +
                "          \"automatic\",\n" +
                "          \"cruise_control\"\n" +
                "        ],\n" +
                "        \"g_CO2_km\": 120,\n" +
                "        \"vehicle_image\": \"https://www.example.com/assets/renault-clio.jpg\",\n" +
                "        \"make\": [\n" +
                "          {\n" +
                "            \"text\": \"Renault\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"model\": [\n" +
                "          {\n" +
                "            \"text\": \"Clio\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"color\": \"white\",\n" +
                "        \"vehicle_assets\": {\n" +
                "          \"icon_url\": \"https://www.example.com/assets/icon_car.svg\",\n" +
                "          \"icon_url_dark\": \"https://www.example.com/assets/icon_car_dark.svg\",\n" +
                "          \"icon_last_modified\": \"2021-06-15\"\n" +
                "        },\n" +
                "        \"default_pricing_plan_id\": \"car_plan_1\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSVehicleTypesApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(vehicleTypes) {
                    assertNotNull(this)
                    assertEquals(4, size)
                    with(this[0]) {
                        assertEquals("abc123", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.BICYCLE, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.HUMAN, propulsionType)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Example Basic Bike", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(2, wheelCount)
                        assertEquals(30, defaultReserveTimeMin)
                        assertEquals(GBFSReturnConstraintApiModel.ANY_STATION, returnConstraint)
                        with(vehicleAssets) {
                            assertNotNull(this)
                            assertEquals("https://www.example.com/assets/icon_bicycle.svg", iconUrl)
                            assertEquals("https://www.example.com/assets/icon_bicycle_dark.svg", iconUrlDark)
                            assertEquals("2021-06-15", iconLastModified)
                        }
                        assertEquals("bike_plan_1", defaultPricingPlanId)
                        with(pricingPlanIds) {
                            assertNotNull(this)
                            assertEquals(3, size)
                            assertEquals("bike_plan_1", this[0])
                            assertEquals("bike_plan_2", this[1])
                            assertEquals("bike_plan_3", this[2])
                        }
                    }
                    with(this[1]) {
                        assertEquals("cargo123", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.CARGO_BICYCLE, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.HUMAN, propulsionType)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Example Cargo Bike", text)
                                assertEquals("en", language)
                            }
                        }
                        with(description) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals(
                                    "Extra comfortable seat with additional suspension.\n\nPlease be aware of the cargo box lock: you need to press it down before pulling it up again!",
                                    text
                                )
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(3, wheelCount)
                        assertEquals(30, defaultReserveTimeMin)
                        assertEquals(GBFSReturnConstraintApiModel.ROUNDTRIP_STATION, returnConstraint)
                        with(vehicleAssets) {
                            assertNotNull(this)
                            assertEquals("https://www.example.com/assets/icon_cargobicycle.svg", iconUrl)
                            assertEquals("https://www.example.com/assets/icon_cargobicycle_dark.svg", iconUrlDark)
                            assertEquals("2021-06-15", iconLastModified)
                        }
                        assertEquals("cargo_plan_1", defaultPricingPlanId)
                        with(pricingPlanIds) {
                            assertNotNull(this)
                            assertEquals(3, size)
                            assertEquals("cargo_plan_1", this[0])
                            assertEquals("cargo_plan_2", this[1])
                            assertEquals("cargo_plan_3", this[2])
                        }
                    }
                    with(this[2]) {
                        assertEquals("def456", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.SCOOTER_STANDING, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.ELECTRIC, propulsionType)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Example E-scooter V2", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(2, wheelCount)
                        assertEquals(25, maxPermittedSpeed)
                        assertEquals(350, ratedPower)
                        assertEquals(30, defaultReserveTimeMin)
                        assertEquals(12_345F, maxRangeMeters)
                        assertEquals(GBFSReturnConstraintApiModel.FREE_FLOATING, returnConstraint)
                        with(vehicleAssets) {
                            assertNotNull(this)
                            assertEquals("https://www.example.com/assets/icon_escooter.svg", iconUrl)
                            assertEquals("https://www.example.com/assets/icon_escooter_dark.svg", iconUrlDark)
                            assertEquals("2021-06-15", iconLastModified)
                        }
                        assertEquals("scooter_plan_1", defaultPricingPlanId)
                    }
                    with(this[3]) {
                        assertEquals("car1", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.CAR, formFactor)
                        assertEquals(5, riderCapacity)
                        assertEquals(200, cargoVolumeCapacity)
                        assertEquals(GBFSPropulsionTypeApiModel.COMBUSTION_DIESEL, propulsionType)
                        with(ecoLabels) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            with(this[0]) {
                                assertEquals("FR", countryCode)
                                assertEquals("critair_1", ecoSticker)
                            }
                            with(this[1]) {
                                assertEquals("DE", countryCode)
                                assertEquals("euro_2", ecoSticker)
                            }
                        }
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Four-door Sedan", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(4, wheelCount)
                        assertEquals(0, defaultReserveTimeMin)
                        assertEquals(523_992F, maxRangeMeters)
                        assertEquals(GBFSReturnConstraintApiModel.ROUNDTRIP_STATION, returnConstraint)
                        with(vehicleAccessories) {
                            assertNotNull(this)
                            assertEquals(3, size)
                            assertEquals(GBFSVehicleAccessoriesApiModel.DOORS_4, this[0])
                            assertEquals(GBFSVehicleAccessoriesApiModel.AUTOMATIC, this[1])
                            assertEquals(GBFSVehicleAccessoriesApiModel.CRUISE_CONTROL, this[2])
                        }
                        assertEquals(120, gCO2Km)
                        assertEquals("https://www.example.com/assets/renault-clio.jpg", vehicleImage)
                        with(make) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Renault", text)
                                assertEquals("en", language)
                            }
                        }
                        with(model) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Clio", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("white", color)
                        with(vehicleAssets) {
                            assertNotNull(this)
                            assertEquals("https://www.example.com/assets/icon_car.svg", iconUrl)
                            assertEquals("https://www.example.com/assets/icon_car_dark.svg", iconUrlDark)
                            assertEquals("2021-06-15", iconLastModified)
                        }
                        assertEquals("car_plan_1", defaultPricingPlanId)
                    }
                }
            }
        }
    }

    @Test
    fun test_station_information_json_parsing_physical_station_limited_hours_of_operation() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"station_id\": \"pga\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"Parking garage A\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"lat\": 12.345678,\n" +
                "        \"lon\": 45.678901,\n" +
                "        \"station_opening_hours\": \"Su-Th 05:00-22:00; Fr-Sa 05:00-01:00\",\n" +
                "        \"parking_type\": \"underground_parking\",\n" +
                "        \"parking_hoop\": false,\n" +
                "        \"contact_phone\": \"+33109874321\",\n" +
                "        \"is_charging_station\": true,\n" +
                "        \"vehicle_docks_capacity\": [\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [\"abc123\"],\n" +
                "            \"count\": 7\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSStationInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(stations) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("pga", stationId)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Parking garage A", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(12.345678, lat, 0.01)
                        assertEquals(45.678901, lon, 0.01)
                        assertEquals("Su-Th 05:00-22:00; Fr-Sa 05:00-01:00", stationOpeningHours)
                        assertEquals(GBFSParkingTypeApiModel.UNDERGROUND_PARKING, parkingType)
                        assertEquals(false, parkingHoop)
                        assertEquals("+33109874321", contactPhone)
                        assertEquals(true, isChargingStation)
                        with(vehicleDocksCapacity) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals(1, vehicleTypeIds.size)
                                assertEquals("abc123", vehicleTypeIds[0])
                                assertEquals(7, count)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_station_information_json_parsing_virtual_station() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"station_id\": \"station12\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"SE Belmont & SE 10th\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"lat\": 45.516445,\n" +
                "        \"lon\": -122.655775,\n" +
                "        \"is_valet_station\": false,\n" +
                "        \"is_virtual_station\": true,\n" +
                "        \"is_charging_station\": false,\n" +
                "        \"station_area\": {\n" +
                "          \"type\": \"MultiPolygon\",\n" +
                "          \"coordinates\": [\n" +
                "            [\n" +
                "              [\n" +
                "                [\n" +
                "                  -122.655775,\n" +
                "                  45.516445\n" +
                "                ],\n" +
                "                [\n" +
                "                  -122.655705,\n" +
                "                  45.516445\n" +
                "                ],\n" +
                "                [\n" +
                "                  -122.655705,\n" +
                "                  45.516495\n" +
                "                ],\n" +
                "                [\n" +
                "                  -122.655775,\n" +
                "                  45.516495\n" +
                "                ],\n" +
                "                [\n" +
                "                  -122.655775,\n" +
                "                  45.516445\n" +
                "                ]\n" +
                "              ]\n" +
                "            ]\n" +
                "          ]\n" +
                "        },\n" +
                "        \"capacity\": 16,\n" +
                "        \"vehicle_types_capacity\": [\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [\"abc123\", \"def456\"],\n" +
                "            \"count\": 15\n" +
                "          },\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [\"def456\"],\n" +
                "            \"count\": 1\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSStationInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(stations) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("station12", stationId)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("SE Belmont & SE 10th", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(45.516445, lat, 0.01)
                        assertEquals(-122.655775, lon, 0.01)
                        assertEquals(false, isValetStation)
                        assertEquals(true, isVirtualStation)
                        assertEquals(false, isChargingStation)
                        with(stationArea) {
                            assertNotNull(this)
                            assertEquals(GBFSGeoJSONTypeApiModel.MULTI_POLYGON, type)
                            with(coordinates) {
                                assertNotNull(this)
                                assertEquals(1, size)
                                with(this[0]) {
                                    assertNotNull(this)
                                    assertEquals(1, size)
                                    with(this[0]) {
                                        assertNotNull(this)
                                        assertEquals(5, size)
                                        with(this[0]) {
                                            assertEquals(-122.655775, this[0], 0.01)
                                            assertEquals(45.516445, this[1], 0.01)
                                        }
                                        with(this[1]) {
                                            assertEquals(-122.655705, this[0], 0.01)
                                            assertEquals(45.516445, this[1], 0.01)
                                        }
                                        with(this[2]) {
                                            assertEquals(-122.655705, this[0], 0.01)
                                            assertEquals(45.516495, this[1], 0.01)
                                        }
                                        with(this[3]) {
                                            assertEquals(-122.655775, this[0], 0.01)
                                            assertEquals(45.516495, this[1], 0.01)
                                        }
                                        with(this[4]) {
                                            assertEquals(-122.655775, this[0], 0.01)
                                            assertEquals(45.516445, this[1], 0.01)
                                        }
                                    }
                                }
                            }
                        }
                        assertEquals(16, capacity)
                        with(vehicleTypesCapacity) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            with(this[0]) {
                                assertEquals(2, vehicleTypeIds.size)
                                assertEquals("abc123", vehicleTypeIds[0])
                                assertEquals("def456", vehicleTypeIds[1])
                                assertEquals(15, count)
                            }
                            with(this[1]) {
                                assertEquals(1, vehicleTypeIds.size)
                                assertEquals("def456", vehicleTypeIds[0])
                                assertEquals(1, count)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_station_status_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"station_id\": \"station1\",\n" +
                "        \"is_installed\": true,\n" +
                "        \"is_renting\": true,\n" +
                "        \"is_returning\": true,\n" +
                "        \"last_reported\": \"2023-07-17T13:34:13+02:00\",\n" +
                "        \"num_docks_available\": 3,\n" +
                "        \"num_docks_disabled\" : 1,\n" +
                "        \"vehicle_docks_available\": [\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [ \"abc123\", \"def456\" ],\n" +
                "            \"count\": 2\n" +
                "          },\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [ \"def456\" ],\n" +
                "            \"count\": 1\n" +
                "          }\n" +
                "        ],\n" +
                "        \"num_vehicles_available\": 1,\n" +
                "        \"num_vehicles_disabled\": 2,\n" +
                "        \"vehicle_types_available\": [\n" +
                "          {\n" +
                "            \"vehicle_type_id\": \"abc123\",\n" +
                "            \"count\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"vehicle_type_id\": \"def456\",\n" +
                "            \"count\": 0\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"station_id\": \"station2\",\n" +
                "        \"is_installed\": true,\n" +
                "        \"is_renting\": true,\n" +
                "        \"is_returning\": true,\n" +
                "        \"last_reported\": \"2023-07-17T13:34:13+02:00\",\n" +
                "        \"num_docks_available\": 8,\n" +
                "        \"num_docks_disabled\" : 1,\n" +
                "        \"vehicle_docks_available\": [\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [ \"abc123\" ],\n" +
                "            \"count\": 6\n" +
                "          },\n" +
                "          {\n" +
                "            \"vehicle_type_ids\": [ \"def456\" ],\n" +
                "            \"count\": 2\n" +
                "          }\n" +
                "        ],\n" +
                "        \"num_vehicles_available\": 6,\n" +
                "        \"num_vehicles_disabled\": 1, \n" +
                "        \"vehicle_types_available\": [\n" +
                "          {\n" +
                "            \"vehicle_type_id\": \"abc123\",\n" +
                "            \"count\": 2\n" +
                "          },\n" +
                "          {\n" +
                "            \"vehicle_type_id\": \"def456\",\n" +
                "            \"count\": 4\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSStationStatusApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(stations) {
                    assertNotNull(this)
                    assertEquals(2, size)
                    with(this[0]) {
                        assertEquals("station1", stationId)
                        assertEquals(true, isInstalled)
                        assertEquals(true, isRenting)
                        assertEquals(true, isReturning)
                        assertEquals(Date(1689593653_000L), lastReported)
                        assertEquals(3, numDocksAvailable)
                        assertEquals(1, numDocksDisabled)
                        with(vehicleDocksAvailable) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            with(this[0]) {
                                assertEquals(2, vehicleTypeIds.size)
                                assertEquals("abc123", vehicleTypeIds[0])
                                assertEquals("def456", vehicleTypeIds[1])
                                assertEquals(2, count)
                            }
                            with(this[1]) {
                                assertEquals(1, vehicleTypeIds.size)
                                assertEquals("def456", vehicleTypeIds[0])
                                assertEquals(1, count)
                            }
                        }
                        assertEquals(1, numVehiclesAvailable)
                        assertEquals(2, numVehiclesDisabled)
                        with(vehicleTypesAvailable) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            with(this[0]) {
                                assertEquals("abc123", vehicleTypeId)
                                assertEquals(1, count)
                            }
                            with(this[1]) {
                                assertEquals("def456", vehicleTypeId)
                                assertEquals(0, count)
                            }
                        }
                    }
                    with(this[1]) {
                        assertEquals("station2", stationId)
                        assertEquals(true, isInstalled)
                        assertEquals(true, isRenting)
                        assertEquals(true, isReturning)
                        assertEquals(Date(1689593653_000L), lastReported)
                        assertEquals(8, numDocksAvailable)
                        assertEquals(1, numDocksDisabled)
                        with(vehicleDocksAvailable) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            with(this[0]) {
                                assertEquals(1, vehicleTypeIds.size)
                                assertEquals("abc123", vehicleTypeIds[0])
                                assertEquals(6, count)
                            }
                            with(this[1]) {
                                assertEquals(1, vehicleTypeIds.size)
                                assertEquals("def456", vehicleTypeIds[0])
                                assertEquals(2, count)
                            }
                        }
                        assertEquals(6, numVehiclesAvailable)
                        assertEquals(1, numVehiclesDisabled)
                        with(vehicleTypesAvailable) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            with(this[0]) {
                                assertEquals("abc123", vehicleTypeId)
                                assertEquals(2, count)
                            }
                            with(this[1]) {
                                assertEquals("def456", vehicleTypeId)
                                assertEquals(4, count)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_vehicle_status_json_parsing_micro_mobility() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\":0,\n" +
                "  \"version\":\"3.0\",\n" +
                "  \"data\":{\n" +
                "    \"vehicles\":[\n" +
                "      {\n" +
                "        \"vehicle_id\":\"973a5c94-c288-4a2b-afa6-de8aeb6ae2e5\",\n" +
                "        \"last_reported\": \"2023-07-17T13:34:13+02:00\",\n" +
                "        \"lat\":12.345678,\n" +
                "        \"lon\":56.789012,\n" +
                "        \"is_reserved\":false,\n" +
                "        \"is_disabled\":false,\n" +
                "        \"vehicle_type_id\":\"abc123\",\n" +
                "        \"rental_uris\": {\n" +
                "          \"android\": \"https://www.example.com/app?vehicle_id=973a5c94-c288-4a2b-afa6-de8aeb6ae2e5&platform=android&\",\n" +
                "          \"ios\": \"https://www.example.com/app?vehicle_id=973a5c94-c288-4a2b-afa6-de8aeb6ae2e5&platform=ios\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_id\":\"987fd100-b822-4347-86a4-b3eef8ca8b53\",\n" +
                "        \"last_reported\": \"2023-07-17T13:34:13+02:00\",\n" +
                "        \"is_reserved\":false,\n" +
                "        \"is_disabled\":false,\n" +
                "        \"vehicle_type_id\":\"def456\",\n" +
                "        \"current_range_meters\":6543.0,\n" +
                "        \"station_id\":\"86\",\n" +
                "        \"pricing_plan_id\":\"plan3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSVehicleStatusApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(vehicles) {
                    assertNotNull(this)
                    assertEquals(2, size)
                    with(this[0]) {
                        assertEquals("973a5c94-c288-4a2b-afa6-de8aeb6ae2e5", vehicleId)
                        assertEquals(Date(1689593653_000L), lastReported)
                        assertEquals(12.345678, lat, 0.01)
                        assertEquals(56.789012, lon, 0.01)
                        assertEquals(false, isReserved)
                        assertEquals(false, isDisabled)
                        assertEquals("abc123", vehicleTypeId)
                        with(rentalUris) {
                            assertNotNull(this)
                            with(android) {
                                assertNotNull(this)
                                assertEquals("https://www.example.com/app?vehicle_id=973a5c94-c288-4a2b-afa6-de8aeb6ae2e5&platform=android&", this)
                            }
                            with(ios) {
                                assertNotNull(this)
                                assertEquals("https://www.example.com/app?vehicle_id=973a5c94-c288-4a2b-afa6-de8aeb6ae2e5&platform=ios", this)
                            }
                        }
                    }
                    with(this[1]) {
                        assertEquals("987fd100-b822-4347-86a4-b3eef8ca8b53", vehicleId)
                        assertEquals(Date(1689593653_000L), lastReported)
                        assertEquals(false, isReserved)
                        assertEquals(false, isDisabled)
                        assertEquals("def456", vehicleTypeId)
                        assertEquals(6543.0F, currentRangeMeters)
                        assertEquals("86", stationId)
                        assertEquals("plan3", pricingPlanId)
                    }
                }
            }
        }
    }

    @Test
    fun test_vehicle_status_json_parsing_car_sharing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\":0,\n" +
                "  \"version\":\"3.0\",\n" +
                "  \"data\":{\n" +
                "    \"vehicles\":[\n" +
                "      {\n" +
                "        \"vehicle_id\":\"45bd3fb7-a2d5-4def-9de1-c645844ba962\",\n" +
                "        \"last_reported\": \"2023-07-17T13:34:13+02:00\",\n" +
                "        \"lat\":12.345678,\n" +
                "        \"lon\":56.789012,\n" +
                "        \"is_reserved\":false,\n" +
                "        \"is_disabled\":false,\n" +
                "        \"vehicle_type_id\":\"abc123\",\n" +
                "        \"current_range_meters\":400000.0,\n" +
                "        \"available_until\":\"2021-05-17T15:00:00Z\",\n" +
                "        \"home_station_id\":\"station1\",\n" +
                "        \"vehicle_equipment\":[\n" +
                "          \"child_seat_a\",\n" +
                "          \"winter_tires\"\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_id\":\"d4521def-7922-4e46-8e1d-8ac397239bd0\",\n" +
                "        \"last_reported\": \"2023-07-17T13:34:13+02:00\",\n" +
                "        \"is_reserved\":false,\n" +
                "        \"is_disabled\":false,\n" +
                "        \"vehicle_type_id\":\"def456\",\n" +
                "        \"current_fuel_percent\":0.7,\n" +
                "        \"current_range_meters\":6543.0,\n" +
                "        \"station_id\":\"86\",\n" +
                "        \"pricing_plan_id\":\"plan3\",\n" +
                "        \"home_station_id\":\"146\",\n" +
                "        \"vehicle_equipment\":[\n" +
                "          \"child_seat_a\"\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSVehicleStatusApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(vehicles) {
                    assertNotNull(this)
                    assertEquals(2, size)
                    with(this[0]) {
                        assertEquals("45bd3fb7-a2d5-4def-9de1-c645844ba962", vehicleId)
                        assertEquals(Date(1689593653_000L), lastReported)
                        assertEquals(12.345678, lat, 0.01)
                        assertEquals(56.789012, lon, 0.01)
                        assertEquals(false, isReserved)
                        assertEquals(false, isDisabled)
                        assertEquals("abc123", vehicleTypeId)
                        assertEquals(400_000F, currentRangeMeters)
                        assertEquals(Date(1621263600_000L), availableUntil)
                        assertEquals("station1", homeStationId)
                        with(vehicleEquipment) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            assertEquals(GBFSVehicleEquipmentApiModel.CHILD_SEAT_A, this[0])
                            assertEquals(GBFSVehicleEquipmentApiModel.WINTER_TIRES, this[1])
                        }
                    }
                    with(this[1]) {
                        assertEquals("d4521def-7922-4e46-8e1d-8ac397239bd0", vehicleId)
                        assertEquals(Date(1689593653_000L), lastReported)
                        assertEquals(false, isReserved)
                        assertEquals(false, isDisabled)
                        assertEquals("def456", vehicleTypeId)
                        assertEquals(0.7F, currentFuelPercent)
                        assertEquals(6543.0F, currentRangeMeters)
                        assertEquals("86", stationId)
                        assertEquals("plan3", pricingPlanId)
                        assertEquals("146", homeStationId)
                        with(vehicleEquipment) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            assertEquals(GBFSVehicleEquipmentApiModel.CHILD_SEAT_A, this[0])
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_system_regions_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 86400,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"regions\": [\n" +
                "      {\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"North\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"region_id\": \"3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"East\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"region_id\": \"4\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"South\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"region_id\": \"5\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"West\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"region_id\": \"6\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBGSSystemRegionsApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(86400, ttlInSec) // 24 hours
            assertEquals("3.0", version)
            with(data) {
                with(regions) {
                    assertNotNull(this)
                    assertEquals(4, size)
                    with(this[0]) {
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("North", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("3", regionId)
                    }
                    with(this[1]) {
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("East", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("4", regionId)
                    }
                    with(this[2]) {
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("South", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("5", regionId)
                    }
                    with(this[3]) {
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("West", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("6", regionId)
                    }
                }
            }
        }
    }

    @Test
    fun test_system_pricing_plans_json_parsing_1() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"plans\": [\n" +
                "      {\n" +
                "        \"plan_id\": \"plan2\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"One-Way\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"currency\": \"USD\",\n" +
                "        \"price\": 2.00,\n" +
                "        \"is_taxable\": false,\n" +
                "        \"description\": [\n" +
                "          {\n" +
                "            \"text\": \"Includes 10km, overage fees apply after 10km.\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"per_km_pricing\": [\n" +
                "          {\n" +
                "            \"start\": 10,\n" +
                "            \"rate\": 1.00,\n" +
                "            \"interval\": 1,\n" +
                "            \"end\": 25\n" +
                "          },\n" +
                "          {\n" +
                "            \"start\": 25,\n" +
                "            \"rate\": 0.50,\n" +
                "            \"interval\": 1\n" +
                "          },\n" +
                "          {\n" +
                "            \"start\": 25,\n" +
                "            \"rate\": 3.00,\n" +
                "            \"interval\": 5\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSSystemPricingPlansApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(plans) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("plan2", planId)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("One-Way", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("USD", currency)
                        assertEquals(2.00F, price)
                        assertEquals(false, isTaxable)
                        with(description) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Includes 10km, overage fees apply after 10km.", text)
                                assertEquals("en", language)
                            }
                        }
                        with(perKmPricing) {
                            assertNotNull(this)
                            assertEquals(3, size)
                            with(this[0]) {
                                assertEquals(10, start)
                                assertEquals(1.00F, rate)
                                assertEquals(1, interval)
                                assertEquals(25, end)
                            }
                            with(this[1]) {
                                assertEquals(25, start)
                                assertEquals(0.50F, rate)
                                assertEquals(1, interval)
                            }
                            with(this[2]) {
                                assertEquals(25, start)
                                assertEquals(3.00F, rate)
                                assertEquals(5, interval)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_system_pricing_plans_json_parsing_2() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"plans\": [\n" +
                "      {\n" +
                "        \"plan_id\": \"plan3\",\n" +
                "        \"name\": [\n" +
                "          {\n" +
                "            \"text\": \"Simple Rate\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"currency\": \"CAD\",\n" +
                "        \"price\": 3.00,\n" +
                "        \"is_taxable\": true,\n" +
                "        \"description\": [\n" +
                "          {\n" +
                "            \"text\": \"\$3 unlock fee, \$0.25 per kilometer and 0.50 per minute.\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"per_km_pricing\": [\n" +
                "          {\n" +
                "            \"start\": 0,\n" +
                "            \"rate\": 0.25,\n" +
                "            \"interval\": 1\n" +
                "          }\n" +
                "        ],\n" +
                "        \"per_min_pricing\": [\n" +
                "          {\n" +
                "            \"start\": 0,\n" +
                "            \"rate\": 0.50,\n" +
                "            \"interval\": 1\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSSystemPricingPlansApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(plans) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("plan3", planId)
                        with(name) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Simple Rate", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals("CAD", currency)
                        assertEquals(3.00F, price)
                        assertEquals(true, isTaxable)
                        with(description) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("\$3 unlock fee, \$0.25 per kilometer and 0.50 per minute.", text)
                                assertEquals("en", language)
                            }
                        }
                        with(perKmPricing) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals(0, start)
                                assertEquals(0.25F, rate)
                                assertEquals(1, interval)
                            }
                        }
                        with(perMinPricing) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals(0, start)
                                assertEquals(0.50F, rate)
                                assertEquals(1, interval)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_system_alerts_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": \"2023-07-17T13:34:13+02:00\",\n" +
                "  \"ttl\": 60,\n" +
                "  \"version\": \"3.0\",\n" +
                "  \"data\": {\n" +
                "    \"alerts\": [\n" +
                "      {\n" +
                "        \"alert_id\": \"21\",\n" +
                "        \"type\": \"station_closure\",\n" +
                "        \"station_ids\": [\n" +
                "          \"123\",\n" +
                "          \"456\",\n" +
                "          \"789\"\n" +
                "        ],\n" +
                "        \"times\": [\n" +
                "          {\n" +
                "            \"start\": \"2023-07-17T13:34:13+02:00\",\n" +
                "            \"end\": \"2023-07-18T13:34:13+02:00\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"url\": [\n" +
                "          {\n" +
                "            \"text\": \"https://example.com/more-info\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ], \n" +
                "        \"summary\": [\n" +
                "          {\n" +
                "            \"text\": \"Disruption of Service\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"description\": [\n" +
                "          {\n" +
                "            \"text\": \"The three stations on Broadway will be out of service from 12:00am Nov 3 to 3:00pm Nov 6th to accommodate road work\",\n" +
                "            \"language\": \"en\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"last_updated\": \"2023-07-17T13:34:13+02:00\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSSystemAlertsApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(Date(1689593653_000L), lastUpdated)
            assertEquals(60, ttlInSec)
            assertEquals("3.0", version)
            with(data) {
                with(alerts) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("21", alertId)
                        assertEquals(GBFSAlertTypeApiModel.STATION_CLOSURE, type)
                        with(stationIds) {
                            assertNotNull(this)
                            assertEquals(3, size)
                            assertEquals("123", this[0])
                            assertEquals("456", this[1])
                            assertEquals("789", this[2])
                        }
                        with(times) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals(Date(1689593653_000L), start)
                                assertEquals(Date(1689680053_000L), end)
                            }
                        }
                        with(url) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("https://example.com/more-info", text)
                                assertEquals("en", language)
                            }
                        }
                        with(summary) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("Disruption of Service", text)
                                assertEquals("en", language)
                            }
                        }
                        with(description) {
                            assertNotNull(this)
                            assertEquals(1, size)
                            with(this[0]) {
                                assertEquals("The three stations on Broadway will be out of service from 12:00am Nov 3 to 3:00pm Nov 6th to accommodate road work", text)
                                assertEquals("en", language)
                            }
                        }
                        assertEquals(Date(1689593653_000L), lastUpdated)
                    }
                }
            }
        }
    }
}