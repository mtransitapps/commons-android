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
		assertEquals("1 day 2 hour 3 min 4 sec 5 milli", TimeUtils.formatSimpleDuration(93784005));
		assertEquals("22s 915ms", TimeUtils.formatSimpleDuration(22_915L, true));
		assertEquals("-1 min", TimeUtils.formatSimpleDuration(-60_000L));
	}
}