package org.mtransit.android.commons.provider.gbfs.data.api.v2

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.fromJson
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSGbfsApiModel.GBFSFeedsAPiModel.FeedAPiModel.GBFSFileTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSFormFactorApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSPropulsionTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSReturnConstraintApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSVehicleAccessoriesApiModel
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

    @Test
    fun test_vehicle_types_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1640887163,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.3\",\n" +
                "  \"data\": {\n" +
                "    \"vehicle_types\": [\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"abc123\",\n" +
                "        \"form_factor\": \"bicycle\",\n" +
                "        \"propulsion_type\": \"human\",\n" +
                "        \"name\": \"Example Basic Bike\",\n" +
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
                "        \"name\": \"Example Cargo Bike\",\n" +
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
                "        \"name\": \"Example E-scooter V2\",\n" +
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
                "        \"eco_label\": [\n" +
                "          {\n" +
                "            \"country_code\": \"FR\",\n" +
                "            \"eco_sticker\": \"critair_1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"country_code\": \"DE\",\n" +
                "            \"eco_sticker\": \"euro_2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": \"Four-door Sedan\",\n" +
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
                "        \"make\": \"Renault\",\n" +
                "        \"model\": \"Clio\",\n" +
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
            assertEquals(1640887163L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.3", version)
            with(data) {
                with(vehicleTypes) {
                    assertNotNull(this)
                    assertEquals(4, size)
                    with(this[0]) {
                        assertEquals("abc123", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.BICYCLE, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.HUMAN, propulsionType)
                        assertEquals("Example Basic Bike", name)
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
                        assertEquals("Example Cargo Bike", name)
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
                        assertEquals("Example E-scooter V2", name)
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
                        with(ecoLabel) {
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
                        assertEquals("Four-door Sedan", name)
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
                        assertEquals("Renault", make)
                        assertEquals("Clio", model)
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
}