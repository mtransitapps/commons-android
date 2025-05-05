package org.mtransit.android.commons;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeUtilsTest {

	@Test
	public void test_timeToTheMinuteMillis() {
		assertEquals(1746320400_000L, TimeUtils.timeToTheMinuteMillis(1746320400_000L));

		assertEquals(1746320400_000L, TimeUtils.timeToTheMinuteMillis(1746320401_000L));
	}
}