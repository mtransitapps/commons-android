package org.mtransit.android.commons.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.commons.CommonsApp;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
@RunWith(MockitoJUnitRunner.class)
public class GrandRiverTransitProviderTests {

	private static final String TZ = "America/Toronto";

	private static final String VEHICLE_ID = "vehicle_id";

	private static final Route DEFAULT_ROUTE = new Route(
			1,
			"1",
			"route 1",
			"blue",
			1,
			0
	);

	@Mock
	private Context context;

	private final GrandRiverTransitProvider provider = new GrandRiverTransitProvider();

	@Before
	public void setUp() {
		CommonsApp.setup(false);
	}

	@Test
	public void testParseAgencyJSONFirstAndLast() {
		// Arrange
		boolean noPickup = false;
		Direction direction = new Direction(
				1L,
				Direction.HEADSIGN_TYPE_STRING,
				"The Boardwalk",
				1
		);
		Stop stop = new Stop(
				1,
				"1",
				"Charles Term", // cleaned by parser
				0.0,
				0.0,
				0,
				1
		);
		RouteDirectionStop rds = getRouteDirectionStop(DEFAULT_ROUTE, direction, stop, noPickup);
		long newLastUpdateInMs = 1539268934000L; // October 11, 2018 10:42 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "The Boardwalk", "/Date(1539270000000)/")); // 11:00:00 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "Charles Terminal", "/Date(1539270549000)/")); // 11:09:09 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "The Boardwalk", "/Date(1539271800000)/")); // 11:30:00 AM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rds, null, newLastUpdateInMs, TZ);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(2, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONFirstAndLastNoPickup() {
		// Arrange
		boolean noPickup = true;
		Direction direction = new Direction(
				1L,
				Direction.HEADSIGN_TYPE_STRING,
				"Charles Term", // cleaned by parser
				1
		);
		Stop stop = new Stop(
				1,
				"1",
				"Charles Term", // cleaned by parser
				0.0,
				0.0,
				0,
				1
		);
		RouteDirectionStop rds = getRouteDirectionStop(DEFAULT_ROUTE, direction, stop, noPickup);
		long newLastUpdateInMs = 1539268934000L; // October 11, 2018 10:42 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "The Boardwalk", "/Date(1539270000000)/")); // 11:00:00 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "Charles Terminal", "/Date(1539270549000)/")); // 11:09:09 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "The Boardwalk", "/Date(1539271800000)/")); // 11:30:00 AM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rds, null, newLastUpdateInMs, TZ);
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
		boolean noPickup = false;
		Direction direction = new Direction(
				1L,
				Direction.HEADSIGN_TYPE_STRING,
				"The Boardwalk",
				1
		);
		Stop stop = new Stop(
				1,
				"1",
				"Columbia / Fischer-Hallman",
				0.0,
				0.0,
				0,
				1
		);
		RouteDirectionStop rds = getRouteDirectionStop(DEFAULT_ROUTE, direction, stop, noPickup);
		long newLastUpdateInMs = 1539272017000L; // October 11, 2018 11:33 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "Laurelwood/Erbsville", "/Date(1539272137000)/")); // 11:35:37 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "The Boardwalk", "/Date(1539272460000)/")); // 11:41:00 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "Laurelwood/Erbsville", "/Date(1539273780000)/")); // 12:03:00 PM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rds, null, newLastUpdateInMs, TZ);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(3, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONSplitCircleWithEmptyHeadSign() {
		// Arrange
		boolean noPickup = false;
		Direction direction = new Direction(
				1L,
				Direction.HEADSIGN_TYPE_STRING,
				"Ainslie Term", // cleaned by parser
				1
		);
		Stop stop = new Stop(
				1,
				"1",
				"Myers / Elgin",
				0.0,
				0.0,
				0,
				1
		);
		RouteDirectionStop rds = getRouteDirectionStop(DEFAULT_ROUTE, direction, stop, noPickup);
		long newLastUpdateInMs = 1539352980000L; // October 12, 2018 10:03 AM
		ArrayList<GrandRiverTransitProvider.JStopTime> jStopTimes = new ArrayList<>();
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "", "/Date(1539353766000)/")); // 10:16:06 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "", "/Date(1539355388000)/")); // 10:43:08 AM
		jStopTimes.add(new GrandRiverTransitProvider.JStopTime(VEHICLE_ID, "", "/Date(1539357180000)/")); // 11:13:00 AM
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSON(context, jStopTimes, rds, null, newLastUpdateInMs, TZ);
		// Assert
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(3, schedule.getTimestampsCount());
	}

	@NonNull
	@SuppressWarnings("SameParameterValue")
	private RouteDirectionStop getRouteDirectionStop(Route route, Direction direction, Stop stop, boolean noPickup) {
		return new RouteDirectionStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP,
				route,
				direction,
				stop,
				noPickup);
	}
}