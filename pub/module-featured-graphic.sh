#!/bin/bash
echo ">> Convering Module Featured Graphic SVG to PNG...";

COLOR="";
RES_DIR=src/main/res;
AGENCY_RTS_FILE=$RES_DIR/values/gtfs_rts_values_gen.xml;
AGENCY_BIKE_FILE=$RES_DIR/values/bike_station_values.xml;
TYPE=-1;
if [ -f $AGENCY_RTS_FILE ]; then
    echo "AGENCY_RTS_FILE: $AGENCY_RTS_FILE";
    COLOR=$(grep -E "<string name=\"gtfs_rts_color\">[0-9A-Z]+</string>$" $AGENCY_RTS_FILE | tr -dc '0-9A-Z');
    # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
    TYPE=$(grep -E "<integer name=\"gtfs_rts_agency_type\">[0-9]+</integer>$" $AGENCY_RTS_FILE | tr -dc '0-9');
elif [ -f $AGENCY_BIKE_FILE ]; then
    echo "AGENCY_BIKE_FILE: $AGENCY_BIKE_FILE";
    COLOR=$(grep -E "<string name=\"bike_station_color\">[0-9A-Z]+</string>$" $AGENCY_BIKE_FILE | tr -dc '0-9A-Z');
    TYPE=100;
else
    echo "No agency file! (rts:$AGENCY_RTS_FILE|bike:$AGENCY_BIKE_FILE)";
    exit -1;
fi
if [ -z "$COLOR" ] ; then
    echo "No color found for agency type!";
    exit -1;
fi
echo " - color: $COLOR";
echo " - type: $TYPE";

# https://github.com/mtransitapps/mtransit-for-android/blob/mmathieum/src/main/java/org/mtransit/android/data/DataSourceType.java

SOURCE_GIT_PATH="pub/module-featured-graphic*.svg";
SOURCE="../commons-android/pub/module-featured-graphic.svg"; # BASE (ALL TYPE LAYERS HIDEEN)
# TODO!
# if [ $TYPE -eq 0 ]; then # LIGHT_RAIL
#     SOURCE="../commons-android/pub/module-featured-graphic-light-rail.svg";
# elif [ $TYPE -eq 1 ]; then # SUBWAY
#     SOURCE="../commons-android/pub/module-featured-graphic-subway.svg";
# elif [ $TYPE -eq 2 ]; then # TRAIN
#     SOURCE="../commons-android/pub/module-featured-graphic-train.svg";
# elif [ $TYPE -eq 3 ]; then # BUS
#     SOURCE="../commons-android/pub/module-featured-graphic-bus.svg";
# elif [ $TYPE -eq 4 ]; then # BUS
#     SOURCE="../commons-android/pub/module-featured-graphic-ferry.svg";
# elif [ $TYPE -eq 100 ]; then # BIKE
#     SOURCE="../commons-android/pub/module-featured-graphic-bike.svg";
# else
#     echo "Unexpected agency type '$TYPE'!";
#     exit -1;
# fi
echo " - svg: $SOURCE";
DEST="src/main/play/listings/en-US/graphics/feature-graphic/1.png";
echo " - png: $DEST";

WIDTH=512;
echo " - width: $WIDTH";
HEIGHT=512;
echo " - height: $HEIGHT";

INKSCAPE_VERSION=$(inkscape --version);
echo "Inkscape version: $INKSCAPE_VERSION"; # requires v1.1+
# https://inkscape.org/doc/inkscape-man.html
# https://wiki.inkscape.org/wiki/index.php/Using_the_Command_Line

git -C ../commons-android checkout $SOURCE_GIT_PATH; #start fresh

sed -i "s/MTAgency/$AGENCY_NAME/g" $SOURCE;
sed -i "s/MTCity/$CITY/g" $SOURCE;
sed -i "s/MTStateCountry/$STATE_COUNTRY/g" $SOURCE;


