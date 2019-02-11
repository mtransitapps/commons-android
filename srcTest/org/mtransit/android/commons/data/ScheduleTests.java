package org.mtransit.android.commons.data;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mtransit.android.commons.TimeUtils;

import android.support.v4.util.Pair;

import static junit.framework.Assert.assertEquals;

public class ScheduleTests {

	public static final long PROVIDER_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	public static final long NOW_IN_MS = TimeUnit.SECONDS.toMillis(1534681140L); // August 19, 2018 8:19 EST
	public static final long AFTER_IN_MS = TimeUtils.timeToTheMinuteMillis(NOW_IN_MS);

	public static final Long MIN_COVERAGE_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	public static final Long MAX_COVERAGE_IN_MS = null;
	public static final Integer MIN_COUNT = 10;
	public static final Integer MAX_COUNT = null;

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetNextTimestamps() {
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-5L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.sortTimestamps();
		//
		ArrayList<Schedule.Timestamp> result = schedule.getNextTimestamps(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).t);
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).t);
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithUsefulLast() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		Pair<ArrayList<Long>, Boolean> resultPair = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		ArrayList<Long> result = resultPair.first;
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithUselessLast() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-10L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		Pair<ArrayList<Long>, Boolean> resultPair = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		ArrayList<Long> result = resultPair.first;
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithMultipleUsefulLast() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-3L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		Pair<ArrayList<Long>, Boolean> resultPair = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		ArrayList<Long> result = resultPair.first;
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithUsefulLastDuplicates() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		// schedule.addTimestampWithoutSort(new Schedule.Timestamp(now + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Long> result = Schedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT).first);
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithUsefulNextDuplicates() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-20L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Long> result = Schedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT).first);
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithUsefulNearDuplicates() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L) - 1L));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L) + 1L));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Long> result = Schedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT).first);
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithUsefulNextNearDuplicates() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-10L) - 1L));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-10L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L) + 1L));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Long> result = Schedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT).first);
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithBusAtStop() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(0L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(3L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Long> result = Schedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT).first);
		//
		assertEquals(5, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(0L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(3L), result.get(1).longValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testGetStatusNextTimestampsWithNearDuplicates() {
		//
		Schedule schedule = new Schedule(null, null, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(2L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(3L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(4L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Schedule.Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Long> result = Schedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT).first);
		//
		assertEquals(7, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).longValue());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(2L), result.get(1).longValue());
	}
}
