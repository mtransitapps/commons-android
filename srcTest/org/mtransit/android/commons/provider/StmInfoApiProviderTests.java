package org.mtransit.android.commons.provider;

import android.content.res.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class StmInfoApiProviderTests {

	@Mock
	private Resources resources;

	@Test
	public void testParseAgencyJSON() {
		StmInfoApiProvider provider = new StmInfoApiProvider();
		RouteTripStop rts = new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(),
				new Trip(),
				new Stop(),
				false);
		long newLastUpdateInMs = 1533067200000L; // 1600 (4:00 pm)
		ArrayList<StmInfoApiProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new StmInfoApiProvider.JResult("8", true, false)); // 1608 (4:08 pm)
		jResults.add(new StmInfoApiProvider.JResult("26", true, false)); // 1626 (4:26 pm)
		jResults.add(new StmInfoApiProvider.JResult("40", true, false)); // 1640 (4:40 pm)
		jResults.add(new StmInfoApiProvider.JResult("55", true, false)); // 1655 (4:55 pm)
		jResults.add(new StmInfoApiProvider.JResult("1713", false, false)); // 73 (5:13 pm)
		jResults.add(new StmInfoApiProvider.JResult("1723", false, false)); // 83 (5:23 pm)
		jResults.add(new StmInfoApiProvider.JResult("1500", false, false)); // 82800 (3:00 pm, tomorrow)
		jResults.add(new StmInfoApiProvider.JResult("1616", false, false)); // 87360 (4:16 pm, tomorrow)
		Collection<POIStatus> result = provider.parseAgencyJSON(resources, jResults, rts, newLastUpdateInMs);
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		ArrayList<Schedule.Timestamp> timestamps = schedule.getTimestamps();
		assertEquals(jResults.size(), timestamps.size());
		assertEquals(1533067680000L, timestamps.get(0).t);
		assertEquals(1533068760000L, timestamps.get(1).t);
		assertEquals(1533069600000L, timestamps.get(2).t);
		assertEquals(1533070500000L, timestamps.get(3).t);
		assertEquals(1533071580000L, timestamps.get(4).t);
		assertEquals(1533072180000L, timestamps.get(5).t);
		assertEquals(1533150000000L, timestamps.get(6).t);
		assertEquals(1533154560000L, timestamps.get(7).t);
	}

	@Test
	public void testParseAgencyJSONRealTimeNotInMinute() {
		StmInfoApiProvider provider = new StmInfoApiProvider();
		RouteTripStop rts = new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(),
				new Trip(),
				new Stop(),
				false);
		long newLastUpdateInMs = 1533067200000L; // 1600 (4:00 pm)
		ArrayList<StmInfoApiProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new StmInfoApiProvider.JResult("1608", true, false)); // 8 (4:08 pm)
		jResults.add(new StmInfoApiProvider.JResult("26", true, false)); // 1626 (4:26 pm)
		jResults.add(new StmInfoApiProvider.JResult("1640", true, true)); // 40 (4:40 pm)
		jResults.add(new StmInfoApiProvider.JResult("55", true, false)); // 1655 (4:55 pm)
		jResults.add(new StmInfoApiProvider.JResult("1713", false, true)); // 73 (5:13 pm)
		jResults.add(new StmInfoApiProvider.JResult("1723", false, false)); // 83 (5:23 pm)
		Collection<POIStatus> result = provider.parseAgencyJSON(resources, jResults, rts, newLastUpdateInMs);
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		ArrayList<Schedule.Timestamp> timestamps = schedule.getTimestamps();
		assertEquals(jResults.size(), timestamps.size());
		assertEquals(1533067680000L, timestamps.get(0).t);
		assertEquals(1533068760000L, timestamps.get(1).t);
		assertEquals(1533069600000L, timestamps.get(2).t);
		assertEquals(1533070500000L, timestamps.get(3).t);
		assertEquals(1533071580000L, timestamps.get(4).t);
		assertEquals(1533072180000L, timestamps.get(5).t);
	}

	@Test
	public void testParseAgencyJSONWithoutRealTime() {
		StmInfoApiProvider provider = new StmInfoApiProvider();
		RouteTripStop rts = new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(),
				new Trip(),
				new Stop(),
				false);
		long newLastUpdateInMs = 1533067200000L; // 1600 (4:00 pm)
		ArrayList<StmInfoApiProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new StmInfoApiProvider.JResult("1500", false, false)); // 82800 (3:00 pm, tomorrow)
		jResults.add(new StmInfoApiProvider.JResult("1616", false, false)); // 87360 (4:16 pm, tomorrow)
		Collection<POIStatus> result = provider.parseAgencyJSON(resources, jResults, rts, newLastUpdateInMs);
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void testParseAgencyJSONInThePastCongestion() {
		StmInfoApiProvider provider = new StmInfoApiProvider();
		RouteTripStop rts = new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(),
				new Trip(),
				new Stop(),
				false);
		long newLastUpdateInMs = 1538775792000L; // 1747 (5:43 pm)
		ArrayList<StmInfoApiProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new StmInfoApiProvider.JResult("1741", false, true)); // -2 (5:41 pm, today)
		jResults.add(new StmInfoApiProvider.JResult("1815", false, false)); // 28 (6:15 pm, today)
		jResults.add(new StmInfoApiProvider.JResult("1849", false, false)); // 62 (6:49 pm, today)
		jResults.add(new StmInfoApiProvider.JResult("1923", false, false)); // 96 (7:23 pm, today)
		jResults.add(new StmInfoApiProvider.JResult("1956", false, false)); // 129 (7:56 pm, today)
		Collection<POIStatus> result = provider.parseAgencyJSON(resources, jResults, rts, newLastUpdateInMs);
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		ArrayList<Schedule.Timestamp> timestamps = schedule.getTimestamps();
		assertEquals(jResults.size(), timestamps.size());
		assertTrue(timestamps.get(0).hasHeadsign());
		assertEquals(1538775660000L, timestamps.get(0).t);
		assertEquals(1538777700000L, timestamps.get(1).t);
		assertEquals(1538779740000L, timestamps.get(2).t);
		assertEquals(1538781780000L, timestamps.get(3).t);
		assertEquals(1538783760000L, timestamps.get(4).t);
	}
}