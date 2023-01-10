package org.mtransit.android.commons.provider;

import org.junit.Before;
import org.junit.Test;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JRealTimeResult;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JStopTimeResult;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JStopTimeResult.JLine;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JStopTimeResult.JStopTime;
import org.mtransit.commons.CommonsApp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CaLTCOnlineProviderTest {

	private static final String AUTHORITY = "authority.test";

	private static final Route DEFAULT_ROUTE = new Route(1, "1", "route 1", "color");
	private static final Trip DEFAULT_TRIP = new Trip(1, Trip.HEADSIGN_TYPE_STRING, "trip 1", 1);
	private static final Stop DEFAULT_STOP = new Stop(1, "1", "stop 1", 0, 0, 0);

	private final CaLTCOnlineProvider provider = new CaLTCOnlineProvider();

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

	@Test
	public void testParseAgencyJSON() {
		// Arrange
		JBusTimes jBusTimes = new JBusTimes(Collections.singletonList(
				new JResult(
						Arrays.asList(
								new JRealTimeResult(52500, 37760, 52725, 32, 999013, true),
								new JRealTimeResult(52500, 37880, 52770, 32, 999554, true),
								new JRealTimeResult(52920, 37711, 52770, 32, 998671, true),
								new JRealTimeResult(53220, 37881, 53490, 32, 999592, true),
								new JRealTimeResult(54300, 37760, 54225, 32, 999012, true),
								new JRealTimeResult(54300, 37880, 54495, 32, 999555, true),
								new JRealTimeResult(54480, 37711, 54975, 32, 998670, true),
								new JRealTimeResult(54720, 37711, 55215, 32, 998670, true),
								new JRealTimeResult(55020, 37881, 55215, 32, 999593, true),
								new JRealTimeResult(55980, 37711, 55980, 32, 998669, true)
						),
						Collections.singletonList(
								new JStopTimeResult(
										Arrays.asList(
												new JLine("EASTBOUND   ", "07", 37760, 32),
												new JLine("WESTBOUND", "02", 37711, 32),
												new JLine("EASTBOUND   ", "17", 37880, 32),
												new JLine("WESTBOUND", "17", 37881, 32)
										),
										Arrays.asList(
												new JStopTime("Argyle Mall via York", 52500, 37760, "32", 999013),
												new JStopTime("Argyle Mall via Oxford", 52500, 37880, "32", 999554),
												new JStopTime("Natural Science via Dundas", 52920, 37711, "32", 998671),
												new JStopTime("Byron via Oxford", 53220, 37881, "32", 999592),
												new JStopTime("Argyle Mall via York", 54300, 37760, "32", 999012),
												new JStopTime("Argyle Mall via Oxford", 54300, 37880, "32", 999555),
												new JStopTime("Natural Science via Dundas", 54480, 37711, "32", 998670),
												new JStopTime("Natural Science via Dundas", 54720, 37711, "32", 998670),
												new JStopTime("Byron via Oxford", 55020, 37881, "32", 999593),
												new JStopTime("Natural Science via Dundas", 55980, 37711, "32", 998669)
										)
								)
						)
				)
		));
		String _7_E = CaLTCOnlineProvider.getAgencyRouteStopTargetUUID(AUTHORITY, "7", Trip.HEADING_EAST, "32");
		String _2_W = CaLTCOnlineProvider.getAgencyRouteStopTargetUUID(AUTHORITY, "2", Trip.HEADING_WEST, "32");
		String _17_E = CaLTCOnlineProvider.getAgencyRouteStopTargetUUID(AUTHORITY, "17", Trip.HEADING_EAST, "32");
		String _17_W = CaLTCOnlineProvider.getAgencyRouteStopTargetUUID(AUTHORITY, "17", Trip.HEADING_WEST, "32");
		List<String> expectedTargetUUIDs = Arrays.asList(_7_E, _2_W, _17_E, _17_W);
		long newLastUpdateInMs = 1544384312000L; // Sun, 09 Dec 2018 14:38:32 GMT-05:00
		long beginningOfTodayInMs = 1544331600000L; // Sun, 09 Dec 2018 00:00:00 GMT-05:00
		// Act
		List<POIStatus> result = provider.parseAgencyJSON(jBusTimes, rts, newLastUpdateInMs, beginningOfTodayInMs);
		// Assert
		assertEquals(4, result.size());
		for (POIStatus poiStatus : result) {
			assertTrue(poiStatus instanceof Schedule);
			Schedule schedule = (Schedule) poiStatus;
			String targetUUID = schedule.getTargetUUID();
			assertNotNull(targetUUID);
			assertTrue(expectedTargetUUIDs.contains(targetUUID));
			if (_2_W.equalsIgnoreCase(targetUUID)) {
				assertEquals(4, schedule.getTimestampsCount());
				assertEquals(
						"Natural Science Via Dundas",
						schedule.getTimestamps().get(0).getHeading());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(52770)),
						schedule.getTimestamps().get(0).getT());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(54975)),
						schedule.getTimestamps().get(1).getT());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(55215)),
						schedule.getTimestamps().get(2).getT());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(55980)),
						schedule.getTimestamps().get(3).getT());
			} else if (_7_E.equalsIgnoreCase(targetUUID)) {
				assertEquals(
						"Argyle Mall Via York",
						schedule.getTimestamps().get(0).getHeading());
				assertEquals(2, schedule.getTimestampsCount());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(52725)),
						schedule.getTimestamps().get(0).getT());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(54225)),
						schedule.getTimestamps().get(1).getT());
			} else if (_17_E.equalsIgnoreCase(targetUUID)) {
				assertEquals(
						"Argyle Mall Via Oxford",
						schedule.getTimestamps().get(0).getHeading());
				assertEquals(2, schedule.getTimestampsCount());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(52770)),
						schedule.getTimestamps().get(0).getT());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(54495)),
						schedule.getTimestamps().get(1).getT());
			} else if (_17_W.equalsIgnoreCase(targetUUID)) {
				assertEquals(
						"Byron Via Oxford",
						schedule.getTimestamps().get(0).getHeading());
				assertEquals(2, schedule.getTimestampsCount());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(53490)),
						schedule.getTimestamps().get(0).getT());
				assertEquals(
						TimeUtils.timeToTheTensSecondsMillis(beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(55215)),
						schedule.getTimestamps().get(1).getT());
			} else {
				fail("Unexpected target UUID'" + targetUUID + "'!");
			}
		}
	}
}