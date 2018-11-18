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
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
@RunWith(MockitoJUnitRunner.class)
public class GrandRiverTransitProviderTests {

	@Mock
	private Context context;

	private GrandRiverTransitProvider provider = new GrandRiverTransitProvider();

	@Test
	public void testParseAgencyJSONFirstAndLast() {
		// Arrange
		boolean descentOnly = false;
		Trip trip = new Trip();
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("The Boardwalk");
		Stop stop = new Stop();
		stop.setName("Charles Terminal");
		RouteTripStop rts = getRouteTripStop(new Route(), new Trip(), stop, descentOnly);
		long newLastUpdateInMs = 1539268934000L; // October 11, 2018 10:42 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("The Boardwalk", "/Date(1539270000000)/")); // 11:00:00 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("Charles Terminal", "/Date(1539270549000)/")); // 11:09:09 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("The Boardwalk", "/Date(1539271800000)/")); // 11:30:00 AM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rts, newLastUpdateInMs);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(2, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONFirstAndLastDescentOnly() {
		// Arrange
		boolean descentOnly = true;
		Trip trip = new Trip();
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("Charles Terminal");
		Stop stop = new Stop();
		stop.setName("Charles Terminal");
		RouteTripStop rts = getRouteTripStop(new Route(), trip, stop, descentOnly);
		long newLastUpdateInMs = 1539268934000L; // October 11, 2018 10:42 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("The Boardwalk", "/Date(1539270000000)/")); // 11:00:00 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("Charles Terminal", "/Date(1539270549000)/")); // 11:09:09 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("The Boardwalk", "/Date(1539271800000)/")); // 11:30:00 AM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rts, newLastUpdateInMs);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(1, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONSameTripDirectionWithDifferentHeadSign() {
		// Arrange
		boolean descentOnly = false;
		Trip trip = new Trip();
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("The Boardwalk");
		Stop stop = new Stop();
		stop.setName("Columbia / Fischer-Hallman");
		RouteTripStop rts = getRouteTripStop(new Route(), trip, stop, descentOnly);
		long newLastUpdateInMs = 1539272017000L; // October 11, 2018 11:33 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("Laurelwood/Erbsville", "/Date(1539272137000)/")); // 11:35:37 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("The Boardwalk", "/Date(1539272460000)/")); // 11:41:00 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("Laurelwood/Erbsville", "/Date(1539273780000)/")); // 12:03:00 PM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rts, newLastUpdateInMs);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(3, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONSplittedCircleWithEmptyHeadSign() {
		// Arrange
		boolean descentOnly = false;
		Trip trip = new Trip();
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("Ainslie Terminal");
		Stop stop = new Stop();
		stop.setName("Myers / Elgin");
		RouteTripStop rts = getRouteTripStop(new Route(), trip, stop, descentOnly);
		long newLastUpdateInMs = 1539352980000L; // October 12, 2018 10:03 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("", "/Date(1539353766000)/")); // 10:16:06 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("", "/Date(1539355388000)/")); // 10:43:08 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime("", "/Date(1539357180000)/")); // 11:13:00 AM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rts, newLastUpdateInMs);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(3, schedule.getTimestampsCount());
	}

	@NonNull
	private RouteTripStop getRouteTripStop(Route route, Trip trip, Stop stop, boolean descentOnly) {
		return new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				route,
				trip,
				stop,
				descentOnly);
	}
}