package org.mtransit.android.commons.data;

import org.junit.Before;
import org.junit.Test;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.commons.CommonsApp;

import static org.junit.Assert.assertTrue;

public class DefaultPOITests {

	@Before
	public void setUp() {
		CommonsApp.setup(false);
	}

	@Test
	public void testCompareToAlpha() {
		DefaultPOI thisPOI;
		DefaultPOI anotherPOI;
		//
		thisPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "thisPOI");
		anotherPOI = null;
		//noinspection ConstantConditions
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(anotherPOI)));
		anotherPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "");
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(anotherPOI)));
		anotherPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "zzzz");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
		thisPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "aaaa");
		anotherPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "ZZZZ");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
	}
}
