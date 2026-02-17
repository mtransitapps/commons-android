#!/bin/bash
SCRIPT_DIR="$(dirname "$0")"
ROOT_DIR="$SCRIPT_DIR/../.."
source "${ROOT_DIR}/commons/commons.sh"
echo ">> Converting Module App Icon SVG to PNG..."

ROOT_DIR="${SCRIPT_DIR}/../..";

# Creating Long Cast Shadow in Inkscape
# - https://designbundles.net/design-school/how-to-make-a-long-shadow-in-inkscape
# - https://nixdaily.com/how-to/create-long-shadow-effects-in-inkscape/
# - https://www.klaasnotfound.com/2016/09/12/creating-material-icons-with-long-shadows/
# Also:
# - https://icon.kitchen/

requireCommand "xmllint" "libxml2-utils";
requireCommand "jq";

APP_ANDROID_DIR="$ROOT_DIR/app-android";
RES_DIR="$APP_ANDROID_DIR/src/main/res";
GTFS_RDS_VALUES_GEN_FILE="$RES_DIR/values/gtfs_rts_values_gen.xml"; # do not change to avoid breaking compat w/ old modules
BIKE_STATION_VALUES_FILE="$RES_DIR/values/bike_station_values.xml";
AGENCY_JSON_FILE="$ROOT_DIR/config/gtfs/agency.json";
COLOR=""
TYPE=-1
if [ -f $GTFS_RDS_VALUES_GEN_FILE ]; then #1st because color computed
  echo "> Agency file: '$GTFS_RDS_VALUES_GEN_FILE'."
  COLOR=$(xmllint --xpath "//resources/string[@name='gtfs_rts_color']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(xmllint --xpath "//resources/integer[@name='gtfs_rts_agency_type']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
elif [ -f $AGENCY_JSON_FILE ]; then
  echo "> Agency file: '$AGENCY_JSON_FILE'."
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(jq '.target_route_type_id // empty' "$AGENCY_JSON_FILE")
  COLOR=$(jq -r '.default_color // empty' "$AGENCY_JSON_FILE")
elif [ -f $BIKE_STATION_VALUES_FILE ]; then
  echo "> Agency file: '$BIKE_STATION_VALUES_FILE'."
  COLOR=$(xmllint --xpath "//resources/string[@name='bike_station_color']/text()" "$BIKE_STATION_VALUES_FILE")
  TYPE=$(xmllint --xpath "//resources/integer[@name='bike_station_agency_type']/text()" "$BIKE_STATION_VALUES_FILE")
else
  echo "> No agency file! (rds:$GTFS_RDS_VALUES_GEN_FILE|json:$AGENCY_JSON_FILE|bike:$BIKE_STATION_VALUES_FILE)"
  exit 1 #error
fi
echo " - color: '$COLOR'"
echo " - type: '$TYPE'"
if [ -z "$COLOR" ]; then
  echo "> No color found for agency type!"
  exit 1 #error
fi

# https://github.com/mtransitapps/mtransit-for-android/blob/master/app-android/src/main/java/org/mtransit/android/data/DataSourceType.java

SOURCE="$ROOT_DIR/commons-android/pub/module-hi-res-app-icon.svg" # BASE (ALL TYPE LAYERS HIDDEN)
SOURCE=$(case $TYPE in
  "0") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-light-rail.svg" ;;
  "1") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-subway.svg" ;;
  "2") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-train.svg" ;;
  "3") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-bus.svg" ;;
  "4") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-ferry.svg" ;;
  "100") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-bike.svg" ;;
  *)
    echo "> Unexpected agency type '$TYPE'!"
    exit 1 #error;;
    ;;
  esac)

echo " - svg: $SOURCE"
DEST="$APP_ANDROID_DIR/src/main/play/listings/en-US/graphics/icon/1.png"
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
