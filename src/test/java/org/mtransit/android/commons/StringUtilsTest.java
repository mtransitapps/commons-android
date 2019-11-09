package org.mtransit.android.commons;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class StringUtilsTest {

	@Test
	public void oneLineOneSpaceNullString() {
		// Arrange
		String string = null;
		// Act
		String result = StringUtils.oneLineOneSpace(string);
		// Assert
		assertNull(result);
	}

	@Test
	public void oneLineOneSpaceEmptyString() {
		// Arrange
		String string = StringUtils.EMPTY;
		// Act
		String result = StringUtils.oneLineOneSpace(string);
		// Assert
		assertNotNull(result);
		assertEquals(StringUtils.EMPTY, result);
	}

	@Test
	public void oneLineOneSpace() {
		// Arrange
		String string = "This is a multiline "
				+ "\n" //
				+ "\n" //
				+ "\n" //
				+ "string with too much spacing "
				+ " " //
				+ " " //
				+ " " //
				+ "between words.";
		// Act
		String result = StringUtils.oneLineOneSpace(string);
		// Assert
		assertNotNull(result);
		assertEquals("This is a multiline "
				+ "string with too much spacing "
				+ "between words.", result);
	}
}