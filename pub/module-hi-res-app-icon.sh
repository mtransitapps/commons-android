#!/bin/bash
echo ">> Convering Module App Icon SVG to PNG...";

COLOR="";
AGENCY_RTS_FILE=res/values/gtfs_rts_values_gen.xml;
AGENCY_BIKE_FILE=res/values/bike_station_values.xml;
if [ -f $AGENCY_RTS_FILE ]; then
    echo "AGENCY_RTS_FILE: $AGENCY_RTS_FILE";
    COLOR=$(grep -E "<string name=\"gtfs_rts_color\">[0-9A-Z]+</string>$" $AGENCY_RTS_FILE | tr -dc '0-9A-Z');
elif [ -f $AGENCY_BIKE_FILE ]; then
    echo "AGENCY_BIKE_FILE: $AGENCY_BIKE_FILE";
    COLOR=$(grep -E "<string name=\"bike_station_color\">[0-9A-Z]+</string>$" $AGENCY_BIKE_FILE | tr -dc '0-9A-Z');
fi
if [ -z "$COLOR" ] ; then
    echo "Unexpected agency type!";
    exit -1;
fi
echo " - color: $COLOR";

SOURCE="../commons-android/pub/module-hi-res-app-icon.svg";
echo " - svg: $SOURCE";
DEST="pub/hi-res-app-icon.png"
echo " - png: $DEST";

WIDTH=512;
echo " - width: $WIDTH";
HEIGHT=512;
echo " - height: $HEIGHT";

echo "> Running inkscape...";
inkscape --without-gui --export-area-page \
--export-width=$WIDTH --export-height=$HEIGHT \
--export-background=#$COLOR \
--export-png=$DEST $SOURCE;
echo "> Running inkscape... DONE";

echo ">> Convering Module App Icon SVG to PNG... DONE";
