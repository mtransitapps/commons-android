package org.mtransit.android.commons.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mtransit.android.commons.data.DirectionExtKt.makeDirection;
import static org.mtransit.android.commons.data.RouteDirectionStopExtKt.makeRouteDirectionStop;
import static org.mtransit.android.commons.data.RouteExtKt.makeRoute;
import static org.mtransit.android.commons.data.StopExtKt.makeStop;

import android.content.Context;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.commons.CommonsApp;

public class OneBusAwayProviderTests {

	private static final String AUTHORITY = "authority.test";

	private static final Route DEFAULT_ROUTE = makeRoute(
			AUTHORITY,
			1,
			"1",
			"route 1",
			"blue"
	);

	private static final Stop DEFAULT_STOP = makeStop(1, "1", "stop 1", 0, 0, 0, 1, null);

	private final Context context = mock();

	@Before
	public void setUp() {
		CommonsApp.setup(false);
	}

	@Test
	public void testIsSameRoute() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "50";
		long routeId = 50L;
		long directionId = -1L;
		String jRouteId = "YRT_50";
		String jRouteShortName = "50";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "50";
		long routeId = 50L;
		long directionId = -1L;
		String jRouteId = "YRT_96";
		String jRouteShortName = "96";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteDoubleRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "98|99";
		long routeId = 9899L;
		long directionId = -1L;
		String jRouteId = "YRT_9899";
		String jRouteShortName = "98|99";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteDoubleRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "98|99";
		long routeId = 9899L;
		long directionId = -1L;
		String jRouteId = "YRT_99";
		String jRouteShortName = "99";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteEndsWithAZRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "4A";
		long routeId = 10004L;
		long directionId = -1L;
		String jRouteId = "YRT_4001";
		String jRouteShortName = "4A";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteEndsWithAZRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "4A";
		long routeId = 10004L;
		long directionId = -1L;
		String jRouteId = "YRT_4";
		String jRouteShortName = "4";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteAZRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "Blue-A";
		long routeId = 602L;
		long directionId = -1L;
		String jRouteId = "YRT_602";
		String jRouteShortName = "blue A";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteAZRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "Blue-A";
		long routeId = 602L;
		long directionId = -1L;
		String jRouteId = "YRT_601";
		String jRouteShortName = "blue";
		RouteDirectionStop rds = getRouteDirectionStop(routeShortName, routeId, directionId);

		boolean result = provider.isSameRoute(rds, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testCleanDirectionHeadsignRDS() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String tripHeadsign = "Martin Grv Via Vaughan Metropolitan Ctr";
		Direction direction = makeDirection(
				AUTHORITY,
				-1,
				Direction.HEADSIGN_TYPE_STRING,
				"Martin Grv",
				DEFAULT_ROUTE.getId()
		);
		RouteDirectionStop rds = getRouteDirectionStop(DEFAULT_ROUTE, direction, DEFAULT_STOP, false);

		String result = provider.cleanDirectionHeadsign(context, tripHeadsign, rds);

		assertEquals("Via Vaughan Metropolitan Ctr", result);
	}

	@NonNull
	private RouteDirectionStop getRouteDirectionStop(String routeShortName, long routeId, long directionId) {
		Route route = makeRoute(
				AUTHORITY,
				routeId,
				routeShortName,
				"route " + routeShortName,
				"color"
		);
		Direction direction = makeDirection(
				AUTHORITY,
				directionId,
				Direction.HEADSIGN_TYPE_STRING,
				"direction " + directionId,
				route.getId()
		);
		boolean noPickup = false;
		//noinspection ConstantConditions
		return getRouteDirectionStop(route, direction, DEFAULT_STOP, noPickup);
	}

	@NonNull
	private RouteDirectionStop getRouteDirectionStop(
			Route route,
			Direction direction,
			@SuppressWarnings("SameParameterValue") Stop stop,
			boolean noPickup
	) {
		return makeRouteDirectionStop(
				DataSourceTypeId.BUS,
				route,
				direction,
				stop,
				noPickup
		);
	}
}