package org.mtransit.android.commons.provider;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class BCTransitNextRideProviderTests {

	@Test
	public void testRemoveStaticHeadsign() {
		BCTransitNextRideProvider provider = new BCTransitNextRideProvider();
		String tripHeadsign;
		String staticHeadsign;
		String expected;
		//
		tripHeadsign = "Woodgrove";
		staticHeadsign = "Woodgrove";
		expected = "";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "Country Club";
		staticHeadsign = "Country Club";
		expected = "";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "Parksville/Wembley Mall";
		staticHeadsign = "Parksville";
		expected = "Parksville/Wembley Mall";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "Parksville/Wembley Mall";
		staticHeadsign = "Wembley Mall";
		expected = "Parksville/Wembley Mall";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "Westwood/Ashlee At Holland";
		staticHeadsign = "Westwood";
		expected = "Westwood/Ashlee At Holland";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "A VIU Via Jingle Pot";
		staticHeadsign = "VIU";
		expected = "A Via Jingle Pot";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "A VIU To Jingle Pot";
		staticHeadsign = "VIU";
		expected = "A To Jingle Pot";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "A Dover To Woodgrove";
		staticHeadsign = "Woodgrove";
		expected = "A Dover";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "A Dover To Country Club/Downtown";
		staticHeadsign = "Woodgrove";
		expected = "A Dover To Country Club/Downtown";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "Cinnabar";
		staticHeadsign = "Cinnabar & Cedar";
		expected = "Cinnabar";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "A Woodgrove Via Jingle Pot";
		staticHeadsign = "Woodgrove";
		expected = "A Via Jingle Pot";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "NRGH/Northfield - To Woodgrove";
		staticHeadsign = "Woodgrove";
		expected = "NRGH/Northfield";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
		//
		tripHeadsign = "Woodgrove & BC Ferries";
		staticHeadsign = "Woodgrove";
		expected = "Woodgrove & BC Ferries";
		assertEquals(expected, provider.removeStaticHeadsign(tripHeadsign, staticHeadsign));
	}

	@Test
	public void testCleanPatternName() {
		BCTransitNextRideProvider provider = new BCTransitNextRideProvider();
		String jPatternName;
		String jRouteCode;
		String jRouteName;
		String expected;
		//
		jRouteCode = "1";
		jRouteName = "Downtown / Country Club";
		jPatternName = "1 - Downtown";
		expected = "Downtown";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "5";
		jRouteName = "Fairview";
		jPatternName = "5 - Fairview - To Westwood/Ashlee at Holland";
		expected = "Westwood/Ashlee at Holland";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "5";
		jRouteName = "Fairview";
		jPatternName = "5 - Fairview - To Westwood/Ashlee at Holland";
		expected = "Westwood/Ashlee at Holland";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "6";
		jRouteName = "Harewood";
		jPatternName = "6 - Harewood - To VIU";
		expected = "VIU";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "6";
		jRouteName = "Harewood";
		jPatternName = "6 - Harewood - To Downtown";
		expected = "Downtown";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "7";
		jRouteName = "Cinnabar / Cedar";
		jPatternName = "7 - Cinnabar & Cedar";
		expected = "";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "7";
		jRouteName = "Cinnabar / Cedar";
		jPatternName = "7 - Cinnabar";
		expected = "Cinnabar";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "11";
		jRouteName = "Lantzville";
		jPatternName = "11 - Lantzville";
		expected = "";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "11";
		jRouteName = "Lantzville";
		jPatternName = "11 - Lantzville - Via McGirr Rd";
		expected = "Via McGirr Rd";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "15";
		jRouteName = "VIU Connector";
		jPatternName = "15A VIU - Via Jingle Pot";
		expected = "A VIU Via Jingle Pot";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "15";
		jRouteName = "VIU Connector";
		jPatternName = "15 VIU - Via Parkway";
		expected = "VIU Via Parkway";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "15";
		jRouteName = "VIU Connector";
		jPatternName = "15 Woodgrove -Via Parkway";
		expected = "Woodgrove Via Parkway";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "15";
		jRouteName = "VIU Connector";
		jPatternName = "15A Woodgrove - Via Jingle Pot";
		expected = "A Woodgrove Via Jingle Pot";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "20";
		jRouteName = "Hammond Bay";
		jPatternName = "20A Dover - To Country Club/Downtown";
		expected = "A Dover To Country Club/Downtown";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "20";
		jRouteName = "Hammond Bay";
		jPatternName = "20 Hammond Bay - To Woodgrove";
		expected = "Woodgrove";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "25";
		jRouteName = "Ferry Shuttle";
		jPatternName = "25 Ferry Shutlle"; // Yes, misspelled
		expected = "Ferry Shutlle"; // Yes, misspelled
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "25";
		jRouteName = "Ferry Shuttle";
		jPatternName = "25 Ferry Shuttle";
		expected = "";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "30";
		jRouteName = "NRGH";
		jPatternName = "30 NRGH/Northfield - To Downtown";
		expected = "NRGH/Northfield - To Downtown";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "30";
		jRouteName = "NRGH";
		jPatternName = "30 NRGH - To Downtown";
		expected = "Downtown";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "40";
		jRouteName = "VIU EXPRESS";
		jPatternName = "40 VIU Express - To Woodgrove";
		expected = "Woodgrove";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "50";
		jRouteName = "Downtown / Woodgrove";
		jPatternName = "50 Downtown";
		expected = "Downtown";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "88";
		jRouteName = "Parksville";
		jPatternName = "88 Parksville/Wembley Mall";
		expected = "Parksville/Wembley Mall";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "91";
		jRouteName = "Intercity";
		jPatternName = "91 Intercity - To Woodgrove & BC Ferries";
		expected = "Woodgrove & BC Ferries";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "92";
		jRouteName = "Hammond Bay";
		jPatternName = "92 Hammond Bay - To Country club";
		expected = "Country club";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "97";
		jRouteName = "Eaglecrest";
		jPatternName = "97 Eaglecrest";
		expected = "";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "98";
		jRouteName = "Qualicum Beach";
		jPatternName = "98 Qualicum Beach";
		expected = "";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
		//
		jRouteCode = "99";
		jRouteName = "Deep Bay";
		jPatternName = "99 Deep Bay - To Qualicum Beach";
		expected = "Qualicum Beach";
		assertEquals(expected, provider.cleanPatternName(jPatternName, jRouteCode, jRouteName));
	}
}