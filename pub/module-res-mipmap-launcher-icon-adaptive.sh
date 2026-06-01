#!/bin/bash
SCRIPT_DIR="$(dirname "$0")"
ROOT_DIR="$SCRIPT_DIR/../.."
source "${ROOT_DIR}/commons/commons.sh"
echo ">> Creating Module Adaptive Launcher Icon...";

setIsCI;

DEBUGING=$IS_CI;
# DEBUGING=true;
function echoDebug() {
	if [[ $DEBUGING == true ]]; then
		echo "$@";
	else
		echo -n ".";
	fi
}

APP_ANDROID_DIR="$ROOT_DIR/app-android";
SRC_DIR="${APP_ANDROID_DIR}/src";
MAIN_DIR="${SRC_DIR}/main";
RES_DIR="${MAIN_DIR}/res";

requireCommand "xmllint" "libxml2-utils";
requireCommand "jq";

GTFS_RDS_VALUES_GEN_FILE="$RES_DIR/values/gtfs_rts_values_gen.xml"; # do not change to avoid breaking compat w/ old modules
BIKE_STATION_VALUES_FILE="$RES_DIR/values/bike_station_values.xml";
AGENCY_JSON_FILE="$ROOT_DIR/config/gtfs/agency.json";
TYPE=-1
if [ -f "$GTFS_RDS_VALUES_GEN_FILE" ]; then #1st because color computed
  echoDebug "> Agency file: '$GTFS_RDS_VALUES_GEN_FILE'."
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(xmllint --xpath "//resources/integer[@name='gtfs_rts_agency_type']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
elif [ -f "$AGENCY_JSON_FILE" ]; then
  echoDebug "> Agency file: '$AGENCY_JSON_FILE'."
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(jq '.target_route_type_id // empty' "$AGENCY_JSON_FILE")
elif [ -f "$BIKE_STATION_VALUES_FILE" ]; then
  echoDebug "> Agency file: '$BIKE_STATION_VALUES_FILE'."
  TYPE=$(xmllint --xpath "//resources/integer[@name='bike_station_agency_type']/text()" "$BIKE_STATION_VALUES_FILE")
else
  echo "> No agency file! (rds:$GTFS_RDS_VALUES_GEN_FILE|json:$AGENCY_JSON_FILE|bike:$BIKE_STATION_VALUES_FILE)"
  exit 1 #error
fi
echoDebug " - type: '$TYPE'"
if [ -z "$TYPE" ]; then
  echo " > No type found for agency!"
  exit 1 # error
fi

SOURCE=$(case $TYPE in
  "0") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-light-rail.svg" ;;
  "1") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-subway.svg" ;;
  "2") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-train.svg" ;;
  "3") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-bus.svg" ;;
  "4") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-ferry.svg" ;;
  "100") echo "$ROOT_DIR/commons-android/pub/module-hi-res-app-icon-bike.svg" ;;
  *)
    echo "> Unexpected agency type '$TYPE'!"
    exit 1 #error
    ;;
  esac)
echoDebug " - svg: $SOURCE"

# Size all layers to 108x108 dp.
# - xxxhdpi (4x) = 432x432 px
# - xxhdpi (3x) = 324x324 px
# - xhdpi (2x) = 216x216 px
# - hdpi (1.5x) = 162x162 px
# - mdpi (1x) = 108x108 px
# Use a logo that's at least 48x48 dp. 
# - xxxhdpi (4x) = 192x192 px
# - xxhdpi (3x) = 144x144 px
# - xhdpi (2x) = 96x96 px
# - hdpi (1.5x) = 72x72 px
# - mdpi (1x) = 48x48 px
# It must not exceed 66x66 dp, because the inner 66x66 dp of the icon appears within the masked viewport.
# - xxxhdpi (4x) = 264x264 px
# - xxhdpi (3x) = 198x198 px
# - xhdpi (2x) = 132x132 px
# - hdpi (1.5x) = 99x99 px
# - mdpi (1x) = 66x66 px

DEST_FILE_NAME="module_app_icon_foreground.png"

XXXHDPI_DIR="$RES_DIR/mipmap-xxxhdpi"
mkdir -p "$XXXHDPI_DIR"
checkResult $?;
XXXHDPI_DEST="$XXXHDPI_DIR/$DEST_FILE_NAME"
if [ -f "$XXXHDPI_DEST" ]; then
  if [[ ${MT_GENERATE_IMAGES} == true ]]; then
    echo ">> XXXHDPI File '$XXXHDPI_DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
    rm -f "$XXXHDPI_DEST";
    checkResult $?;
  else
    echo ">> XXXHDPI File '$XXXHDPI_DEST' already exist."; # compat with existing mipmap-xxxhdpi/module_app_icon_foreground.png
  fi
fi
if [ ! -f "$XXXHDPI_DEST" ]; then
  requireCommand "inkscape"; # used by imagemagick to convert SVG properly
  requireCommand "convert" "imagemagick";
  echoDebug "> Converting SVG to XXXHDPI PNG..."
  convert \
    -background none \
    "$SOURCE" \
    -gravity center \
    -scale 348x348 \
    -extent 432x432 \
    +set date:create +set date:modify \
    "$XXXHDPI_DEST";
  RESULT=$?
  if [[ ${RESULT} -ne 0 ]]; then
    echo "> Error converting SVG to PNG!"
    exit ${RESULT}
  fi
  echoDebug "> Converting SVG to XXXHDPI PNG... DONE"
fi

for DENSITY in xxhdpi xhdpi hdpi mdpi; do
  case "$DENSITY" in
    xxhdpi) DEST_DIR="$RES_DIR/mipmap-xxhdpi"; RES="75%";;
    xhdpi) DEST_DIR="$RES_DIR/mipmap-xhdpi"; RES="50%";;
    hdpi) DEST_DIR="$RES_DIR/mipmap-hdpi"; RES="37.5%";;
    mdpi) DEST_DIR="$RES_DIR/mipmap-mdpi"; RES="25%";;
  esac
  mkdir -p "$DEST_DIR";
  checkResult $?;
  DEST="$DEST_DIR/$DEST_FILE_NAME"
  if [ -f "$DEST" ]; then
    if [[ ${MT_GENERATE_IMAGES} == true ]]; then
      echo ">> ${DENSITY^^} File '$DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
      rm -f "$DEST";
      checkResult $?;
    else
      echo ">> ${DENSITY^^} File '$DEST' already exist.";
    fi
  fi
  if [ ! -f "$DEST" ]; then
    echoDebug "> Converting XXXHDPI to ${DENSITY^^}..."
    requireCommand "convert" "imagemagick";
    convert "$XXXHDPI_DEST" -resize "$RES" +set date:create +set date:modify "$DEST"
    checkResult $?;
    echoDebug "> Converting XXXHDPI to ${DENSITY^^}... DONE"
  fi
done

echo -e "\n>> Creating Module Adaptive Launcher Icon... DONE";
