package org.mtransit.android.commons.data;

import org.junit.Test;
import org.mtransit.android.commons.ComparatorUtils;

import static junit.framework.Assert.assertTrue;

public class DefaultPOITests {

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testCompareToAlpha() {
		DefaultPOI thisPOI;
		DefaultPOI anotherPOI;
		//
		thisPOI = new DefaultPOI(null, -1, -1, -1, -1);
		thisPOI.setName("thisPOI");
		anotherPOI = null;
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(null, anotherPOI)));
		anotherPOI = new DefaultPOI(null, -1, -1, -1, -1);
		anotherPOI.setName("");
		assertTrue(ComparatorUtils.isAfter(thisPOI.compareToAlpha(null, anotherPOI)));
		anotherPOI.setName("zzzz");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(null, anotherPOI)));
		thisPOI.setName("aaaa");
		anotherPOI.setName("ZZZZ");
		assertTrue(ComparatorUtils.isBefore(thisPOI.compareToAlpha(null, anotherPOI)));
	}
}
