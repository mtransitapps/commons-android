package org.mtransit.android.commons;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeUtilsTest {

	@Test
	public void test_timeToTheMinuteMillis() {
		assertEquals(1746320400_000L, TimeUtils.timeToTheMinuteMillis(1746320400_000L));

		assertEquals(1746320400_000L, TimeUtils.timeToTheMinuteMillis(1746320401_000L));
	}

	@Test
	public void test_formatSimpleDuration() {
		assertEquals("1 days 2 h 3 min 4 sec 5 ms", TimeUtils.formatSimpleDuration(93784005));
		assertEquals("22 sec 915 ms", TimeUtils.formatSimpleDuration(22_915L));
		assertEquals("-1 min", TimeUtils.formatSimpleDuration(-60_000L));
	}
}