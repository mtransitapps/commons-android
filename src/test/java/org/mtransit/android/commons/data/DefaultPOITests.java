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
		thisPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1);
		thisPOI.setName("thisPOI");
		anotherPOI = null;
		//noinspection ConstantConditions
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(null, anotherPOI)));
		anotherPOI = new DefaultPOI("authority", -1, -1, -1, -1, -1);
		anotherPOI.setName("");
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(null, anotherPOI)));
		anotherPOI.setName("zzzz");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(null, anotherPOI)));
		thisPOI.setName("aaaa");
		anotherPOI.setName("ZZZZ");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(null, anotherPOI)));
	}
}
