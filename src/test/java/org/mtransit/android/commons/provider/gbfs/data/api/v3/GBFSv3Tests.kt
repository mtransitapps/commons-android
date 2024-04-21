package org.mtransit.android.commons.provider.gbfs.data.api.v3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSv3Parser
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSFileTypeApiModel
import org.mtransit.commons.CommonsApp
import java.util.Date

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

        assertNotNull(result)
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
}