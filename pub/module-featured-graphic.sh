#!/bin/bash
echo ">> Convering Module Featured Graphic SVG to PNG '$@'...";

if [[ "$#" -lt 3 || "$#" -gt 4 ]]; then
    echo "> Wrong $# parameters '$@'!";
    echo "> Ex: 'app-android: ../commons-android/pub/module-featured-graphic.sh \"STM\" \"Montréal\" \"QC, Canada\";'";
    echo "> Ex: 'app-android: ../commons-android/pub/module-featured-graphic.sh \"Société de Transport\" \"de Montréal\" \"Montréal\" \"QC, Canada\";'";
    exit -1;
fi

AGENCY_NAME_1="";
AGENCY_NAME_2="";
CITY="";
STATE_COUNTRY="";
if [[ "$#" -eq 3 ]]; then
    AGENCY_NAME_1=$1;
    AGENCY_NAME_2="";
    CITY=$2;
    STATE_COUNTRY=$3;
else
    AGENCY_NAME_1=$1;
    AGENCY_NAME_2=$2;
    CITY=$3;
    STATE_COUNTRY=$4;
fi

MAX_AGENCY_LENGTH=17;

MAX_CITY_LENGTH=77;

if [[ -z "${AGENCY_NAME_1}" ]]; then
    echo "> No agency name provided '$AGENCY_NAME_1'!";
    exit 1;
elif [ "${#AGENCY_NAME_1}" -gt "$MAX_AGENCY_LENGTH" ]; then
    echo "> Provided agency name '$AGENCY_NAME_1'(${#AGENCY_NAME_1}) is too long (max:$MAX_AGENCY_LENGTH)!";
    exit 1;
fi
if [[ "$#" -eq 4 ]]; then
    if [[ -z "${AGENCY_NAME_2}" ]]; then
        echo "> No agency name provided '$AGENCY_NAME_2'!";
        exit 1;
    elif [ "${#AGENCY_NAME_2}" -gt "$MAX_AGENCY_LENGTH" ]; then
        echo "> Provided agency name '$AGENCY_NAME_2'(${#AGENCY_NAME_2}) is too long (max:$MAX_AGENCY_LENGTH)!";
        exit 1;
    fi
fi
if [[ -z "${CITY}" ]]; then
    echo "> No city provided '$CITY'!";
    exit 1;
elif [ "${#CITY}" -gt "$MAX_CITY_LENGTH" ]; then
    echo "> Provided city '$CITY'(${#CITY}) is too long (max:$MAX_CITY_LENGTH)!";
    exit 1;
fi
if [[ -z "${STATE_COUNTRY}" ]]; then
    echo "> No state, country provided '$STATE_COUNTRY'!";
    exit 1;
fi
echo " - agency name: '$AGENCY_NAME_1' '$AGENCY_NAME_2'";
echo " - city: '$CITY'";
echo " - state & country: '$STATE_COUNTRY'";

COLOR="";
RES_DIR=src/main/res;
AGENCY_RTS_FILE=$RES_DIR/values/gtfs_rts_values_gen.xml;
AGENCY_BIKE_FILE=$RES_DIR/values/bike_station_values.xml;
TYPE=-1;
if [ -f $AGENCY_RTS_FILE ]; then
    echo "> Agency file: '$AGENCY_RTS_FILE'.";
    COLOR=$(grep -E "<string name=\"gtfs_rts_color\">[0-9A-Z]+</string>$" $AGENCY_RTS_FILE | tr -dc '0-9A-Z');
    # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
    TYPE=$(grep -E "<integer name=\"gtfs_rts_agency_type\">[0-9]+</integer>$" $AGENCY_RTS_FILE | tr -dc '0-9');
elif [ -f $AGENCY_BIKE_FILE ]; then
    echo "> Agency file: '$AGENCY_BIKE_FILE'.";
    COLOR=$(grep -E "<string name=\"bike_station_color\">[0-9A-Z]+</string>$" $AGENCY_BIKE_FILE | tr -dc '0-9A-Z');
    TYPE=$(grep -E "<integer name=\"bike_station_agency_type\">[0-9]+</integer>$" $AGENCY_BIKE_FILE | tr -dc '0-9');
else
    echo " > No agency file! (rts:$AGENCY_RTS_FILE|bike:$AGENCY_BIKE_FILE)";
    exit -1;
fi
if [ -z "$COLOR" ] ; then
    echo " > No color found for agency type!";
    exit -1;
fi
echo " - color: $COLOR";
echo " - type: $TYPE";

# https://github.com/mtransitapps/mtransit-for-android/blob/mmathieum/src/main/java/org/mtransit/android/data/DataSourceType.java

SOURCE_GIT_PATH="pub/module-featured-graphic*.svg";
SOURCE="../commons-android/pub/module-featured-graphic.svg"; # BASE (ALL TYPE LAYERS HIDEEN)
if [ $TYPE -eq 0 ]; then # LIGHT_RAIL
    SOURCE="../commons-android/pub/module-featured-graphic-light-rail.svg";
    if [[ "$#" -eq 4 ]]; then
        SOURCE="../commons-android/pub/module-featured-graphic-light-rail-2.svg";
    fi
elif [ $TYPE -eq 1 ]; then # SUBWAY
    SOURCE="../commons-android/pub/module-featured-graphic-subway.svg";
    if [[ "$#" -eq 4 ]]; then
        SOURCE="../commons-android/pub/module-featured-graphic-subway-2.svg";
    fi
elif [ $TYPE -eq 2 ]; then # TRAIN
    SOURCE="../commons-android/pub/module-featured-graphic-train.svg";
    if [[ "$#" -eq 4 ]]; then
        SOURCE="../commons-android/pub/module-featured-graphic-train-2.svg";
    fi
elif [ $TYPE -eq 3 ]; then # BUS
    SOURCE="../commons-android/pub/module-featured-graphic-bus.svg";
    if [[ "$#" -eq 4 ]]; then
        SOURCE="../commons-android/pub/module-featured-graphic-bus-2.svg";
    fi
elif [ $TYPE -eq 4 ]; then # FERRY
    SOURCE="../commons-android/pub/module-featured-graphic-ferry.svg";
    if [[ "$#" -eq 4 ]]; then
        SOURCE="../commons-android/pub/module-featured-graphic-ferry-2.svg";
    fi
elif [ $TYPE -eq 100 ]; then # BIKE
    SOURCE="../commons-android/pub/module-featured-graphic-bike.svg";
    if [[ "$#" -eq 4 ]]; then
        SOURCE="../commons-android/pub/module-featured-graphic-bike-2.svg";
    fi
else
    echo "Unexpected agency type '$TYPE'!";
    exit -1;
fi
echo " - svg: $SOURCE";
DEST="src/main/play/listings/en-US/graphics/feature-graphic/1.png";
echo " - png: $DEST";

WIDTH=1024;
echo " - width: $WIDTH";
HEIGHT=500;
echo " - height: $HEIGHT";

FONT_INSTALLED=$(fc-list | grep -i roboto | grep -i condensed); # Roboto Condensed
if [[ -z "${FONT_INSTALLED}" ]]; then
    echo "> Font not installed! ('$FONT_INSTALLED')";
    exit 1;
fi

INKSCAPE_VERSION=$(inkscape --version);
echo "> Inkscape version: $INKSCAPE_VERSION"; # requires v1.1+
# https://inkscape.org/doc/inkscape-man.html
# https://wiki.inkscape.org/wiki/index.php/Using_the_Command_Line

echo "> Resetting file...";
git -C ../commons-android checkout $SOURCE_GIT_PATH; #start fresh
echo "> Resetting file... DONE";

echo "> Setting file strings...";

if [[ "$#" -eq 3 ]]; then
    sed -i "s/MTAgency/$AGENCY_NAME_1/g" $SOURCE;
else
    sed -i "s/MTAgency1/$AGENCY_NAME_1/g" $SOURCE;
    sed -i "s/MTAgency2/$AGENCY_NAME_2/g" $SOURCE;
fi
sed -i "s/MTCity/$CITY/g" $SOURCE;
sed -i "s/MTStateCountry/$STATE_COUNTRY/g" $SOURCE;
git -C ../commons-android diff $SOURCE_GIT_PATH;
echo "> Setting file strings... DONE";

echo "> Running inkscape...";
inkscape \
--export-area-page \
--export-width=$WIDTH \
--export-height=$HEIGHT \
--export-background=#$COLOR \
--export-type=png \
--export-filename=$DEST \
$SOURCE;
RESULT=$?;
if [[ ${RESULT} -ne 0 ]]; then
    echo "> Error running Inkscape!";
    exit ${RESULT};
fi
echo "> Running inkscape... DONE";

echo "> Resetting file...";
git -C ../commons-android checkout $SOURCE_GIT_PATH; #start fresh
echo "> Resetting file... DONE";

echo ">> Convering Module Featured Graphic SVG to PNG '$@'... DONE";
