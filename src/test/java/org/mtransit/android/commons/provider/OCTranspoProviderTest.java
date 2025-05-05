package org.mtransit.android.commons.provider;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
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
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult.JRoute;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult.JRoute.JRouteDirection;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult.JRoute.JRouteDirection.JTrips;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult.JRoute.JRouteDirection.JTrips.JTrip;
import org.mtransit.commons.CommonsApp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class OCTranspoProviderTest {

	private static final String AUTHORITY = "authority.test";

	private static final Route DEFAULT_ROUTE = new Route(1, "1", "route 1", "color");
	private static final Trip DEFAULT_TRIP = new Trip(1, Trip.HEADSIGN_TYPE_STRING, "trip 1", 1);
	private static final Stop DEFAULT_STOP = new Stop(1, "1", "stop 1", 0, 0, 0, 1);

	@Mock
	private Context context;

	private final OCTranspoProvider provider = new OCTranspoProvider();

	private RouteTripStop rts;

	@Before
	public void setUp() {
		CommonsApp.setup(false);
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				DEFAULT_STOP,
				false);
	}

	@After
	public void tearDown() {
		rts = null;
	}

	@Test
	public void testParseAgencyJSONArrivalsResults() {
		// Arrange
		JGetNextTripsForStop jGetNextTripsForStop = new JGetNextTripsForStop(new JGetNextTripsForStopResult(new JRoute(Arrays.asList(
				new JRouteDirection(
						"Greenboro",
						"20191221101219",
						new JTrips(Arrays.asList(
								new JTrip("Greenboro", "5", "-1"),
								new JTrip("Greenboro", "29", "-1"),
								new JTrip("Greenboro", "53", "-1")

						))
				),
				new JRouteDirection(
						"Rockcliffe",
						"20191221101219",
						new JTrips(
								Collections.emptyList()
						)
				)
		))));
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				new Trip(1, Trip.HEADSIGN_TYPE_STRING, "Greenboro", 1),
				DEFAULT_STOP,
				false);
		long lastUpdateInMs = 1576984339000L; // December 21, 2019 10:12:19 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, null, lastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		assertNotNull(schedule);
		assertEquals(3, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONArrivalsResults_OtherDirection() {
		// Arrange
		JGetNextTripsForStop jGetNextTripsForStop = new JGetNextTripsForStop(new JGetNextTripsForStopResult(new JRoute(Arrays.asList(
				new JRouteDirection(
						"Greenboro",
						"20191221101219",
						new JTrips(Arrays.asList(
								new JTrip("Greenboro", "5", "-1"),
								new JTrip("Greenboro", "29", "-1"),
								new JTrip("Greenboro", "53", "-1")

						))
				),
				new JRouteDirection(
						"Rockcliffe",
						"20191221101219",
						new JTrips(
								Collections.emptyList()
						)
				)
		))));
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				new Trip(1, Trip.HEADSIGN_TYPE_STRING, "Rockcliffe", 1),
				DEFAULT_STOP,
				true);
		long lastUpdateInMs = 1576984339000L; // December 21, 2019 10:12:19 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, null, lastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		assertNotNull(schedule);
		assertEquals(0, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONArrivalsResults_OneDirection() {
		// Arrange
		JGetNextTripsForStop jGetNextTripsForStop = new JGetNextTripsForStop(new JGetNextTripsForStopResult(new JRoute(Collections.singletonList(
				new JRouteDirection(
						"Greenboro",
						"20191221101219",
						new JTrips(Arrays.asList(
								new JTrip("Greenboro", "26", "0.18"),
								new JTrip("Greenboro", "49", "0.25"),
								new JTrip("Greenboro", "76", "0.50")

						))
				)
		))));
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				new Trip(1, Trip.HEADSIGN_TYPE_STRING, "Greenboro", 1),
				DEFAULT_STOP,
				false);
		long lastUpdateInMs = 1576984339000L; // December 21, 2019 10:12:19 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, null, lastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		assertNotNull(schedule);
		assertEquals(3, schedule.getTimestampsCount());
	}

	@Test
	public void testParseAgencyJSONArrivalsResults_TwoDirections() { // stop: Lyon #3051
		// Arrange
		JGetNextTripsForStop jGetNextTripsForStop = new JGetNextTripsForStop(new JGetNextTripsForStopResult(new JRoute(Arrays.asList(
				new JRouteDirection(
						"Blair",
						"20191221101210",
						new JTrips(Arrays.asList(
								new JTrip("Blair", "5", "-1"),
								new JTrip("Blair", "15", "-1"),
								new JTrip("Blair", "25", "-1"),
								new JTrip("Blair", "35", "-1")

						))
				),
				new JRouteDirection(
						"Tunney's Pasture",
						"20191221101210",
						new JTrips(Arrays.asList(
								new JTrip("Tunney's Pasture", "10", "-1"),
								new JTrip("Tunney's Pasture", "20", "-1"),
								new JTrip("Tunney's Pasture", "30", "-1")
						))
				)
		))));
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				new Trip(1, Trip.HEADSIGN_TYPE_STRING, "Tunney's Pasture", 1),
				DEFAULT_STOP,
				true);
		long lastUpdateInMs = 1576984320000L; // December 21, 2019 10:12:10 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, null, lastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		assertNotNull(schedule);
		assertEquals(3, schedule.getTimestampsCount());
		Schedule.Timestamp t0 = schedule.getTimestamps().get(0);
		assertEquals("20191221102210", OCTranspoProvider.getDateFormat(context).formatThreadSafe(t0.getT()));
	}
}