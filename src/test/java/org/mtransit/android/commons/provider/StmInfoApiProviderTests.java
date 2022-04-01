package org.mtransit.android.commons.provider;

import android.content.res.Resources;

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
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.StmInfoApiProvider.JArrivals;
import org.mtransit.android.commons.provider.StmInfoApiProvider.JMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class StmInfoApiProviderTests {

	private static final String AUTHORITY = "authority.test";
	private static final String CODE_MESSAGE = "Message";

	private static final Route DEFAULT_ROUTE = new Route(1, "1", "route 1", "color");
	private static final Trip DEFAULT_TRIP = new Trip(1, Trip.HEADSIGN_TYPE_STRING, "trip 1", 1);
	private static final Stop DEFAULT_STOP = new Stop(1, "1", "stop 1", 0, 0);

	@Mock
	private Resources resources;

	private final StmInfoApiProvider provider = new StmInfoApiProvider();

	private RouteTripStop rts;

	@Before
	public void setUp() {
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
		long newLastUpdateInMs = 1533067200000L; // 1600 (4:00 pm)
		List<JArrivals.JResult> jResults = new ArrayList<>();
		jResults.add(new JArrivals.JResult("8", true, false, true)); // 1608 (4:08 pm)
		jResults.add(new JArrivals.JResult("26", true, false, true)); // 1626 (4:26 pm)
		jResults.add(new JArrivals.JResult("40", true, false, true)); // 1640 (4:40 pm)
		jResults.add(new JArrivals.JResult("55", true, false, true)); // 1655 (4:55 pm)
		jResults.add(new JArrivals.JResult("1713", false, false, true)); // 73 (5:13 pm)
		jResults.add(new JArrivals.JResult("1723", false, false, true)); // 83 (5:23 pm)
		jResults.add(new JArrivals.JResult("1500", false, false, false)); // 82800 (3:00 pm, tomorrow)
		jResults.add(new JArrivals.JResult("1616", false, false, false)); // 87360 (4:16 pm, tomorrow)
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(resources, jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		List<Schedule.Timestamp> timestamps = schedule.getTimestamps();
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
	public void testParseAgencyJSONArrivalsResultsRealTimeNotInMinute() {
		// Arrange
		long newLastUpdateInMs = 1533067200000L; // 1600 (4:00 pm)
		List<JArrivals.JResult> jResults = new ArrayList<>();
		jResults.add(new JArrivals.JResult("1608", true, false, true)); // 8 (4:08 pm)
		jResults.add(new JArrivals.JResult("26", true, false, true)); // 1626 (4:26 pm)
		jResults.add(new JArrivals.JResult("1640", true, true, true)); // 40 (4:40 pm)
		jResults.add(new JArrivals.JResult("55", true, false, true)); // 1655 (4:55 pm)
		jResults.add(new JArrivals.JResult("1713", false, true, true)); // 73 (5:13 pm)
		jResults.add(new JArrivals.JResult("1723", false, false, true)); // 83 (5:23 pm)
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(resources, jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		List<Schedule.Timestamp> timestamps = schedule.getTimestamps();
		assertEquals(jResults.size(), timestamps.size());
		assertEquals(1533067680000L, timestamps.get(0).t);
		assertEquals(1533068760000L, timestamps.get(1).t);
		assertEquals(1533069600000L, timestamps.get(2).t);
		assertEquals(1533070500000L, timestamps.get(3).t);
		assertEquals(1533071580000L, timestamps.get(4).t);
		assertEquals(1533072180000L, timestamps.get(5).t);
	}

	@Test
	public void testParseAgencyJSONArrivalsResultsWithoutRealTime() {
		// Arrange
		long newLastUpdateInMs = 1533067200000L; // 1600 (4:00 pm)
		List<JArrivals.JResult> jResults = new ArrayList<>();
		jResults.add(new JArrivals.JResult("1500", false, false, false)); // 82800 (3:00 pm, tomorrow)
		jResults.add(new JArrivals.JResult("1616", false, false, false)); // 87360 (4:16 pm, tomorrow)
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(resources, jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void testParseAgencyJSONArrivalsResultsInThePastCongestion() {
		// Arrange
		long newLastUpdateInMs = 1538775792000L; // 1747 (5:43 pm)
		List<JArrivals.JResult> jResults = new ArrayList<>();
		jResults.add(new JArrivals.JResult("1741", false, true, true)); // -2 (5:41 pm, today)
		jResults.add(new JArrivals.JResult("1815", false, false, true)); // 28 (6:15 pm, today)
		jResults.add(new JArrivals.JResult("1849", false, false, true)); // 62 (6:49 pm, today)
		jResults.add(new JArrivals.JResult("1923", false, false, true)); // 96 (7:23 pm, today)
		jResults.add(new JArrivals.JResult("1956", false, false, true)); // 129 (7:56 pm, today)
		// Act
		Collection<POIStatus> result = provider.parseAgencyJSONArrivalsResults(resources, jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		Schedule schedule = ((Schedule) result.iterator().next());
		List<Schedule.Timestamp> timestamps = schedule.getTimestamps();
		assertEquals(jResults.size(), timestamps.size());
		assertTrue(timestamps.get(0).hasHeadsign());
		assertEquals(1538775660000L, timestamps.get(0).t);
		assertEquals(1538777700000L, timestamps.get(1).t);
		assertEquals(1538779740000L, timestamps.get(2).t);
		assertEquals(1538781780000L, timestamps.get(3).t);
		assertEquals(1538783760000L, timestamps.get(4).t);
	}

	@Test
	public void testParseAgencyJSONMessageResults() {
		// Arrange
		String routeShortName = "10";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(1, routeShortName, "route 1", "color"),
				DEFAULT_TRIP,
				DEFAULT_STOP,
				false);
		long newLastUpdateInMs = System.currentTimeMillis();
		List<JMessages.JResult> jResults = new ArrayList<>();
		ArrayList<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRoutes = new ArrayList<>();
		HashMap<String, List<JMessages.JResult.JResultRoute>> shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Arrays.asList(
				new JMessages.JResult.JResultRoute("South", "South",
						"Because of a situation beyond our control, some bus stops have been relocated: " +
								"52614 (Rachel / Parthenais), " +
								"52685 (Parthenais / Gauthier), " +
								"52736 (Parthenais / Sherbrooke), " +
								"52830 (Parthenais / de Rouen), " +
								"52879 (Parthenais / Larivière). " +
								"In effect from 8 June 2018 at 18 h 34 for an indefinite period",
						CODE_MESSAGE,
						"20180608"),
				new JMessages.JResult.JResultRoute("South", "South",
						"Some bus stops have been relocated because of road conditions: " +
								"53046 (De Maisonneuve / De Lorimier). " +
								"In effect from 16 September 2009 at 15 h 30 to 31 December 2018 at 18 h 30",
						CODE_MESSAGE,
						"20090916")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		shortNameResultRoutes = new ArrayList<>();
		shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Collections.singletonList(
				new JMessages.JResult.JResultRoute("North", "North",
						"Because of roadwork, some bus stops have been relocated: " +
								"52561 (De Lorimier / Rachel). " +
								"In effect from 27 September 2018 at 15 h 42 for an indefinite period",
						CODE_MESSAGE,
						"20180927")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		// Act
		Collection<ServiceUpdate> serviceUpdates = provider.parseAgencyJSONMessageResults(jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(serviceUpdates);
		assertEquals(3, serviceUpdates.size());
		Iterator<ServiceUpdate> it = serviceUpdates.iterator();
		ServiceUpdate serviceUpdate = it.next(); // 1
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 2
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 3
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
	}

	@Test
	public void testParseAgencyJSONMessageResultsFr() {
		// Arrange
		String routeShortName = "10";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(1, routeShortName, "route 1", "color"),
				DEFAULT_TRIP,
				DEFAULT_STOP,
				false);
		long newLastUpdateInMs = System.currentTimeMillis();
		List<JMessages.JResult> jResults = new ArrayList<>();
		ArrayList<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRoutes = new ArrayList<>();
		HashMap<String, List<JMessages.JResult.JResultRoute>> shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Arrays.asList(
				new JMessages.JResult.JResultRoute("South", "Sud",
						"En raison d'une situation indépendante de notre volonté, certains arrêts ont été déplacés: " +
								"52614 (Rachel / Parthenais), " +
								"52685 (Parthenais / Gauthier), " +
								"52736 (Parthenais / Sherbrooke), " +
								"52830 (Parthenais / de Rouen), " +
								"52879 (Parthenais / Larivière). " +
								"En vigueur du 8 juin 2018 à 18 h 34 pour une durée indéterminée",
						CODE_MESSAGE,
						"20180608"),
				new JMessages.JResult.JResultRoute("South", "Sud",
						"Certains arrêts sont déplacés en raison des conditions de la route: " +
								"53046 (De Maisonneuve / De Lorimier). " +
								"En vigueur du 16 septembre 2009 à 15 h 30 jusqu'au 31 décembre 2018 à 18 h 30",
						CODE_MESSAGE,
						"20090916")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		shortNameResultRoutes = new ArrayList<>();
		shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Collections.singletonList(
				new JMessages.JResult.JResultRoute("North", "Nord",
						"En raison de travaux de voirie, certains arrêts ont été déplacés: " +
								"52561 (De Lorimier / Rachel). " +
								"En vigueur du 27 septembre 2018 à 15 h 42 pour une durée indéterminée",
						CODE_MESSAGE,
						"20180927")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		// Act
		Collection<ServiceUpdate> serviceUpdates = provider.parseAgencyJSONMessageResults(jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(serviceUpdates);
		assertEquals(3, serviceUpdates.size());
		Iterator<ServiceUpdate> it = serviceUpdates.iterator();
		ServiceUpdate serviceUpdate = it.next(); // 1
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 2
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 3
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
	}

	@Test
	public void testParseAgencyJSONMessageResultsNonStandardDirectionName() {
		// Arrange
		String routeShortName = "37";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(1, routeShortName, "route 1", "color"),
				DEFAULT_TRIP,
				DEFAULT_STOP,
				false);
		long newLastUpdateInMs = System.currentTimeMillis();
		List<JMessages.JResult> jResults = new ArrayList<>();
		ArrayList<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRoutes = new ArrayList<>();
		HashMap<String, List<JMessages.JResult.JResultRoute>> shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Arrays.asList(
				new JMessages.JResult.JResultRoute("South", "To Angrignon metro station",
						"Due to major roadworks around Turcot, Bonaventure and Champlain, " +
								"bus service is running late on this line.",
						CODE_MESSAGE,
						"20160720"),
				new JMessages.JResult.JResultRoute("South", "To Angrignon metro station",
						"Because of roadwork, some bus stops have been relocated: " +
								"51594 (De Maisonneuve / Bulmer), " +
								"56272 (Sainte-Catherine / Victoria), " +
								"53906 (Sainte-Catherine / Grosvenor). " +
								"In effect from 1 October 2018 at 13 h 33 for an indefinite period",
						CODE_MESSAGE,
						"20181001"),
				new JMessages.JResult.JResultRoute("South", "To Angrignon metro station",
						"Because of roadwork, some bus stops have been relocated: " +
								"61641 (École James-Lyng (Notre-Dame / de Carillon)). " +
								"In effect from 17 January 2018 at 22 h 33 for an indefinite period",
						CODE_MESSAGE,
						"20180117"),
				new JMessages.JResult.JResultRoute("South", "To Angrignon metro station",
						"Because of roadwork, some bus stops have been relocated: " +
								"56272 (Sainte-Catherine / Victoria), " +
								"54044 (Saint-Rémi / Pullman). " +
								"In effect from 28 November 2017 at 21 h 00 to 31 December 2018 at 5 h 00",
						CODE_MESSAGE,
						"20171128")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		shortNameResultRoutes = new ArrayList<>();
		shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Arrays.asList(
				new JMessages.JResult.JResultRoute("North", "To Vendôme metro station",
						"Due to major roadworks around Turcot, Bonaventure and Champlain, bus service is running late on this line.",
						CODE_MESSAGE,
						"20160720"),
				new JMessages.JResult.JResultRoute("North", "To Vendôme metro station",
						"Because of roadwork, some bus stops have been relocated: " +
								"60533 (Centre Gadbois (Côte-Saint-Paul / No 5485)), " +
								"60708 (Côte-Saint-Paul / Saint-Ambroise). " +
								"In effect from 1 October 2018 at 13 h 33 for an indefinite period",
						CODE_MESSAGE,
						"20181001"),
				new JMessages.JResult.JResultRoute("North", "To Vendôme metro station",
						"Because of a situation beyond our control, some bus stops have been relocated: " +
								"56591 (de l'Église / de Verdun). " +
								"In effect from 27 September 2018 at 18 h 20 for an indefinite period",
						CODE_MESSAGE,
						"20180927"),
				new JMessages.JResult.JResultRoute("North", "To Vendôme metro station",
						"Because of roadwork, some bus stops have been relocated: " +
								"56727 (des Trinitaires / Newman). " +
								"In effect from 3 August 2018 at 14 h 35 for an indefinite period",
						CODE_MESSAGE,
						"20180803"),
				new JMessages.JResult.JResultRoute("North", "To Vendôme metro station",
						"Because of a situation beyond our control, some bus stops have been relocated: " +
								"56614 (Woodland / Wellington), " +
								"56633 (LaSalle / Woodland). " +
								"In effect from 30 June 2018 at 0 h 00 for an indefinite period",
						CODE_MESSAGE,
						"20180630"),
				new JMessages.JResult.JResultRoute("North", "To Vendôme metro station",
						"Because of roadwork, some bus stops have been cancelled: " +
								"52050 (Jolicoeur / Angers). " +
								"In effect from 13 March 2018 at 13 h 07 for an indefinite period",
						CODE_MESSAGE,
						"20180313")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		// Act
		Collection<ServiceUpdate> serviceUpdates = provider.parseAgencyJSONMessageResults(jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(serviceUpdates);
		assertEquals(10, serviceUpdates.size());
		Iterator<ServiceUpdate> it = serviceUpdates.iterator();
		ServiceUpdate serviceUpdate = it.next(); // 1
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 2
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 3
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 4
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 5
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 6
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 7
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 8
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 9
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 10
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
	}

	@Test
	public void testParseAgencyJSONMessageResultsNonStandardDirectionName2() {
		// Arrange
		String routeShortName = "747";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(1, routeShortName, "route 1", "color"),
				DEFAULT_TRIP,
				DEFAULT_STOP,
				false);
		long newLastUpdateInMs = System.currentTimeMillis();
		List<JMessages.JResult> jResults = new ArrayList<>();
		ArrayList<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRoutes = new ArrayList<>();
		HashMap<String, List<JMessages.JResult.JResultRoute>> shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Arrays.asList(
				new JMessages.JResult.JResultRoute("East", "To Downtown Montreal",
						"Due to major roadworks around Turcot, Bonaventure and Champlain, bus service is running late on this line.",
						CODE_MESSAGE,
						"20160720"),
				new JMessages.JResult.JResultRoute("East", "To Downtown Montreal",
						"Le tarif de la ligne de bus 747 reliant le centre-ville à l'aéroport Montréal-Trudeau est de 10$, payable en pièces de monnaie dans les bus. " +
								"Pour plus d’informations sur le tarif et les titres valides, veuillez consultez la section « Titres de transport »",
						CODE_MESSAGE,
						"20100316"),
				new JMessages.JResult.JResultRoute("East", "To Downtown Montreal",
						"Because of roadwork, some bus stops have been relocated: " +
								"61611 (Station Berri-UQAM (1621 rue Berri)). " +
								"In effect from 30 October 2017 at 7 h 31 for an indefinite period",
						CODE_MESSAGE,
						"20171030")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		shortNameResultRoutes = new ArrayList<>();
		shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Arrays.asList(
				new JMessages.JResult.JResultRoute("West", "To Montréal-Trudeau airport",
						"Due to major roadworks around Turcot, Bonaventure and Champlain, bus service is running late on this line.",
						CODE_MESSAGE,
						"20160720"),
				new JMessages.JResult.JResultRoute("West", "To Montréal-Trudeau airport",
						"Le tarif de la ligne de bus 747 reliant le centre-ville à l'aéroport Montréal-Trudeau est de 10$, payable en pièces de monnaie dans les bus. " +
								"Pour plus d’informations sur le tarif et les titres valides, veuillez consultez la section « Titres de transport ».",
						CODE_MESSAGE,
						"20100316"),
				new JMessages.JResult.JResultRoute("West", "To Montréal-Trudeau airport",
						"Because of roadwork, some bus stops have been relocated: " +
								"52844 (Station Berri-UQAM (Berri / Ste-Catherine)). " +
								"In effect from 4 September 2018 at 18 h 10 for an indefinite period",
						CODE_MESSAGE,
						"20180904")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		// Act
		Collection<ServiceUpdate> serviceUpdates = provider.parseAgencyJSONMessageResults(jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(serviceUpdates);
		assertEquals(6, serviceUpdates.size());
		Iterator<ServiceUpdate> it = serviceUpdates.iterator();
		ServiceUpdate serviceUpdate = it.next(); // 1
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_EAST),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 2
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_EAST),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 3
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_EAST),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 4
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_WEST),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 5
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_WEST),
				serviceUpdate.getTargetUUID());
		serviceUpdate = it.next(); // 6
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_WEST),
				serviceUpdate.getTargetUUID());
	}

	@Test
	public void testParseAgencyJSONMessageResultsServiceNormal() {
		// Arrange
		String routeShortName = "14";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(1, routeShortName, "route 1", "color"),
				DEFAULT_TRIP,
				DEFAULT_STOP,
				false);
		long newLastUpdateInMs = System.currentTimeMillis();
		List<JMessages.JResult> jResults = new ArrayList<>();
		ArrayList<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRoutes = new ArrayList<>();
		HashMap<String, List<JMessages.JResult.JResultRoute>> shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Collections.singletonList(
				new JMessages.JResult.JResultRoute("South", "Sud",
						"En raison de travaux de voirie, certains arrêts ont été déplacés: " +
								"54013 (Saint-Denis / De La Gauchetière). " +
								"En vigueur du 4 septembre 2018 à 18 h 16 pour une durée indéterminée",
						CODE_MESSAGE,
						"20180904")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		shortNameResultRoutes = new ArrayList<>();
		shortNameResultRoute = new HashMap<>();
		shortNameResultRoute.put(routeShortName, Collections.singletonList(
				new JMessages.JResult.JResultRoute("North", "Nord",
						"Service normal",
						"Normal",
						"")
		));
		shortNameResultRoutes.add(shortNameResultRoute);
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		// Act
		Collection<ServiceUpdate> serviceUpdates = provider.parseAgencyJSONMessageResults(jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(serviceUpdates);
		assertEquals(2, serviceUpdates.size());
		Iterator<ServiceUpdate> it = serviceUpdates.iterator();
		ServiceUpdate serviceUpdate = it.next(); // 1
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_SOUTH),
				serviceUpdate.getTargetUUID());
		assertTrue(ServiceUpdate.isSeverityInfo(serviceUpdate.getSeverity()));
		serviceUpdate = it.next(); // 2
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, Trip.HEADING_NORTH),
				serviceUpdate.getTargetUUID());
		assertFalse(ServiceUpdate.isSeverityInfo(serviceUpdate.getSeverity()));
	}

	@Test
	public void testParseAgencyJSONMessageResultsNoMessages() {
		// Arrange
		String routeShortName = "1234";
		String headsignValue = "ABCD";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				new Route(1, routeShortName, "route 1", "color"),
				new Trip(1, Trip.HEADSIGN_TYPE_STRING, headsignValue, 1),
				DEFAULT_STOP,
				false);
		long newLastUpdateInMs = System.currentTimeMillis();
		List<JMessages.JResult> jResults = new ArrayList<>();
		ArrayList<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRoutes = new ArrayList<>();
		jResults.add(new JMessages.JResult(shortNameResultRoutes));
		// Act
		Collection<ServiceUpdate> serviceUpdates = provider.parseAgencyJSONMessageResults(jResults, rts, newLastUpdateInMs);
		// Assert
		assertNotNull(serviceUpdates);
		assertEquals(1, serviceUpdates.size());
		Iterator<ServiceUpdate> it = serviceUpdates.iterator();
		ServiceUpdate serviceUpdate = it.next(); // 1
		assertEquals(
				StmInfoApiProvider.getServiceUpdateTargetUUID(AUTHORITY, routeShortName, headsignValue),
				serviceUpdate.getTargetUUID());
	}

	@Test
	public void testFindRTSSeverityStopWarning() {
		// Arrange
		String stopCode = "52844";
		String text = "Because of roadwork, some bus stops have been relocated: " +
				stopCode + " (Station Berri-UQAM (Berri / Ste-Catherine)). " +
				"In effect from 4 September 2018 at 18 h 10 for an indefinite period";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				new Stop(1, stopCode, "stop 1", 0, 0),
				false);
		Pattern stopPattern = StmInfoApiProvider.STOP;
		// Act
		int severity = provider.findRTSSeverity(text, rts, stopPattern);
		// Assert
		assertEquals(ServiceUpdate.SEVERITY_WARNING_POI, severity);
	}

	@Test
	public void testFindRTSSeverityStopWarningFr() {
		// Arrange
		String stopCode = "52844";
		String text = "En raison de travaux de voirie, certains arrêts ont été déplacés: " +
				stopCode + " (Station Berri-UQAM (1621 rue Berri)). " +
				"En vigueur du 30 octobre 2017 à 7 h 31 pour une durée indéterminée";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				new Stop(1, stopCode, "stop 1", 0, 0),
				false);
		Pattern stopPattern = StmInfoApiProvider.STOP_FR;
		// Act
		int severity = provider.findRTSSeverity(text, rts, stopPattern);
		// Assert
		assertEquals(ServiceUpdate.SEVERITY_WARNING_POI, severity);
	}

	@Test
	public void testFindRTSSeverityAnotherStopInfo() {
		// Arrange
		String stopCode = "12345";
		String text = "Because of roadwork, some bus stops have been relocated: " +
				stopCode + " (Station Berri-UQAM (Berri / Ste-Catherine)). " +
				"In effect from 4 September 2018 at 18 h 10 for an indefinite period";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				new Stop(1, "67890", "stop 1", 0, 0),
				false);
		Pattern stopPattern = StmInfoApiProvider.STOP;
		// Act
		int severity = provider.findRTSSeverity(text, rts, stopPattern);
		// Assert
		assertEquals(ServiceUpdate.SEVERITY_INFO_RELATED_POI, severity);
	}

	@Test
	public void testFindRTSSeverityAnotherStopInfoFr() {
		// Arrange
		String stopCode = "12345";
		String text = "En raison de travaux de voirie, certains arrêts ont été déplacés: " +
				stopCode + " (Station Berri-UQAM (1621 rue Berri)). " +
				"En vigueur du 30 octobre 2017 à 7 h 31 pour une durée indéterminée";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				new Stop(1, "67890", "stop 1", 0, 0),
				false);
		Pattern stopPattern = StmInfoApiProvider.STOP_FR;
		// Act
		int severity = provider.findRTSSeverity(text, rts, stopPattern);
		// Assert
		assertEquals(ServiceUpdate.SEVERITY_INFO_RELATED_POI, severity);
	}

	@Test
	public void testFindRTSSeverityGenericInfo() {
		// Arrange
		String text = "Due to major roadworks around Turcot, Bonaventure and Champlain, " +
				"bus service is running late on this line.";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				new Stop(1, "12345", "stop 1", 0, 0),
				false);
		Pattern stopPattern = StmInfoApiProvider.STOP;
		// Act
		int severity = provider.findRTSSeverity(text, rts, stopPattern);
		// Assert
		assertTrue(ServiceUpdate.isSeverityInfo(severity));
	}

	@Test
	public void testFindRTSSeverityGenericInfoFr() {
		// Arrange
		String text = "En raison des travaux entourant le grand chantier Turcot, Bonaventure et Champlain " +
				"des retards sont à prévoir sur cette ligne.";
		rts = new RouteTripStop(
				AUTHORITY,
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				DEFAULT_ROUTE,
				DEFAULT_TRIP,
				new Stop(1, "12345", "stop 1", 0, 0),
				false);
		Pattern stopPattern = StmInfoApiProvider.STOP_FR;
		// Act
		int severity = provider.findRTSSeverity(text, rts, stopPattern);
		// Assert
		assertTrue(ServiceUpdate.isSeverityInfo(severity));
	}
}