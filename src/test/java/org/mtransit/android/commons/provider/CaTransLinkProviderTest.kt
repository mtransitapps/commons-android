package org.mtransit.android.commons.provider

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mtransit.commons.CommonsApp
import org.mtransit.commons.Constants.EMPTY

class CaTransLinkProviderTest {

    @Before
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_keepOrRemoveVia_keepVia() {
        // Arrange
        val tripHeadsign = "Aaa via Bbb ccc"
        val tripHeading = "Aaa"
        val routeLongName = "Zzz"
        // Act
        val result = CaTransLinkProvider.keepOrRemoveVia(tripHeadsign, tripHeading, routeLongName)
        // Assert
        assertEquals("via Bbb ccc", result)
    }

    @Test
    fun test_keepOrRemoveVia_doNotKeepVia() {
        // Arrange
        val tripHeadsign = "Aaa via Bbb ccc"
        val tripHeading = "Mmm"
        val routeLongName = "Zzz"
        // Act
        val result = CaTransLinkProvider.keepOrRemoveVia(tripHeadsign, tripHeading, routeLongName)
        // Assert
        assertEquals("Aaa", result)
    }

    @Test
    fun test_keepOrRemoveVia_noVia() {
        // Arrange
        val tripHeadsign = "Aaa Bbb ccc"
        val tripHeading = "Mmm"
        val routeLongName = "Zzz"
        // Act
        val result = CaTransLinkProvider.keepOrRemoveVia(tripHeadsign, tripHeading, routeLongName)
        // Assert
        assertEquals(tripHeadsign, result)
    }

    @Test
    fun test_keepOrRemoveVia_onlyVia() {
        // Arrange
        val tripHeadsign = "via Bbb ccc"
        val tripHeading = "Mmm"
        val routeLongName = "Zzz"
        // Act
        val result = CaTransLinkProvider.keepOrRemoveVia(tripHeadsign, tripHeading, routeLongName)
        // Assert
        assertEquals(tripHeadsign, result)
    }

    @Test
    fun test_keepOrRemoveVia_empty() {
        // Arrange
        val tripHeadsign = EMPTY
        val tripHeading = "Mmm"
        val routeLongName = "Zzz"
        // Act
        val result = CaTransLinkProvider.keepOrRemoveVia(tripHeadsign, tripHeading, routeLongName)
        // Assert
        assertEquals(EMPTY, result)
    }
}