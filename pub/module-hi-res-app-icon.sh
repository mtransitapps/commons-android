#!/bin/bash
echo ">> Converting Module App Icon SVG to PNG..."

COLOR=""
RES_DIR=src/main/res
AGENCY_RTS_FILE=$RES_DIR/values/gtfs_rts_values_gen.xml
AGENCY_BIKE_FILE=$RES_DIR/values/bike_station_values.xml
TYPE=-1
if [ -f $AGENCY_RTS_FILE ]; then
  echo "> Agency file: '$AGENCY_BIKE_FILE'."
  COLOR=$(grep -E "<string name=\"gtfs_rts_color\">[0-9A-Z]+</string>$" $AGENCY_RTS_FILE | tr -dc '0-9A-Z')
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(grep -E "<integer name=\"gtfs_rts_agency_type\">[0-9]+</integer>$" $AGENCY_RTS_FILE | tr -dc '0-9')
elif [ -f $AGENCY_BIKE_FILE ]; then
  echo "> Agency file: '$AGENCY_BIKE_FILE'."
  COLOR=$(grep -E "<string name=\"bike_station_color\">[0-9A-Z]+</string>$" $AGENCY_BIKE_FILE | tr -dc '0-9A-Z')
  TYPE=$(grep -E "<integer name=\"bike_station_agency_type\">[0-9]+</integer>$" $AGENCY_BIKE_FILE | tr -dc '0-9')
else
  echo "> No agency file! (rts:$AGENCY_RTS_FILE|bike:$AGENCY_BIKE_FILE)"
  exit 1 #error
fi
if [ -z "$COLOR" ]; then
  echo "> No color found for agency type!"
  exit 1 #error
fi
echo " - color: $COLOR"
echo " - type: $TYPE"

# https://github.com/mtransitapps/mtransit-for-android/blob/mmathieum/src/main/java/org/mtransit/android/data/DataSourceType.java

SOURCE="../commons-android/pub/module-hi-res-app-icon.svg" # BASE (ALL TYPE LAYERS HIDDEN)
SOURCE=$(case $TYPE in
  "0") echo "../commons-android/pub/module-hi-res-app-icon-light-rail.svg" ;;
  "1") echo "../commons-android/pub/module-hi-res-app-icon-subway.svg" ;;
  "2") echo "../commons-android/pub/module-hi-res-app-icon-train.svg" ;;
  "3") echo "../commons-android/pub/module-hi-res-app-icon-bus.svg" ;;
  "4") echo "../commons-android/pub/module-hi-res-app-icon-ferry.svg" ;;
  "100") echo "../commons-android/pub/module-hi-res-app-icon-bike.svg" ;;
  *)
    echo "> Unexpected agency type '$TYPE'!"
    exit 1 #error;;
    ;;
  esac)

echo " - svg: $SOURCE"
DEST="src/main/play/listings/en-US/graphics/icon/1.png"
echo " - png: $DEST"

WIDTH=512
echo " - width: $WIDTH"
HEIGHT=512
echo " - height: $HEIGHT"

if ! [ -x "$(command -v inkscape)" ]; then
  echo "> Inkscape not installed!"
  exit "${RESULT}"
fi

INKSCAPE_VERSION=$(inkscape --version)
echo "> Inkscape version: $INKSCAPE_VERSION" # requires v1.1+
# https://inkscape.org/doc/inkscape-man.html
# https://wiki.inkscape.org/wiki/index.php/Using_the_Command_Line

echo "> Running inkscape..."
inkscape \
  --export-area-page \
  --export-width=$WIDTH \
  --export-height=$HEIGHT \
  --export-background="#$COLOR" \
  --export-type=png \
  --export-filename=$DEST \
  "$SOURCE"
RESULT=$?
if [[ ${RESULT} -ne 0 ]]; then
  echo "> Error running Inkscape!"
  exit ${RESULT}
fi
echo "> Running inkscape... DONE"

echo ">> Converting Module App Icon SVG to PNG... DONE"
