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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class OCTranspoProviderTest {

	private static final String AUTHORITY = "authority.test";

	@Mock
	private Context context;

	private OCTranspoProvider provider = new OCTranspoProvider();

	private RouteTripStop rts;

	@Before
	public void setUp() {
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(),
				new Trip(),
				new Stop(),
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
		rts.getTrip().setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		rts.getTrip().setHeadsignValue("Greenboro");
		long lastUpdateInMs = 1576984339000L; // December 21, 2019 10:12:19 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, lastUpdateInMs);
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
		rts.getTrip().setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		rts.getTrip().setHeadsignValue("Rockcliffe");
		long lastUpdateInMs = 1576984339000L; // December 21, 2019 10:12:19 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, lastUpdateInMs);
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
		rts.getTrip().setHeadsignType(Trip.HEADSIGN_TYPE_STRING);
		rts.getTrip().setHeadsignValue("Greenboro");
		long lastUpdateInMs = 1576984339000L; // December 21, 2019 10:12:19 PM GMT-05:00
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, lastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		assertNotNull(schedule);
		assertEquals(3, schedule.getTimestampsCount());
	}
}