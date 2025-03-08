package org.mtransit.android.commons.provider

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.data.Stop
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.GreaterSudburyProvider.SudburyTransitApiV2
import org.mtransit.android.commons.provider.GreaterSudburyProvider.SudburyTransitApiV2.JCall
import org.mtransit.commons.CommonsApp
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class GreaterSudburyProviderTests {

    companion object {
        private const val AUTHORITY = "authority.test"

        private val DEFAULT_ID = 111;
        private val DEFAULT_ROUTE = Route(1, "1", "route 1", "color", 1, 0)
        private val DEFAULT_TRIP = Trip(1, Trip.HEADSIGN_TYPE_STRING, "Trip 1", 1)
        private val DEFAULT_STOP = Stop(1, "1", "stop 1", 0.0, 0.0, 0, 1)
    }

    @Mock
    private lateinit var context: Context

    private val provider = GreaterSudburyProvider()

    @Before
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_pickDestination_Simple() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_001_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_005_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(123, result)
    }

    @Test
    fun test_pickDestination_Multiple() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_001_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_006_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(555, result)
    }

    @Test
    fun test_pickDestination_Multiple_SameTimes() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_006_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(555, result)
    }

    @Test
    fun test_pickDestination_Multiple_SameTime_Reversed() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_006_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(555, result)
    }

    @Test
    fun test_pickDestination_Multiple_SameTimes_Reversed() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = "other head-sign" }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = "other head-sign" }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_005_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(555, result)
    }

    @Test
    fun test_pickDestination_Multiple_AlreadyArrived() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_001_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_006_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_010_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(123, result)
    }

    @Test
    fun test_pickDestination_Multiple_DropOffOnly() {
        // Arrange
        val dropOffOnly = true
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = "other head-sign" }
                        this.passingTime = Date(1_001_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = "other head-sign" }
                        this.passingTime = Date(1_006_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(123, result)
    }

    @Test
    fun test_pickDestination_Multiple_DropOffOnly_AlreadyArrived() {
        // Arrange
        val dropOffOnly = true
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = "other head-sign" }
                        this.passingTime = Date(1_001_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = "other head-sign" }
                        this.passingTime = Date(1_006_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_010_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(555, result)
    }

    @Test
    fun test_pickDestination_Multiple_OtherRoutes() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = "otherRoute"
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_001_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_005_000L)
                    },
                    JCall().apply {
                        this.route = "otherRoute"
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_006_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(123, result)
    }

    @Test
    fun test_pickDestination_Multiple_2_PassingTimes() {
        // Arrange
        val dropOffOnly = false
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123 }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555 }
                        this.passingTime = Date(1_001_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(555, result)
    }

    @Test
    fun test_pickDestination_Multiple_2_PassingTimes_DropOffOnly() {
        // Arrange
        val dropOffOnly = true
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_000_000L)
                    },
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 555; this.name = "other head-sign" }
                        this.passingTime = Date(1_001_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(123, result)
    }

    @Test
    fun test_pickDestination_Multiple_1_PassingTimes_DropOffOnly() {
        // Arrange
        val dropOffOnly = true
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = DEFAULT_TRIP.headsignValue }
                        this.passingTime = Date(1_000_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(123, result)
    }

    @Test
    fun test_pickDestination_Multiple_1_PassingTimes_DropOffOnly_Other() {
        // Arrange
        val dropOffOnly = true
        val rts = RouteTripStop(AUTHORITY, DEFAULT_ID, POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP, DEFAULT_ROUTE, DEFAULT_TRIP, DEFAULT_STOP, dropOffOnly)
        val jStopResponse = SudburyTransitApiV2.JStopResponse().apply {
            stop = SudburyTransitApiV2.JStop().apply {
                calls = arrayListOf<JCall>(
                    JCall().apply {
                        this.route = DEFAULT_ROUTE.shortName
                        this.destination = SudburyTransitApiV2.JDestination().apply { this.number = 123; this.name = "other head-sign" }
                        this.passingTime = Date(1_000_000L)
                    },
                )
            }
        }
        // Act
        val result = provider.pickRTSDestination(context, jStopResponse, rts)
        // Assert
        assertEquals(-1, result)
    }
}