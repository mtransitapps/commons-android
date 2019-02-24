package org.mtransit.android.commons.provider;

import android.content.Context;
import android.support.annotation.NonNull;

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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ReginaTransitProviderTests {

	@Mock
	private Context context;

	@Test
	public void testParseAgencyJSONFilteringRoute() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "2";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_STRING;
		String tripHeadsign = "Argyle Pk";
		String stopCode = "9999";
		boolean descentOnly = false;
		long newLastUpdateInMs = 1539086255000L; // October 9, 2018 5:57 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("06:07 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:22 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:37 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:52 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:07 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:22 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:37 AM", "ARGYLE PARK", "0"));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(7, schedule.getTimestampsCount());
	}


	@Test
	public void testParseAgencyJSONFilteringRouteOtherDirection() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "2";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_STRING;
		String tripHeadsign = "Wood Mdws";
		String stopCode = "9999";
		boolean descentOnly = true;
		long newLastUpdateInMs = 1539086255000L; // October 9, 2018 6:57 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("06:07 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:22 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:37 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:52 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:07 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:22 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:37 AM", "ARGYLE PARK", "0"));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(7, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONFilteringRouteBothDirectionDescentOnly() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "2";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_STRING;
		String tripHeadsign = "Argyle Pk";
		String stopCode = "0528";
		boolean descentOnly = true;
		long newLastUpdateInMs = 1539088292000L; // October 9, 2018 6:31 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("06:45 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:45 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:00 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:15 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:15 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:30 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:30 AM", "ARGYLE PARK", "0"));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(7, schedule.getTimestampsCount());
		assertEquals(1539089100000L, schedule.getTimestamps().get(0).t); // 06:45 AM
		assertEquals(1539090900000L, schedule.getTimestamps().get(3).t); // 07:15 AM
		assertEquals(1539091800000L, schedule.getTimestamps().get(6).t); // 07:30 AM
		assertFalse(schedule.getTimestamps().get(0).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(1).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(2).isDescentOnly());
	}

	@Test
	public void testParseAgencyJSONFilteringRouteBothDirection() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "2";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_STRING;
		String tripHeadsign = "Argyle Pk";
		String stopCode = "0528";
		boolean descentOnly = false;
		long newLastUpdateInMs = 1539088292000L; // October 9, 2018 6:31 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("06:45 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("06:50 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:00 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:15 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:25 AM", "ARGYLE PARK", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:30 AM", "WOOD MEADOWS", "0"));
		jResults.add(new ReginaTransitProvider.JResult("07:35 AM", "ARGYLE PARK", "0"));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(7, schedule.getTimestampsCount());
		assertFalse(schedule.getTimestamps().get(0).hasHeadsign());
		assertEquals("Wood Mdws", schedule.getTimestamps().get(1).getHeading(context));
		assertEquals("Wood Mdws", schedule.getTimestamps().get(2).getHeading(context));
		assertEquals("Wood Mdws", schedule.getTimestamps().get(3).getHeading(context));
		assertFalse(schedule.getTimestamps().get(4).hasHeadsign());
		assertEquals("Wood Mdws", schedule.getTimestamps().get(5).getHeading(context));
		assertFalse(schedule.getTimestamps().get(6).hasHeadsign());
	}

	@Test
	public void testParseAgencyJSONSplittedCircleRouteTripHeadSignTypeDirection() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "15";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_DIRECTION;
		String tripHeadsign = Trip.HEADING_WEST;
		String stopCode = "0038";
		boolean descentOnly = true;
		long newLastUpdateInMs = 1539092404000L; // October 9, 2018 7:40 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("08:52 AM", "HERITAGE WEST", "0"));
		jResults.add(new ReginaTransitProvider.JResult("09:37 AM", "HERITAGE WEST", "0"));
		jResults.add(new ReginaTransitProvider.JResult("10:22 AM", "HERITAGE WEST", "0"));
		jResults.add(new ReginaTransitProvider.JResult("11:07 AM", "HERITAGE WEST", "0"));
		jResults.add(new ReginaTransitProvider.JResult("11:52 AM", "HERITAGE WEST", "0"));
		jResults.add(new ReginaTransitProvider.JResult("12:37 PM", "HERITAGE WEST", "0"));
		jResults.add(new ReginaTransitProvider.JResult("01:22 PM", "HERITAGE WEST", "0"));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(7, schedule.getTimestampsCount());
		assertFalse(schedule.getTimestamps().get(0).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(0).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(1).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(2).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(3).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(4).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(5).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(6).hasHeadsign());
	}

	@Test
	public void testParseAgencyJSONWithLastStop() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "10";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_STRING;
		String tripHeadsign = "RCMP";
		String stopCode = "0625"; // 1625
		boolean descentOnly = false;
		long newLastUpdateInMs = 1539092404000L; // October 9, 2018 7:40 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("08:17 AM", "NORMANVIEW", "0"));
		jResults.add(new ReginaTransitProvider.JResult("08:49 AM", "NORMANVIEW", "459"));
		jResults.add(new ReginaTransitProvider.JResult("03:20 PM", "NORMANVIEW", "0"));
		jResults.add(new ReginaTransitProvider.JResult("03:50 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("04:20 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("04:50 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("05:20 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("05:50 PM", "RCMP", "625"));
		jResults.add(new ReginaTransitProvider.JResult("06:20 PM", "RCMP", "625"));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(9, schedule.getTimestampsCount());
		assertEquals("Normanview", schedule.getTimestamps().get(0).getHeading(context));
		assertEquals("Normanview", schedule.getTimestamps().get(1).getHeading(context));
		assertEquals("Normanview", schedule.getTimestamps().get(2).getHeading(context));
		assertFalse(schedule.getTimestamps().get(3).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(4).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(5).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(6).hasHeadsign());
		assertTrue(schedule.getTimestamps().get(7).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(8).isDescentOnly());
	}

	@Test
	public void testParseAgencyJSONWithLastStopDescentOnly() {
		ReginaTransitProvider provider = new ReginaTransitProvider();

		String routeShortName = "10";
		int tripHeadsignType = Trip.HEADSIGN_TYPE_STRING;
		String tripHeadsign = "Normanview";
		boolean descentOnly = true;
		String stopCode = "1625";
		long newLastUpdateInMs = 1539092404000L; // October 9, 2018 7:40 AM
		RouteTripStop rts = getRouteTripStop(routeShortName, tripHeadsignType, tripHeadsign, stopCode, descentOnly);

		ArrayList<ReginaTransitProvider.JResult> jResults = new ArrayList<>();
		jResults.add(new ReginaTransitProvider.JResult("08:17 AM", "NORMANVIEW", "0"));
		jResults.add(new ReginaTransitProvider.JResult("08:49 AM", "NORMANVIEW", "459"));
		jResults.add(new ReginaTransitProvider.JResult("03:20 PM", "NORMANVIEW", "0"));
		jResults.add(new ReginaTransitProvider.JResult("03:50 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("04:20 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("04:50 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("05:20 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("05:20 PM", "RCMP", "0"));
		jResults.add(new ReginaTransitProvider.JResult("05:50 PM", "RCMP", stopCode));
		jResults.add(new ReginaTransitProvider.JResult("06:20 PM", "NORMANVIEW", stopCode));
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jResults, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(10, schedule.getTimestampsCount());
		assertFalse(schedule.getTimestamps().get(0).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(1).hasHeadsign());
		assertFalse(schedule.getTimestamps().get(2).hasHeadsign());
		assertEquals("Rcmp", schedule.getTimestamps().get(3).getHeading(context));
		assertEquals("Rcmp", schedule.getTimestamps().get(4).getHeading(context));
		assertEquals("Rcmp", schedule.getTimestamps().get(5).getHeading(context));
		assertEquals("Rcmp", schedule.getTimestamps().get(6).getHeading(context));
		assertEquals("Rcmp", schedule.getTimestamps().get(7).getHeading(context));
		assertTrue(schedule.getTimestamps().get(8).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(9).isDescentOnly());
	}

	@NonNull
	private RouteTripStop getRouteTripStop(String routeShortName, int tripHeadsignType, String tripHeadsign, String stopCode, boolean descentOnly) {
		Route route = new Route();
		route.setShortName(routeShortName);
		// route.setLongName(routeLongName);
		Trip trip = new Trip();
		// trip.setId(tripId);
		trip.setHeadsignType(tripHeadsignType);
		trip.setHeadsignValue(tripHeadsign);
		Stop stop = new Stop();
		stop.setCode(stopCode);
		return new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				route,
				trip,
				stop,
				descentOnly);
	}
}