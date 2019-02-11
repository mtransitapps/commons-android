package org.mtransit.android.commons.provider;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class CaTransLinkProviderTest {

	@Test
	public void testDATE_FORMATTER_UTC() throws ParseException {
		// Arrange
		String time = "10:28am";
		// Act
		Date result = CaTransLinkProvider.DATE_FORMATTER_UTC.parseThreadSafe(time);
		// Assert
		System.out.println(result);
	}
}