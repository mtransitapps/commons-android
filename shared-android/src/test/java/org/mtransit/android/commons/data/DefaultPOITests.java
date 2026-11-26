package org.mtransit.android.commons.data;

import org.junit.Before;
import org.junit.Test;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.commons.CommonsApp;

import static org.junit.Assert.assertTrue;
import static org.mtransit.android.commons.data.DefaultPOIExtKt.makeDefaultPOI;

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
		thisPOI = makeDefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "thisPOI");
		anotherPOI = null;
		//noinspection ConstantConditions
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
		anotherPOI = makeDefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "");
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(anotherPOI)));
		anotherPOI = makeDefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "zzzz");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
		thisPOI = makeDefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "aaaa");
		anotherPOI = makeDefaultPOI("authority", -1, -1, -1, -1, -1, 0.0, 0.0, "ZZZZ");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(anotherPOI)));
	}
}
