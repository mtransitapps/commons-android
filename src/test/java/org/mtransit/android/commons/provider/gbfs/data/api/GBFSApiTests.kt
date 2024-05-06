package org.mtransit.android.commons.provider.gbfs.data.api

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSGbfsApiModel.GBFSFeedsApiModel.FeedAPiModel.GBFSFileTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSFormFactorApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSPropulsionTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSReturnConstraintApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSVehicleAccessoriesApiModel
import org.mtransit.commons.CommonsApp
import java.util.Date
import kotlin.test.assertNotNull

class GBFSApiTests {

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

        val result: GBFSGbfsApiModel = GBFSParser.gson.fromJson(string, GBFSGbfsApiModel::class.java)

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

        val result: GBFSManifestApiModel = GBFSParser.gson.fromJson(string, GBFSManifestApiModel::class.java)

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

        val result: GBFSGbfsVersionsApiModel = GBFSParser.gson.fromJson(string, GBFSGbfsVersionsApiModel::class.java)

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

        val result: GBFSSystemInformationApiModel = GBFSParser.gson.fromJson(string, GBFSSystemInformationApiModel::class.java)

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

        val result: GBFSVehicleTypesApiModel = GBFSParser.gson.fromJson(string, GBFSVehicleTypesApiModel::class.java)

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
                            assertEquals(Date(1623729600_000L), iconLastModified)
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
                            assertEquals(Date(1623729600_000L), iconLastModified)
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
                            assertEquals(Date(1623729600_000L), iconLastModified)
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
                            assertEquals(Date(1623729600_000L), iconLastModified)
                        }
                        assertEquals("car_plan_1", defaultPricingPlanId)
                    }
                }
            }
        }
    }
}