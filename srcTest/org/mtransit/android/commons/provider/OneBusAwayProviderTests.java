package org.mtransit.android.commons.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;

import android.content.Context;
import android.support.annotation.NonNull;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OneBusAwayProviderTests {

	@Mock
	private Context context;

	@Test
	public void testIsSameRoute() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "50";
		long routeId = 50L;
		long tripId = -1L;
		String jRouteId = "YRT_50";
		String jRouteLongName = "QUEENSWAY";
		String jRouteShortName = "50";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "50";
		long routeId = 50L;
		long tripId = -1L;
		String jRouteId = "YRT_96";
		String jRouteLongName = "KEELE - YONGE";
		String jRouteShortName = "96";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertFalse(result);
	}


	@Test
	public void testIsSameRouteDoubleRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "98|99";
		long routeId = 9899L;
		long tripId = -1L;
		String jRouteId = "YRT_9899";
		String jRouteLongName = "YONGE";
		String jRouteShortName = "98|99";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteDoubleRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "98|99";
		long routeId = 9899L;
		long tripId = -1L;
		String jRouteId = "YRT_99";
		String jRouteLongName = "YONGE";
		String jRouteShortName = "99";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteEndsWithAZRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "4A";
		long routeId = 10004L;
		long tripId = -1L;
		String jRouteId = "YRT_4001";
		String jRouteLongName = "MAJOR MACKENZIE";
		String jRouteShortName = "4A";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteEndsWithAZRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "4A";
		long routeId = 10004L;
		long tripId = -1L;
		String jRouteId = "YRT_4";
		String jRouteLongName = "MAJOR MACKENZIE";
		String jRouteShortName = "4";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteAZRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "Blue-A";
		long routeId = 602L;
		long tripId = -1L;
		String jRouteId = "YRT_602";
		String jRouteLongName = "VIVA BLUE-A";
		String jRouteShortName = "blue A";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteAZRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "Blue-A";
		long routeId = 602L;
		long tripId = -1L;
		String jRouteId = "YRT_601";
		String jRouteLongName = "VIVA BLUE";
		String jRouteShortName = "blue";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteLongName, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameTripSplittedTrip() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		Route route = new Route();
		route.setId(21L);
		route.setShortName("21");
		Trip trip = new Trip();
		trip.setId(2103000L);
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("Vaughan Mills Terminal");
		RouteTripStop rts = getRouteTripStop(route, trip, new Stop(), false);
		String jTripHeadsign = "21 Vellore Local - AF";

		boolean result = provider.isSameTrip(rts, jTripHeadsign);
		assertTrue(result);
	}

	@Test
	public void testIsSameTripSplittedTripFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		Route route = new Route();
		route.setId(21L);
		route.setShortName("21");
		Trip trip = new Trip();
		trip.setId(2100L);
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("Vaughan Mills Terminal");
		RouteTripStop rts = getRouteTripStop(route, trip, new Stop(), false);
		String jTripHeadsign = "21 Vellore Local - AF";

		boolean result = provider.isSameTrip(rts, jTripHeadsign);
		assertFalse(result);
	}

	@Test
	public void testCleanTripHeadsignRTS() {
		when(context.getString(anyInt())).thenReturn("context string");

		OneBusAwayProvider provider = new OneBusAwayProvider();


		String tripHeadsign = "Martin Grv Via Vaughan Metropolitan Ctr";
		Trip trip = new Trip();
		trip.setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		trip.setHeadsignValue("Martin Grv");
		RouteTripStop rts = getRouteTripStop(new Route(), trip, new Stop(), false);

		String result = provider.cleanTripHeadsign(context, tripHeadsign, rts);

		assertEquals("Via Vaughan Metropolitan Ctr", result);
	}

	@NonNull
	private RouteTripStop getRouteTripStop(String routeShortName, long routeId, long tripId) {
		Route route = new Route();
		route.setId(routeId);
		route.setShortName(routeShortName);
		Trip trip = new Trip();
		trip.setId(tripId);
		Stop stop = new Stop();
		boolean descentOnly = false;
		//noinspection ConstantConditions
		return getRouteTripStop(route, trip, stop, descentOnly);
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