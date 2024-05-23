package org.mtransit.android.commons.provider.gbfs.data.api.v2

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.fromJson
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSGbfsApiModel.GBFSFeedsAPiModel.FeedAPiModel.GBFSFileTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSFormFactorApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel.GBFSVehicleTypeApiModel.GBFSPropulsionTypeApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSGeoJSONTypeApiModel
import org.mtransit.commons.CommonsApp
import kotlin.test.assertNotNull

// https://github.com/MobilityData/gbfs/blob/v2.2/gbfs.md
class GBFSv22ApiTests {

    @Before
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_gbfs_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1609866247,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.2\",\n" +
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
            assertEquals(1609866247L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.2", version)
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
                "  \"last_updated\": 1609866247,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.2\",\n" +
                "  \"data\": {\n" +
                "    \"versions\": [\n" +
                "      {\n" +
                "        \"version\":\"2.0\",\n" +
                "        \"url\":\"https://www.example.com/gbfs/2/gbfs\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"version\":\"2.2\",\n" +
                "        \"url\":\"https://www.example.com/gbfs/2-2/gbfs\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSGbfsVersionsApiModel = GBFSParser.gson.fromJson(string)

        assertNotNull(result)
        with(result) {
            assertEquals(1609866247L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.2", version)
            with(data) {
                assertEquals(2, versions.size)
                with(versions[0]) {
                    assertEquals("2.0", version)
                    assertEquals("https://www.example.com/gbfs/2/gbfs", url)
                }
                with(versions[1]) {
                    assertEquals("2.2", version)
                    assertEquals("https://www.example.com/gbfs/2-2/gbfs", url)
                }
            }
        }
    }

    @Test
    fun test_system_information_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\":1611598155,\n" +
                "  \"ttl\":1800,\n" +
                "  \"version\": \"2.2\",\n" +
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
                "    \"feed_contact_email\": \"datafeed@exampleride.org\",\n" +
                "    \"system_id\":\"example_ride\",\n" +
                "    \"language\":\"en\"\n" +
                "  }\n" +
                "}"

        val result: GBFSSystemInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1611598155L, lastUpdated)
            assertEquals(1800, ttlInSec)
            assertEquals("2.2", version)
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

    @Test
    fun test_vehicle_types_json_parsing() {
        val string = "{\n" +
                "  \"last_updated\": 1609866247,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.2\",\n" +
                "  \"data\": {\n" +
                "    \"vehicle_types\": [\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"abc123\",\n" +
                "        \"form_factor\": \"bicycle\",\n" +
                "        \"propulsion_type\": \"human\",\n" +
                "        \"name\": \"Example Basic Bike\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"def456\",\n" +
                "        \"form_factor\": \"scooter\",\n" +
                "        \"propulsion_type\": \"electric\",\n" +
                "        \"name\": \"Example E-scooter V2\",\n" +
                "        \"max_range_meters\": 12345\n" +
                "      },\n" +
                "      {\n" +
                "        \"vehicle_type_id\": \"car1\",\n" +
                "        \"form_factor\": \"car\",\n" +
                "        \"propulsion_type\": \"combustion\",\n" +
                "        \"name\": \"Foor-door Sedan\",\n" +
                "        \"max_range_meters\": 523992\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSVehicleTypesApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1609866247L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.2", version)
            with(data) {
                with(vehicleTypes) {
                    assertNotNull(this)
                    assertEquals(3, size)
                    with(this[0]) {
                        assertEquals("abc123", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.BICYCLE, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.HUMAN, propulsionType)
                        assertEquals("Example Basic Bike", name)
                    }
                    with(this[1]) {
                        assertEquals("def456", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.SCOOTER, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.ELECTRIC, propulsionType)
                        assertEquals("Example E-scooter V2", name)
                        assertEquals(12_345F, maxRangeMeters)
                    }
                    with(this[2]) {
                        assertEquals("car1", vehicleTypeId)
                        assertEquals(GBFSFormFactorApiModel.CAR, formFactor)
                        assertEquals(GBFSPropulsionTypeApiModel.COMBUSTION, propulsionType)
                        assertEquals("Foor-door Sedan", name)
                        assertEquals(523_992F, maxRangeMeters)
                    }
                }
            }
        }
    }

    @Test
    fun test_station_information_json_parsing_physical_station() {
        val string = "{\n" +
                "  \"last_updated\": 1609866247,\n" +
                "  \"ttl\": 0,\n" +
                "  \"version\": \"2.2\",\n" +
                "  \"data\": {\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"station_id\": \"pga\",\n" +
                "        \"name\": \"Parking garage A\",\n" +
                "        \"lat\": 12.345678,\n" +
                "        \"lon\": 45.678901,\n" +
                "        \"vehicle_type_capacity\": {\n" +
                "          \"abc123\": 7,\n" +
                "          \"def456\": 9\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSStationInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1609866247L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.2", version)
            with(data) {
                with(stations) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("pga", stationId)
                        assertEquals("Parking garage A", name)
                        assertEquals(12.345678, lat, 0.01)
                        assertEquals(45.678901, lon, 0.01)
                        with(vehicleTypeCapacity) {
                            assertNotNull(this)
                            assertEquals(2, size)
                            assertEquals(7, this["abc123"])
                            assertEquals(9, this["def456"])
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_station_information_json_parsing_virtual_station() {
        val string = "{\n" +
                "  \"last_updated\":1609866247,\n" +
                "  \"ttl\":0,\n" +
                "  \"version\":\"2.2\",\n" +
                "  \"data\":{\n" +
                "    \"stations\":[\n" +
                "      {\n" +
                "        \"station_id\":\"station12\",\n" +
                "        \"name\":\"SE Belmont & SE 10 th\",\n" +
                "        \"is_valet_station\":false,\n" +
                "        \"is_virtual_station\":true,\n" +
                "        \"station_area\":{\n" +
                "          \"type\":\"MultiPolygon\",\n" +
                "          \"coordinates\":[\n" +
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
                "        \"capacity\":16,\n" +
                "        \"vehicle_capacity\":{\n" +
                "          \"abc123\":8,\n" +
                "          \"def456\":8,\n" +
                "          \"ghi789\":16\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val result: GBFSStationInformationApiModel = GBFSParser.gson.fromJson(string)

        with(result) {
            assertEquals(1609866247L, lastUpdated)
            assertEquals(0, ttlInSec)
            assertEquals("2.2", version)
            with(data) {
                with(stations) {
                    assertNotNull(this)
                    assertEquals(1, size)
                    with(this[0]) {
                        assertEquals("station12", stationId)
                        assertEquals("SE Belmont & SE 10 th", name)
                        assertEquals(false, isValetStation)
                        assertEquals(true, isVirtualStation)
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
                                            assertNotNull(this)
                                            assertEquals(-122.655775, this[0], 0.01)
                                            assertEquals(45.516445, this[1], 0.01)
                                        }
                                        with(this[1]) {
                                            assertNotNull(this)
                                            assertEquals(-122.655705, this[0], 0.01)
                                            assertEquals(45.516445, this[1], 0.01)
                                        }
                                        with(this[2]) {
                                            assertNotNull(this)
                                            assertEquals(-122.655705, this[0], 0.01)
                                            assertEquals(45.516495, this[1], 0.01)
                                        }
                                        with(this[3]) {
                                            assertNotNull(this)
                                            assertEquals(-122.655775, this[0], 0.01)
                                            assertEquals(45.516495, this[1], 0.01)
                                        }
                                        with(this[4]) {
                                            assertNotNull(this)
                                            assertEquals(-122.655775, this[0], 0.01)
                                            assertEquals(45.516445, this[1], 0.01)
                                        }
                                    }
                                }
                            }
                        }
                        assertEquals(16, capacity)
                        with(vehicleCapacity) {
                            assertNotNull(this)
                            assertEquals(3, size)
                            assertEquals(8, this["abc123"])
                            assertEquals(8, this["def456"])
                            assertEquals(16, this["ghi789"])
                        }
                    }
                }
            }
        }
    }
}