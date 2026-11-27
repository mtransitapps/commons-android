package org.mtransit.android.commons.data;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.commons.CommonsApp;

import static org.junit.Assert.assertTrue;
import static org.mtransit.android.commons.data.RouteDirectionStopTestFixturesKt.mkDefaultPOI;

public class DefaultPOITests {

	private static final String AUTHORITY = "authority";

	@Before
	public void setUp() {
		CommonsApp.setup(false);
	}

	@Test
	public void testCompareToAlpha() {
		DefaultPOI thisPOI;
		DefaultPOI anotherPOI;
		//
		thisPOI = mkPOI("thisPOI");
		anotherPOI = null;
		//noinspection ConstantConditions
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
		anotherPOI = mkPOI("");
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(anotherPOI)));
		anotherPOI = mkPOI("zzzz");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
		thisPOI = mkPOI("aaaa");
		anotherPOI = mkPOI("ZZZZ");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
	}

	private @NotNull DefaultPOI mkPOI(String name) {
		return mkDefaultPOI(AUTHORITY, -1, -1, -1, -1, -1, 0.0, 0.0, name);
	}
}
