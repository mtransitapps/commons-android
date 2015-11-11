package org.mtransit.android.commons;

import java.util.regex.Pattern;

public final class CleanUtils {

	private CleanUtils() {
	}

	private static final Pattern CLEAN_SPACES = Pattern.compile("\\s+");
	private static final Pattern CLEAN_P1 = Pattern.compile("\\([\\s]+");
	private static final String CLEAN_P1_REPLACEMENT = "(";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]+\\)");
	private static final String CLEAN_P2_REPLACEMENT = ")";

	public static String cleanLabel(String label) {
		label = CLEAN_SPACES.matcher(label).replaceAll(SPACE);
		label = CLEAN_P1.matcher(label).replaceAll(CLEAN_P1_REPLACEMENT);
		label = CLEAN_P2.matcher(label).replaceAll(CLEAN_P2_REPLACEMENT);
		label = WordUtils.capitalize(label, SPACE_CHAR, '-', '–', '/', '(', '.');
		return label.trim();
	}

	public static final Pattern CLEAN_AT = Pattern.compile("((^|\\W){1}(at)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	public static final String CLEAN_AT_REPLACEMENT = "$2@$4";

	public static final Pattern CLEAN_AND = Pattern.compile("((^|\\W){1}(and)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	public static final String CLEAN_AND_REPLACEMENT = "$2&$4";

	public static final Pattern CLEAN_ET = Pattern.compile("((^|\\W){1}(et)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	public static final String CLEAN_ET_REPLACEMENT = "$2&$4";

	private static final String SPACE = " ";
	private static final char SPACE_CHAR = ' ';

	private static final Pattern CLEAN_SLASH = Pattern.compile("(\\S)[\\s]*[/][\\s]*(\\S)");
	private static final String CLEAN_SLASH_REPLACEMENT = "$1 / $2";

	public static final String cleanSlashes(String string) {
		return CLEAN_SLASH.matcher(string).replaceAll(CLEAN_SLASH_REPLACEMENT);
	}

	private static final Pattern POINT1 = Pattern.compile("((^|\\W){1}([\\w]{1})\\.(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String POINT1_REPLACEMENT = "$2$3$4";

	private static final Pattern POINTS = Pattern.compile("((^|\\W){1}([\\w]+)\\.(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String POINTS_REPLACEMENT = "$2$3$4";

	public static String removePoints(String string) {
		string = POINT1.matcher(string).replaceAll(POINT1_REPLACEMENT);
		string = POINTS.matcher(string).replaceAll(POINTS_REPLACEMENT);
		return string;
	}

	private static final Pattern FIRST = Pattern.compile("(^|\\s){1}(first)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String FIRST_REPLACEMENT = "$11st$3";
	private static final Pattern SECOND = Pattern.compile("(^|\\s){1}(second)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SECOND_REPLACEMENT = "$12nd$3";
	private static final Pattern THIRD = Pattern.compile("(^|\\s){1}(third)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String THIRD_REPLACEMENT = "$13rd$3";
	private static final Pattern FOURTH = Pattern.compile("(^|\\s){1}(fourth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String FOURTH_REPLACEMENT = "$14th$3";
	private static final Pattern FIFTH = Pattern.compile("(^|\\s){1}(fifth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String FIFTH_REPLACEMENT = "$15th$3";
	private static final Pattern SIXTH = Pattern.compile("(^|\\s){1}(sixth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SIXTH_REPLACEMENT = "$16th$3";
	private static final Pattern SEVENTH = Pattern.compile("(^|\\s){1}(seventh)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SEVENTH_REPLACEMENT = "$17th$3";
	private static final Pattern EIGHTH = Pattern.compile("(^|\\s){1}(eighth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String EIGHTH_REPLACEMENT = "$18th$3";
	private static final Pattern NINTH = Pattern.compile("(^|\\s){1}(ninth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String NINTH_REPLACEMENT = "$19th$3";

	public static String cleanNumbers(String string) {
		string = FIRST.matcher(string).replaceAll(FIRST_REPLACEMENT);
		string = SECOND.matcher(string).replaceAll(SECOND_REPLACEMENT);
		string = THIRD.matcher(string).replaceAll(THIRD_REPLACEMENT);
		string = FOURTH.matcher(string).replaceAll(FOURTH_REPLACEMENT);
		string = FIFTH.matcher(string).replaceAll(FIFTH_REPLACEMENT);
		string = SIXTH.matcher(string).replaceAll(SIXTH_REPLACEMENT);
		string = SEVENTH.matcher(string).replaceAll(SEVENTH_REPLACEMENT);
		string = EIGHTH.matcher(string).replaceAll(EIGHTH_REPLACEMENT);
		string = NINTH.matcher(string).replaceAll(NINTH_REPLACEMENT);
		return string;
	}

	private static final String REGEX_START_END = "((^|\\W){1}(%s)(\\W|$){1})";
	private static final String REGEX_START_END_REPLACEMENT = "$2%s$4";

	private static final String REGEX_START_END_S = "((^|\\W){1}((%s)([s]?))(\\W|$){1})";
	private static final String REGEX_START_END_S_REPLACEMENT = "$2%s$5$6";

	private static final Pattern STREET = Pattern.compile(String.format(REGEX_START_END, "street"), Pattern.CASE_INSENSITIVE);
	private static final String STREET_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "St");
	private static final Pattern AVENUE = Pattern.compile(String.format(REGEX_START_END, "avenue"), Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ave");
	private static final Pattern ROAD = Pattern.compile(String.format(REGEX_START_END, "road"), Pattern.CASE_INSENSITIVE);
	private static final String ROAD_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Rd");
	private static final Pattern HIGHWAY = Pattern.compile(String.format(REGEX_START_END, "highway"), Pattern.CASE_INSENSITIVE);
	private static final String HIGHWAY_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hwy");
	private static final Pattern BOULEVARD = Pattern.compile(String.format(REGEX_START_END, "boulevard"), Pattern.CASE_INSENSITIVE);
	private static final String BOULEVARD_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Blvd");
	private static final Pattern DRIVE = Pattern.compile(String.format(REGEX_START_END, "drive"), Pattern.CASE_INSENSITIVE);
	private static final String DRIVE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Dr");
	private static final Pattern PLACE = Pattern.compile(String.format(REGEX_START_END, "place"), Pattern.CASE_INSENSITIVE);
	private static final String PLACE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pl");
	private static final Pattern PLAZA = Pattern.compile(String.format(REGEX_START_END, "plaza"), Pattern.CASE_INSENSITIVE);
	private static final String PLAZA_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Plz");
	private static final Pattern LANE = Pattern.compile(String.format(REGEX_START_END, "lane"), Pattern.CASE_INSENSITIVE);
	private static final String LANE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ln");
	private static final Pattern CRESCENT = Pattern.compile(String.format(REGEX_START_END, "crescent"), Pattern.CASE_INSENSITIVE);
	private static final String CRESCENT_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Cr");
	private static final Pattern HEIGHTS = Pattern.compile(String.format(REGEX_START_END, "heights"), Pattern.CASE_INSENSITIVE);
	private static final String HEIGHTS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hts");
	private static final Pattern GROVE = Pattern.compile(String.format(REGEX_START_END, "grove"), Pattern.CASE_INSENSITIVE);
	private static final String GROVE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Grv");
	private static final Pattern POINT = Pattern.compile(String.format(REGEX_START_END, "point"), Pattern.CASE_INSENSITIVE);
	private static final String POINT_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pt");
	private static final Pattern POINTE = Pattern.compile(String.format(REGEX_START_END, "pointe"), Pattern.CASE_INSENSITIVE);
	private static final String POINTE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pte");
	private static final Pattern TERRACE = Pattern.compile(String.format(REGEX_START_END, "terrace"), Pattern.CASE_INSENSITIVE);
	private static final String TERRACE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ter");
	private static final Pattern MANOR = Pattern.compile(String.format(REGEX_START_END, "manor"), Pattern.CASE_INSENSITIVE);
	private static final String MANOR_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mnr");
	private static final Pattern GREEN = Pattern.compile(String.format(REGEX_START_END, "green"), Pattern.CASE_INSENSITIVE);
	private static final String GREEN_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Grn");
	private static final Pattern VALLEY = Pattern.compile(String.format(REGEX_START_END, "valley|vallley"), Pattern.CASE_INSENSITIVE);
	private static final String VALLEY_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Vly");
	private static final Pattern HILL = Pattern.compile(String.format(REGEX_START_END_S, "hill|h ill"), Pattern.CASE_INSENSITIVE);
	private static final String HILL_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Hl");
	private static final Pattern LAKE = Pattern.compile(String.format(REGEX_START_END, "lake"), Pattern.CASE_INSENSITIVE);
	private static final String LAKE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Lk");
	private static final Pattern MEADOW = Pattern.compile(String.format(REGEX_START_END_S, "meadow"), Pattern.CASE_INSENSITIVE);
	private static final String MEADOW_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Mdw");
	private static final Pattern CIRCLE = Pattern.compile(String.format(REGEX_START_END, "circle"), Pattern.CASE_INSENSITIVE);
	private static final String CIRCLE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Cir");
	private static final Pattern GLEN = Pattern.compile(String.format(REGEX_START_END, "glen"), Pattern.CASE_INSENSITIVE);
	private static final String GLEN_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Gln");
	private static final Pattern RIDGE = Pattern.compile(String.format(REGEX_START_END_S, "ridge"), Pattern.CASE_INSENSITIVE);
	private static final String RIDGE_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Rdg");
	private static final Pattern GARDEN = Pattern.compile(String.format(REGEX_START_END_S, "garden"), Pattern.CASE_INSENSITIVE);
	private static final String GARDEN_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Gdn");
	private static final Pattern CENTER = Pattern.compile(String.format(REGEX_START_END, "center|centre"), Pattern.CASE_INSENSITIVE);
	private static final String CENTER_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ctr");
	private static final Pattern ESTATE = Pattern.compile(String.format(REGEX_START_END_S, "estate"), Pattern.CASE_INSENSITIVE);
	private static final String ESTATE_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Est");
	private static final Pattern LANDING = Pattern.compile(String.format(REGEX_START_END, "landing"), Pattern.CASE_INSENSITIVE);
	private static final String LANDING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Lndg");
	private static final Pattern TRAIL = Pattern.compile(String.format(REGEX_START_END, "trail"), Pattern.CASE_INSENSITIVE);
	private static final String TRAIL_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Trl");
	private static final Pattern SPRING = Pattern.compile(String.format(REGEX_START_END_S, "spring"), Pattern.CASE_INSENSITIVE);
	private static final String SPRING_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Spg");
	private static final Pattern VIEW = Pattern.compile(String.format(REGEX_START_END, "view"), Pattern.CASE_INSENSITIVE);
	private static final String VIEW_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Vw");
	private static final Pattern VILLAGE = Pattern.compile(String.format(REGEX_START_END, "village"), Pattern.CASE_INSENSITIVE);
	private static final String VILLAGE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Vlg");
	private static final Pattern STATION = Pattern.compile(String.format(REGEX_START_END, "station"), Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Sta");
	private static final Pattern RANCH = Pattern.compile(String.format(REGEX_START_END, "ranch"), Pattern.CASE_INSENSITIVE);
	private static final String RANCH_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Rnch");
	private static final Pattern COVE = Pattern.compile(String.format(REGEX_START_END, "cove"), Pattern.CASE_INSENSITIVE);
	private static final String COVE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Cv");
	private static final Pattern SQUARE = Pattern.compile(String.format(REGEX_START_END, "square"), Pattern.CASE_INSENSITIVE);
	private static final String SQUARE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Sq");
	private static final Pattern BROOK = Pattern.compile(String.format(REGEX_START_END, "brook"), Pattern.CASE_INSENSITIVE);
	private static final String BROOK_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Brk");
	private static final Pattern CREEK = Pattern.compile(String.format(REGEX_START_END, "creek"), Pattern.CASE_INSENSITIVE);
	private static final String CREEK_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Crk");
	private static final Pattern CROSSING = Pattern.compile(String.format(REGEX_START_END, "crossing"), Pattern.CASE_INSENSITIVE);
	private static final String CROSSING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Xing");
	private static final Pattern CLIFF = Pattern.compile(String.format(REGEX_START_END_S, "cliff"), Pattern.CASE_INSENSITIVE);
	private static final String CLIFF_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Clf");
	private static final Pattern SHORE = Pattern.compile(String.format(REGEX_START_END_S, "shore"), Pattern.CASE_INSENSITIVE);
	private static final String SHORE_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Shr");
	private static final Pattern MOUNT = Pattern.compile(String.format(REGEX_START_END, "mount"), Pattern.CASE_INSENSITIVE);
	private static final String MOUNT_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mt");
	private static final Pattern MOUNTAIN = Pattern.compile(String.format(REGEX_START_END, "mountain"), Pattern.CASE_INSENSITIVE);
	private static final String MOUNTAIN_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mtn");
	private static final Pattern MARKET = Pattern.compile(String.format(REGEX_START_END, "market"), Pattern.CASE_INSENSITIVE);
	private static final String MARKET_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mkt");
	private static final Pattern BUILDING = Pattern.compile(String.format(REGEX_START_END, "building"), Pattern.CASE_INSENSITIVE);
	private static final String BUILDING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Bldg");
	private static final Pattern GREENS = Pattern.compile(String.format(REGEX_START_END, "Greens"), Pattern.CASE_INSENSITIVE);
	private static final String GREENS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Grns");
	private static final Pattern PARKWAY = Pattern.compile(String.format(REGEX_START_END, "parkway"), Pattern.CASE_INSENSITIVE);
	private static final String PARKWAY_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pkwy");
	private static final Pattern ISLAND = Pattern.compile(String.format(REGEX_START_END_S, "island"), Pattern.CASE_INSENSITIVE);
	private static final String ISLAND_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Isl");
	//
	private static final Pattern PARK = Pattern.compile(String.format(REGEX_START_END, "park"), Pattern.CASE_INSENSITIVE);
	private static final String PARK_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pk"); // not official
	private static final Pattern GATE = Pattern.compile(String.format(REGEX_START_END_S, "gate"), Pattern.CASE_INSENSITIVE);
	private static final String GATE_REPLACEMENT = String.format(REGEX_START_END_S_REPLACEMENT, "Gt"); // not official
	private static final Pattern PARKING = Pattern.compile(String.format(REGEX_START_END, "parking"), Pattern.CASE_INSENSITIVE);
	private static final String PARKING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pkng"); // not official
	private static final Pattern HOSPITAL = Pattern.compile(String.format(REGEX_START_END, "hospital"), Pattern.CASE_INSENSITIVE);
	private static final String HOSPITAL_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hosp"); // not official

	public static String cleanStreetTypes(String string) {
		string = LANE.matcher(string).replaceAll(LANE_REPLACEMENT);
		string = PLACE.matcher(string).replaceAll(PLACE_REPLACEMENT);
		string = PLAZA.matcher(string).replaceAll(PLAZA_REPLACEMENT);
		string = DRIVE.matcher(string).replaceAll(DRIVE_REPLACEMENT);
		string = BOULEVARD.matcher(string).replaceAll(BOULEVARD_REPLACEMENT);
		string = HIGHWAY.matcher(string).replaceAll(HIGHWAY_REPLACEMENT);
		string = STREET.matcher(string).replaceAll(STREET_REPLACEMENT);
		string = AVENUE.matcher(string).replaceAll(AVENUE_REPLACEMENT);
		string = ROAD.matcher(string).replaceAll(ROAD_REPLACEMENT);
		string = CRESCENT.matcher(string).replaceAll(CRESCENT_REPLACEMENT);
		string = HEIGHTS.matcher(string).replaceAll(HEIGHTS_REPLACEMENT);
		string = GROVE.matcher(string).replaceAll(GROVE_REPLACEMENT);
		string = POINT.matcher(string).replaceAll(POINT_REPLACEMENT);
		string = POINTE.matcher(string).replaceAll(POINTE_REPLACEMENT);
		string = TERRACE.matcher(string).replaceAll(TERRACE_REPLACEMENT);
		string = MANOR.matcher(string).replaceAll(MANOR_REPLACEMENT);
		string = GREEN.matcher(string).replaceAll(GREEN_REPLACEMENT);
		string = VALLEY.matcher(string).replaceAll(VALLEY_REPLACEMENT);
		string = LAKE.matcher(string).replaceAll(LAKE_REPLACEMENT);
		string = MEADOW.matcher(string).replaceAll(MEADOW_REPLACEMENT);
		string = CIRCLE.matcher(string).replaceAll(CIRCLE_REPLACEMENT);
		string = GLEN.matcher(string).replaceAll(GLEN_REPLACEMENT);
		string = RIDGE.matcher(string).replaceAll(RIDGE_REPLACEMENT);
		string = GARDEN.matcher(string).replaceAll(GARDEN_REPLACEMENT);
		string = CENTER.matcher(string).replaceAll(CENTER_REPLACEMENT);
		string = HILL.matcher(string).replaceAll(HILL_REPLACEMENT);
		string = ESTATE.matcher(string).replaceAll(ESTATE_REPLACEMENT);
		string = LANDING.matcher(string).replaceAll(LANDING_REPLACEMENT);
		string = TRAIL.matcher(string).replaceAll(TRAIL_REPLACEMENT);
		string = SPRING.matcher(string).replaceAll(SPRING_REPLACEMENT);
		string = VIEW.matcher(string).replaceAll(VIEW_REPLACEMENT);
		string = VILLAGE.matcher(string).replaceAll(VILLAGE_REPLACEMENT);
		string = STATION.matcher(string).replaceAll(STATION_REPLACEMENT);
		string = RANCH.matcher(string).replaceAll(RANCH_REPLACEMENT);
		string = COVE.matcher(string).replaceAll(COVE_REPLACEMENT);
		string = SQUARE.matcher(string).replaceAll(SQUARE_REPLACEMENT);
		string = BROOK.matcher(string).replaceAll(BROOK_REPLACEMENT);
		string = CREEK.matcher(string).replaceAll(CREEK_REPLACEMENT);
		string = CROSSING.matcher(string).replaceAll(CROSSING_REPLACEMENT);
		string = CLIFF.matcher(string).replaceAll(CLIFF_REPLACEMENT);
		string = SHORE.matcher(string).replaceAll(SHORE_REPLACEMENT);
		string = PARKING.matcher(string).replaceAll(PARKING_REPLACEMENT);
		string = MOUNT.matcher(string).replaceAll(MOUNT_REPLACEMENT);
		string = MOUNTAIN.matcher(string).replaceAll(MOUNTAIN_REPLACEMENT);
		string = PARK.matcher(string).replaceAll(PARK_REPLACEMENT);
		string = GATE.matcher(string).replaceAll(GATE_REPLACEMENT);
		string = HOSPITAL.matcher(string).replaceAll(HOSPITAL_REPLACEMENT);
		string = MARKET.matcher(string).replaceAll(MARKET_REPLACEMENT);
		string = BUILDING.matcher(string).replaceAll(BUILDING_REPLACEMENT);
		string = GREENS.matcher(string).replaceAll(GREENS_REPLACEMENT);
		string = PARKWAY.matcher(string).replaceAll(PARKWAY_REPLACEMENT);
		string = ISLAND.matcher(string).replaceAll(ISLAND_REPLACEMENT);
		return string;
	}

	private static final Pattern FR_CA_AVENUE = Pattern.compile(String.format(REGEX_START_END, "avenue"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_AVENUE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Av.");
	private static final Pattern FR_CA_AUTOROUTE = Pattern.compile(String.format(REGEX_START_END, "autoroute"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_AUTOROUTE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Aut.");
	private static final Pattern FR_CA_BOULEVARD = Pattern.compile(String.format(REGEX_START_END, "boulevard"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_BOULEVARD_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Boul.");
	private static final Pattern FR_CA_CARREFOUR = Pattern.compile(String.format(REGEX_START_END, "carrefour"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_CARREFOUR_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Carref.");
	private static final Pattern FR_CA_MONTAGNE = Pattern.compile(String.format(REGEX_START_END, "montagne"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_MONTAGNE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mgne");
	private static final Pattern FR_CA_MONTEE = Pattern.compile(String.format(REGEX_START_END, "mont[é|e]e"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_MONTEE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mtée");
	private static final Pattern FR_CA_PARC_INDUSTRIEL = Pattern.compile(String.format(REGEX_START_END, "parc industriel"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_PARC_INDUSTRIEL_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Parc Ind.");
	private static final Pattern FR_CA_RIVIERE = Pattern.compile(String.format(REGEX_START_END, "rivi[e|è]re"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_RIVIERE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Riv.");
	private static final Pattern FR_CA_SECTEUR = Pattern.compile(String.format(REGEX_START_END, "secteur"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_SECTEUR_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Sect.");
	private static final Pattern FR_CA_STATION_DE_METRO = Pattern.compile(String.format(REGEX_START_END, "Station de m[é|e]tro"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_STATION_DE_METRO_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ston mét.");
	private static final Pattern FR_CA_STATION = Pattern.compile(String.format(REGEX_START_END, "station"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_STATION_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ston");
	private static final Pattern FR_CA_STATIONNEMENT = Pattern.compile(String.format(REGEX_START_END, "stationnement"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_STATIONNEMENT_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Stat");
	private static final Pattern FR_CA_TERRASSE = Pattern.compile(String.format(REGEX_START_END, "terrasse"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_TERRASSE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Tsse");
	private static final Pattern FR_CA_TERRASSES = Pattern.compile(String.format(REGEX_START_END, "terrasses"), Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_TERRASSES_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Tsses");

	public static String cleanStreetTypesFRCA(String string) {
		string = FR_CA_AVENUE.matcher(string).replaceAll(FR_CA_AVENUE_REPLACEMENT);
		string = FR_CA_AUTOROUTE.matcher(string).replaceAll(FR_CA_AUTOROUTE_REPLACEMENT);
		string = FR_CA_BOULEVARD.matcher(string).replaceAll(FR_CA_BOULEVARD_REPLACEMENT);
		string = FR_CA_CARREFOUR.matcher(string).replaceAll(FR_CA_CARREFOUR_REPLACEMENT);
		string = FR_CA_MONTAGNE.matcher(string).replaceAll(FR_CA_MONTAGNE_REPLACEMENT);
		string = FR_CA_MONTEE.matcher(string).replaceAll(FR_CA_MONTEE_REPLACEMENT);
		string = FR_CA_PARC_INDUSTRIEL.matcher(string).replaceAll(FR_CA_PARC_INDUSTRIEL_REPLACEMENT);
		string = FR_CA_RIVIERE.matcher(string).replaceAll(FR_CA_RIVIERE_REPLACEMENT);
		string = FR_CA_SECTEUR.matcher(string).replaceAll(FR_CA_SECTEUR_REPLACEMENT);
		string = FR_CA_STATION_DE_METRO.matcher(string).replaceAll(FR_CA_STATION_DE_METRO_REPLACEMENT);
		string = FR_CA_STATION.matcher(string).replaceAll(FR_CA_STATION_REPLACEMENT);
		string = FR_CA_STATIONNEMENT.matcher(string).replaceAll(FR_CA_STATIONNEMENT_REPLACEMENT);
		string = FR_CA_TERRASSE.matcher(string).replaceAll(FR_CA_TERRASSE_REPLACEMENT);
		string = FR_CA_TERRASSES.matcher(string).replaceAll(FR_CA_TERRASSES_REPLACEMENT);
		return string;
	}
}
