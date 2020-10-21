package org.mtransit.android.commons.provider;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

public class ReginaTransitProviderTest {

	@Test
	public void test_parseTime_09_19_PM() throws ParseException {
		// Arrange
		String timeString = "09:19 PM";
		long expected = ((21L * 60L + 19L) * 60L) * 1000L;
		// Act
		long result = ReginaTransitProvider.parseTime(timeString);
		// Assert
		assertEquals(expected, result);
	}
}