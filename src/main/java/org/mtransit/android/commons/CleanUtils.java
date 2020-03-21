package org.mtransit.android.commons;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class CleanUtils {

	private CleanUtils() {
	}

	private static final Pattern CLEAN_SPACES = Pattern.compile("\\s+");
	private static final Pattern CLEAN_P1 = Pattern.compile("\\([\\s]+");
	private static final String CLEAN_P1_REPLACEMENT = "(";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]+\\)");
	private static final String CLEAN_P2_REPLACEMENT = ")";

	@NonNull
	public static String cleanLabel(@NonNull String label) {
		label = CLEAN_SPACES.matcher(label).replaceAll(SPACE);
		label = CLEAN_P1.matcher(label).replaceAll(CLEAN_P1_REPLACEMENT);
		label = CLEAN_P2.matcher(label).replaceAll(CLEAN_P2_REPLACEMENT);
		label = WordUtils.capitalize(label, SPACE_CHAR, '-', '–', '/', '(', '.');
		return label.trim();
	}

	public static Pattern cleanWords(String... words) {
		if (words == null || words.length <= 0) {
			throw new RuntimeException("Cannot clean empty list of words!");
		}
		StringBuilder sb = new StringBuilder("((^|\\W)(");
		boolean firstWorld = true;
		for (String word : words) {
			if (!firstWorld) {
				sb.append('|');
			}
			sb.append(word);
			firstWorld = false;
		}
		sb.append(")(\\W|$))");
		return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
	}

	public static String cleanWordsReplacement(String replacement) {
		if (replacement == null || replacement.length() <= 0) {
			return StringUtils.EMPTY;
		}
		return "$2" + replacement + "$4";
	}

	public static Pattern cleanWordsPlural(String... words) {
		if (words == null || words.length <= 0) {
			throw new RuntimeException("Cannot clean empty list of words!");
		}
		StringBuilder sb = new StringBuilder("((^|\\W)((");
		boolean firstWorld = true;
		for (String word : words) {
			if (!firstWorld) {
				sb.append('|');
			}
			sb.append(word);
			firstWorld = false;
		}
		sb.append(")([s]?))(\\W|$))");
		return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
	}

	public static String cleanWordsReplacementPlural(String replacement) {
		if (replacement == null || replacement.length() <= 0) {
			return StringUtils.EMPTY;
		}
		return "$2" + replacement + "$5" + "$6";
	}

	private static final String PLACE_CHAR_DE_L = "de l'";
	private static final String PLACE_CHAR_DE_LA = "de la ";
	private static final String PLACE_CHAR_D = "d'";
	private static final String PLACE_CHAR_DE = "de ";
	private static final String PLACE_CHAR_DES = "des ";
	private static final String PLACE_CHAR_DU = "du ";
	private static final String PLACE_CHAR_LA = "la ";
	private static final String PLACE_CHAR_LE = "le ";
	private static final String PLACE_CHAR_LES = "les ";
	private static final String PLACE_CHAR_L = "l'";

	private static final Pattern[] START_WITH_CHARS = new Pattern[]{ //
			Pattern.compile("^(" + PLACE_CHAR_DE_L + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DE_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_D + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DES + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DU + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_LE + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("^(" + PLACE_CHAR_LES + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("^(" + PLACE_CHAR_L + ")", Pattern.CASE_INSENSITIVE) //
	};

	public static final Pattern[] SPACE_CHARS = new Pattern[]{ //
			Pattern.compile("( " + PLACE_CHAR_DE_L + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DE_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_D + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DES + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DU + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_LE + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("( " + PLACE_CHAR_LES + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("( " + PLACE_CHAR_L + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SLASH_CHARS = new Pattern[]{//
			Pattern.compile("(/ " + PLACE_CHAR_DE_L + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DE_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_D + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DES + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DU + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_LE + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("(/ " + PLACE_CHAR_LES + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("(/ " + PLACE_CHAR_L + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final String PLACE_CHAR_ARRONDISSEMENT = "arrondissement ";
	private static final String PLACE_CHAR_AV = "av. ";
	private static final String PLACE_CHAR_AVENUE = "avenue ";
	private static final String PLACE_CHAR_BOUL = "boul. ";
	private static final String PLACE_CHAR_BOULEVARD = "boulevard ";
	private static final String PLACE_CHAR_CH = "ch. ";
	private static final String PLACE_CHAR_CIVIQUE = "civique ";
	private static final String PLACE_CHAR_CROISS = "croiss. ";
	private static final String PLACE_CHAR_QUARTIER = "quartier ";
	private static final String PLACE_CHAR_RTE = "rte ";
	private static final String PLACE_CHAR_RUE = "rue ";
	private static final String PLACE_CHAR_TSSE = "tsse ";

	private static final Pattern[] START_WITH_ST = new Pattern[]{ //
			Pattern.compile("^(" + PLACE_CHAR_ARRONDISSEMENT + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_AV + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_AVENUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_BOUL + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_BOULEVARD + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_CH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_CIVIQUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_CROISS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_QUARTIER + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_RTE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_RUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_TSSE + ")", Pattern.CASE_INSENSITIVE) //
	};

	public static final Pattern[] SPACE_ST = new Pattern[]{ //
			Pattern.compile("( " + PLACE_CHAR_ARRONDISSEMENT + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_AV + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_AVENUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_BOUL + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_BOULEVARD + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_CH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_CIVIQUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_CROISS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_QUARTIER + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_RTE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_RUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_TSSE + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SLASH_ST = new Pattern[]{ //
			Pattern.compile("(/ " + PLACE_CHAR_ARRONDISSEMENT + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_AV + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_AVENUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_BOUL + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_BOULEVARD + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_CH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_CIVIQUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_CROISS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_QUARTIER + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_RTE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_RUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_TSSE + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final String PARENTHESE1 = "\\(";
	private static final String PARENTHESE2 = "\\)";

	public static final Pattern CLEAN_PARENTHESE1 = Pattern.compile("[" + PARENTHESE1 + "][\\s]*(\\w)");
	public static final String CLEAN_PARENTHESE1_REPLACEMENT = PARENTHESE1 + "$1";
	public static final Pattern CLEAN_PARENTHESE2 = Pattern.compile("(\\w)[\\s]*[" + PARENTHESE2 + "]");
	public static final String CLEAN_PARENTHESE2_REPLACEMENT = "$1" + PARENTHESE2;

	public static final String SLASH_SPACE = "/ ";

	@NonNull
	public static String cleanLabelFR(@NonNull String label) {
		label = cleanSlashes(label);
		label = CLEAN_PARENTHESE1.matcher(label).replaceAll(CLEAN_PARENTHESE1_REPLACEMENT);
		label = CLEAN_PARENTHESE2.matcher(label).replaceAll(CLEAN_PARENTHESE2_REPLACEMENT);
		label = SAINT.matcher(label).replaceAll(SAINT_REPLACEMENT);
		label = StringUtils.replaceAll(label.trim(), START_WITH_ST, SPACE); // StringUtils.EMPTY); // SPACE);
		label = StringUtils.replaceAll(label, SLASH_ST, SLASH_SPACE);
		label = StringUtils.replaceAll(label.trim(), START_WITH_CHARS, SPACE); // , StringUtils.EMPTY); //
		label = StringUtils.replaceAll(label, SLASH_CHARS, SLASH_SPACE);
		return cleanLabel(label);
	}

	public static final Pattern SAINT = Pattern.compile("(saint)", Pattern.CASE_INSENSITIVE);
	public static final String SAINT_REPLACEMENT = "St";

	public static final Pattern CLEAN_AT = cleanWords("at");
	public static final String CLEAN_AT_REPLACEMENT = cleanWordsReplacement("@");

	public static final Pattern CLEAN_AND = cleanWords("and");
	public static final String CLEAN_AND_REPLACEMENT = cleanWordsReplacement("&");

	public static final Pattern CLEAN_ET = cleanWords("et");
	public static final String CLEAN_ET_REPLACEMENT = CLEAN_AND_REPLACEMENT;

	private static final String SPACE = " ";
	private static final char SPACE_CHAR = ' ';

	private static final Pattern CLEAN_SLASH = Pattern.compile("(\\S)[\\s]*[/][\\s]*(\\S)");
	private static final String CLEAN_SLASH_REPLACEMENT = "$1 / $2";

	@NonNull
	public static String cleanSlashes(@NonNull String string) {
		return CLEAN_SLASH.matcher(string).replaceAll(CLEAN_SLASH_REPLACEMENT);
	}

	private static final Pattern POINT1 = Pattern.compile("((^|\\W)([\\w])\\.(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String POINT1_REPLACEMENT = "$2" + "$3" + "$4";

	private static final Pattern POINTS = Pattern.compile("((^|\\W)([\\w]+)\\.(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String POINTS_REPLACEMENT = "$2" + "$3" + "$4";

	@NonNull
	public static String removePoints(@NonNull String string) {
		string = POINT1.matcher(string).replaceAll(POINT1_REPLACEMENT);
		string = POINTS.matcher(string).replaceAll(POINTS_REPLACEMENT);
		return string;
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("((^.* |^)to )", Pattern.CASE_INSENSITIVE);

	@NonNull
	public static String keepTo(@NonNull String string) {
		string = STARTS_WITH_TO.matcher(string).replaceAll(StringUtils.EMPTY);
		return string;
	}

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("( via .*$)", Pattern.CASE_INSENSITIVE);

	@NonNull
	public static String removeVia(@NonNull String string) {
		string = ENDS_WITH_VIA.matcher(string).replaceAll(StringUtils.EMPTY);
		return string;
	}

	@NonNull
	public static String keepToAndRemoveVia(@NonNull String string) {
		string = keepTo(string);
		string = removeVia(string);
		return string;
	}

	// TODO white-space VS non-word?
	private static final Pattern FIRST = cleanWords("first");
	private static final String FIRST_REPLACEMENT = cleanWordsReplacement("1st");
	private static final Pattern SECOND = cleanWords("second");
	private static final String SECOND_REPLACEMENT = cleanWordsReplacement("2nd");
	private static final Pattern THIRD = cleanWords("third");
	private static final String THIRD_REPLACEMENT = cleanWordsReplacement("3rd");
	private static final Pattern FOURTH = cleanWords("fourth");
	private static final String FOURTH_REPLACEMENT = cleanWordsReplacement("4th");
	private static final Pattern FIFTH = cleanWords("fifth");
	private static final String FIFTH_REPLACEMENT = cleanWordsReplacement("5th");
	private static final Pattern SIXTH = cleanWords("sixth");
	private static final String SIXTH_REPLACEMENT = cleanWordsReplacement("6th");
	private static final Pattern SEVENTH = cleanWords("seventh");
	private static final String SEVENTH_REPLACEMENT = cleanWordsReplacement("7th");
	private static final Pattern EIGHTH = cleanWords("eighth");
	private static final String EIGHTH_REPLACEMENT = cleanWordsReplacement("8th");
	private static final Pattern NINTH = cleanWords("ninth");
	private static final String NINTH_REPLACEMENT = cleanWordsReplacement("9th");

	@NonNull
	public static String cleanNumbers(@NonNull String string) {
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

	@NonNull
	public static String cleanStreetTypes(@NonNull String string) {
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

	@NonNull
	public static String cleanStreetTypesFRCA(@NonNull String string) {
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
