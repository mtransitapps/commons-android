package org.mtransit.android.commons.provider;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.StrategicMappingProvider.Group;
import org.mtransit.android.commons.provider.StrategicMappingProvider.Prediction;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class StrategicMappingProviderTests {

	private ThreadSafeDateFormatter timeFormatter = new ThreadSafeDateFormatter(StrategicMappingProvider.TIME_FORMAT, Locale.ENGLISH);

	@Test
	public void testParseAgencyJSONFilteringRoute() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Parksville"; // routeName
		String routeShortName = "88"; // routeCode
		Long tripId = 88011L;
		boolean descentOnly = false;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_OUTBOUND;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T08:05:52").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(StrategicMappingProvider.DIRECT_NAME_OUTBOUND, "Intercity", "91"),
				Collections.singletonList(
						new Prediction("2018-10-04T22:50:00", "2018-10-04T22:50:00", "Scheduled", 40)
				)
		);
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Collections.singletonList(
						new Prediction("2018-10-04T07:35:00", "2018-10-04T07:35:00", "Scheduled", 1)
				)
		);
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Collections.singletonList(
						new Prediction("2018-10-04T12:35:00", "2018-10-04T12:35:00", "Scheduled", 1)
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(2, schedule.getTimestampsCount());
		assertEquals("2018-10-04T07:35:00", timeFormatter.formatThreadSafe(schedule.getTimestamps().get(0).t));
		assertEquals("2018-10-04T12:35:00", timeFormatter.formatThreadSafe(schedule.getTimestamps().get(1).t));
	}

	@Test
	public void testParseAgencyJSONFilteringRouteTrip() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Downtown/Country Club"; // routeName
		String routeShortName = "1"; // routeCode
		Long tripId = 100L;
		boolean descentOnly = false;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_INBOUND;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T10:57:40").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Collections.singletonList(
						new Prediction("2018-10-04T11:07:00", "2018-10-04T11:07:00", "Predicted", 1)
				)
		);
		groupPredictionsMap.put(new Group(StrategicMappingProvider.DIRECT_NAME_OUTBOUND, routeLongName, routeShortName),
				Collections.singletonList(
						new Prediction("2018-10-04T11:01:00", "2018-10-04T11:01:00", "Predicted", 37)
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(1, schedule.getTimestampsCount());
		assertEquals("2018-10-04T11:07:00", timeFormatter.formatThreadSafe(schedule.getTimestamps().get(0).t));
	}

	@Test
	public void testParseAgencyJSONSplittedFirstAndOtherLastStop() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Parksville"; // routeName
		String routeShortName = "88"; // routeCode
		Long tripId = 88011L;
		boolean descentOnly = true;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_OUTBOUND;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T08:05:52").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T07:35:00", "2018-10-04T07:35:00", "Scheduled", 1),
						new Prediction("2018-10-04T05:00:31", "2018-10-04T08:30:00", "Scheduled", 1),
						new Prediction("2018-10-04T11:41:00", "2018-10-04T11:41:00", "Scheduled", 67),
						new Prediction("2018-10-04T12:35:00", "2018-10-04T12:35:00", "Scheduled", 1),
						new Prediction("2018-10-04T18:11:00", "2018-10-04T18:11:00", "Scheduled", 67),
						new Prediction("2018-10-04T18:58:00", "2018-10-04T18:58:00", "Scheduled", 1),
						new Prediction("2018-10-04T19:40:00", "2018-10-04T19:40:00", "Scheduled", 1),
						new Prediction("2018-10-04T20:16:00", "2018-10-04T20:16:00", "Scheduled", 67)
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(3, schedule.getTimestampsCount());
		assertTrue(schedule.getTimestamps().get(0).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(1).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(2).isDescentOnly());
	}

	@Test
	public void testParseAgencyJSONSplittedFirstAndOtherFirstStop() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Parksville"; // routeName
		String routeShortName = "88"; // routeCode
		Long tripId = 88010L;
		boolean descentOnly = false;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_OUTBOUND;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T08:05:52").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T07:35:00", "2018-10-04T07:35:00", "Scheduled", 1),
						new Prediction("2018-10-04T05:00:31", "2018-10-04T08:30:00", "Scheduled", 1),
						new Prediction("2018-10-04T11:41:00", "2018-10-04T11:41:00", "Scheduled", 67),
						new Prediction("2018-10-04T12:35:00", "2018-10-04T12:35:00", "Scheduled", 1),
						new Prediction("2018-10-04T18:11:00", "2018-10-04T18:11:00", "Scheduled", 67),
						new Prediction("2018-10-04T18:58:00", "2018-10-04T18:58:00", "Scheduled", 1),
						new Prediction("2018-10-04T19:40:00", "2018-10-04T19:40:00", "Scheduled", 1),
						new Prediction("2018-10-04T20:16:00", "2018-10-04T20:16:00", "Scheduled", 67)
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(5, schedule.getTimestampsCount());
		assertFalse(schedule.getTimestamps().get(0).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(1).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(2).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(3).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(4).isDescentOnly());
	}

	@Test
	public void testParseAgencyJSONCircleSplittedFirstAndOtherLastStop() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Cinnabar/Cedar"; // routeName
		String routeShortName = "7"; // routeCode
		Long tripId = 7090L;
		boolean descentOnly = true;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_COUNTERCLOCKWISE;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T10:32:42").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T12:48:00", "2018-10-04T12:48:00", "Predicted", 1),
						new Prediction("2018-10-04T14:04:00", "2018-10-04T13:55:00", "Predicted", 85),
						new Prediction("2018-10-04T15:56:00", "2018-10-04T15:56:00", "Scheduled", 1),
						new Prediction("2018-10-04T16:20:00", "2018-10-04T16:20:00", "Scheduled", 1),
						new Prediction("2018-10-04T16:53:00", "2018-10-04T16:53:00", "Scheduled", 1),
						new Prediction("2018-10-04T17:08:00", "2018-10-04T17:08:00", "Scheduled", 85)
						// ...
				)
		);
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T11:06:00", "2018-10-04T11:01:00", "Predicted", 56),
						new Prediction("2018-10-04T11:28:00", "2018-10-04T11:28:00", "Predicted", 1),
						new Prediction("2018-10-04T12:15:00", "2018-10-04T12:10:00", "Predicted", 56),
						new Prediction("2018-10-04T14:00:00", "2018-10-04T14:00:00", "Scheduled", 1),
						new Prediction("2018-10-04T14:44:00", "2018-10-04T14:44:00", "Scheduled", 56),
						new Prediction("2018-10-04T15:10:00", "2018-10-04T15:10:00", "Scheduled", 1),
						new Prediction("2018-10-04T15:55:00", "2018-10-04T15:55:00", "Scheduled", 56)
						// ...
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(6, schedule.getTimestampsCount());
		assertTrue(schedule.getTimestamps().get(0).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(1).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(2).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(3).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(4).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(5).isDescentOnly());
	}

	@Test
	public void testParseAgencyJSONCircleSplittedFirstAndOtherFirstStop() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Cinnabar/Cedar"; // routeName
		String routeShortName = "7"; // routeCode
		Long tripId = 7091L;
		boolean descentOnly = false;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_COUNTERCLOCKWISE;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T10:32:42").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T12:48:00", "2018-10-04T12:48:00", "Predicted", 1),
						new Prediction("2018-10-04T14:04:00", "2018-10-04T13:55:00", "Predicted", 85),
						new Prediction("2018-10-04T15:56:00", "2018-10-04T15:56:00", "Scheduled", 1),
						new Prediction("2018-10-04T16:20:00", "2018-10-04T16:20:00", "Scheduled", 1),
						new Prediction("2018-10-04T16:53:00", "2018-10-04T16:53:00", "Scheduled", 1),
						new Prediction("2018-10-04T17:08:00", "2018-10-04T17:08:00", "Scheduled", 85)
						// ...
				)
		);
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T11:06:00", "2018-10-04T11:01:00", "Predicted", 56),
						new Prediction("2018-10-04T11:28:00", "2018-10-04T11:28:00", "Predicted", 1),
						new Prediction("2018-10-04T12:15:00", "2018-10-04T12:10:00", "Predicted", 56),
						new Prediction("2018-10-04T14:00:00", "2018-10-04T14:00:00", "Scheduled", 1),
						new Prediction("2018-10-04T14:44:00", "2018-10-04T14:44:00", "Scheduled", 56),
						new Prediction("2018-10-04T15:10:00", "2018-10-04T15:10:00", "Scheduled", 1),
						new Prediction("2018-10-04T15:55:00", "2018-10-04T15:55:00", "Scheduled", 56)
						// ...
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(7, schedule.getTimestampsCount());
		assertFalse(schedule.getTimestamps().get(0).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(1).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(2).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(3).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(4).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(5).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(6).isDescentOnly());
	}

	@Test
	public void testParseAgencyJSONCircleNonSplittedFirstAndOtherFirstStop() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Cinnabar/Cedar"; // routeName
		String routeShortName = "7"; // routeCode
		Long tripId = 700L;
		boolean descentOnly = false;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_COUNTERCLOCKWISE;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T10:32:42").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T12:48:00", "2018-10-04T12:48:00", "Predicted", 1),
						new Prediction("2018-10-04T14:04:00", "2018-10-04T13:55:00", "Predicted", 85),
						new Prediction("2018-10-04T15:56:00", "2018-10-04T15:56:00", "Scheduled", 1),
						new Prediction("2018-10-04T16:20:00", "2018-10-04T16:20:00", "Scheduled", 1),
						new Prediction("2018-10-04T16:53:00", "2018-10-04T16:53:00", "Scheduled", 1),
						new Prediction("2018-10-04T17:08:00", "2018-10-04T17:08:00", "Scheduled", 85)
						// ...
				)
		);
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T11:06:00", "2018-10-04T11:01:00", "Predicted", 56),
						new Prediction("2018-10-04T11:28:00", "2018-10-04T11:28:00", "Predicted", 1),
						new Prediction("2018-10-04T12:15:00", "2018-10-04T12:10:00", "Predicted", 56),
						new Prediction("2018-10-04T14:00:00", "2018-10-04T14:00:00", "Scheduled", 1),
						new Prediction("2018-10-04T14:44:00", "2018-10-04T14:44:00", "Scheduled", 56),
						new Prediction("2018-10-04T15:10:00", "2018-10-04T15:10:00", "Scheduled", 1),
						new Prediction("2018-10-04T15:55:00", "2018-10-04T15:55:00", "Scheduled", 56)
						// ...
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(13, schedule.getTimestampsCount());
		assertTrue(schedule.getTimestamps().get(0).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(1).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(2).isDescentOnly());
		assertFalse(schedule.getTimestamps().get(3).isDescentOnly());
		// ...
	}

	@Test
	public void testParseAgencyJSONSingleTripDirectionNonSplittedLastStop() throws ParseException {
		StrategicMappingProvider provider = new StrategicMappingProvider();

		String routeLongName = "Hammond Bay"; // routeName
		String routeShortName = "92"; // routeCode
		Long tripId = 9200L;
		boolean descentOnly = true;
		String tripDirectName = StrategicMappingProvider.DIRECT_NAME_INBOUND;
		long newLastUpdateInMs = timeFormatter.parseThreadSafe("2018-10-04T11:09:07").getTime();

		Map<Group, List<Prediction>> groupPredictionsMap = new HashMap<>();
		groupPredictionsMap.put(new Group(tripDirectName, routeLongName, routeShortName),
				Arrays.asList(
						new Prediction("2018-10-04T15:42:00", "2018-10-04T15:42:00", "Scheduled", 30),
						new Prediction("2018-10-04T15:47:00", "2018-10-04T15:47:00", "Scheduled", 30)
				)
		);
		RouteTripStop rts = getRouteTripStop(routeLongName, routeShortName, tripId, descentOnly);

		Collection<POIStatus> result = provider.parseAgencyJSON(timeFormatter, groupPredictionsMap, rts, newLastUpdateInMs);
		assertEquals(1, result.size());
		POIStatus poiStatus = result.iterator().next();
		assertTrue(poiStatus instanceof Schedule);
		Schedule schedule = (Schedule) poiStatus;
		assertEquals(2, schedule.getTimestampsCount());
		assertTrue(schedule.getTimestamps().get(0).isDescentOnly());
		assertTrue(schedule.getTimestamps().get(1).isDescentOnly());
	}

	@NonNull
	private RouteTripStop getRouteTripStop(String routeLongName, String routeShortName, Long tripId, boolean descentOnly) {
		Route route = new Route();
		route.setShortName(routeShortName);
		route.setLongName(routeLongName);
		Trip trip = new Trip();
		trip.setId(tripId);
		return new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				route,
				trip,
				new Stop(),
				descentOnly);
	}
}