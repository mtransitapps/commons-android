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
COLOR=""
TYPE=-1
if [ -f "$GTFS_RDS_VALUES_GEN_FILE" ]; then #1st because color computed
  echoDebug "> Agency file: '$GTFS_RDS_VALUES_GEN_FILE'."
  COLOR=$(xmllint --xpath "//resources/string[@name='gtfs_rts_color']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(xmllint --xpath "//resources/integer[@name='gtfs_rts_agency_type']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
elif [ -f "$AGENCY_JSON_FILE" ]; then
  echoDebug "> Agency file: '$AGENCY_JSON_FILE'."
  # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
  TYPE=$(jq '.target_route_type_id // empty' "$AGENCY_JSON_FILE")
  COLOR=$(jq -r '.default_color // empty' "$AGENCY_JSON_FILE")
elif [ -f "$BIKE_STATION_VALUES_FILE" ]; then
  echoDebug "> Agency file: '$BIKE_STATION_VALUES_FILE'."
  COLOR=$(xmllint --xpath "//resources/string[@name='bike_station_color']/text()" "$BIKE_STATION_VALUES_FILE")
  TYPE=$(xmllint --xpath "//resources/integer[@name='bike_station_agency_type']/text()" "$BIKE_STATION_VALUES_FILE")
else
  echo "> No agency file! (rds:$GTFS_RDS_VALUES_GEN_FILE|json:$AGENCY_JSON_FILE|bike:$BIKE_STATION_VALUES_FILE)"
  exit 1 #error
fi
echoDebug " - color: '$COLOR'"
echoDebug " - type: '$TYPE'"
if [ -z "$COLOR" ]; then
  echo "> No color found for agency type!"
  exit 1 #error
fi
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
echoDebug " - xxxhdpi: $XXXHDPI_DEST"

XXHDPI_DIR="$RES_DIR/mipmap-xxhdpi"
mkdir -p "$XXHDPI_DIR"
checkResult $?;
XXHDPI_DEST="$XXHDPI_DIR/$DEST_FILE_NAME"
echoDebug " - xxhdpi: $XXHDPI_DEST"

XHDPI_DIR="$RES_DIR/mipmap-xhdpi"
mkdir -p "$XHDPI_DIR"
checkResult $?;
XHDPI_DEST="$XHDPI_DIR/$DEST_FILE_NAME"
echoDebug " - xhdpi: $XHDPI_DEST"

HDPI_DIR="$RES_DIR/mipmap-hdpi"
mkdir -p "$HDPI_DIR"
checkResult $?;
HDPI_DEST="$HDPI_DIR/$DEST_FILE_NAME"
echoDebug " - hdpi: $HDPI_DEST"

MDPI_DIR="$RES_DIR/mipmap-mdpi"
mkdir -p "$MDPI_DIR"
checkResult $?;
MDPI_DEST="$MDPI_DIR/$DEST_FILE_NAME"
echoDebug " - mdpi: $MDPI_DEST"

if [ -f "$XXXHDPI_DEST" ]; then
  if [[ ${MT_GENERATE_IMAGES} == true ]]; then
    echo ">> File '$XXXHDPI_DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
    rm -f "$XXXHDPI_DEST";
    checkResult $?;
  else
    echo ">> File '$XXXHDPI_DEST' already exist."; # compat with existing mipmap-xxxhdpi/module_app_icon_foreground.png
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
    "$XXXHDPI_DEST";
  RESULT=$?
  if [[ ${RESULT} -ne 0 ]]; then
    echo "> Error converting SVG to PNG!"
    exit ${RESULT}
  fi
  echoDebug "> Converting SVG to XXXHDPI PNG... DONE"
fi

if [ -f "$XXHDPI_DEST" ]; then
  if [[ ${MT_GENERATE_IMAGES} == true ]]; then
    echo ">> File '$XXHDPI_DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
    rm -f "$XXHDPI_DEST";
    checkResult $?;
  else
    echo ">> File '$XXHDPI_DEST' already exist."; # compat with existing mipmap-xxhdpi/module_app_icon_foreground.png
  fi
fi
if [ ! -f "$XXHDPI_DEST" ]; then
  echoDebug "> Converting XXXHDPI to XXHDPI..."
  requireCommand "inkscape"; # used by imagemagick to convert SVG properly
  requireCommand "convert" "imagemagick";
  convert $XXXHDPI_DEST -resize 75% $XXHDPI_DEST
  checkResult $?;
  echoDebug "> Converting XXXHDPI to XXHDPI... DONE"
fi

if [ -f "$XHDPI_DEST" ]; then
  if [[ ${MT_GENERATE_IMAGES} == true ]]; then
    echo ">> File '$XHDPI_DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
    rm -f "$XHDPI_DEST";
    checkResult $?;
  else
    echo ">> File '$XHDPI_DEST' already exist."; # compat with existing mipmap-xhdpi/module_app_icon_foreground.png
  fi
fi
if [ ! -f "$XHDPI_DEST" ]; then
  echoDebug "> Converting XXXHDPI to XHDPI..."
  requireCommand "inkscape"; # used by imagemagick to convert SVG properly
  requireCommand "convert" "imagemagick";
  convert $XXXHDPI_DEST -resize 50% $XHDPI_DEST
  checkResult $?;
  echoDebug "> Converting XXXHDPI to XHDPI... DONE"
fi

if [ -f "$HDPI_DEST" ]; then
  if [[ ${MT_GENERATE_IMAGES} == true ]]; then
    echo ">> File '$HDPI_DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
    rm -f "$HDPI_DEST";
    checkResult $?;
  else
    echo ">> File '$HDPI_DEST' already exist."; # compat with existing mipmap-hdpi/module_app_icon_foreground.png
  fi
fi
if [ ! -f "$HDPI_DEST" ]; then
  echoDebug "> Converting XXXHDPI to HDPI..."
  requireCommand "inkscape"; # used by imagemagick to convert SVG properly
  requireCommand "convert" "imagemagick";
  convert $XXXHDPI_DEST -resize 37.5% $HDPI_DEST
  checkResult $?;
  echoDebug "> Converting XXXHDPI to HDPI... DONE"
fi

if [ -f "$MDPI_DEST" ]; then
  if [[ ${MT_GENERATE_IMAGES} == true ]]; then
    echo ">> File '$MDPI_DEST' already exist: overriding image... (MT_GENERATE_IMAGES=$MT_GENERATE_IMAGES)";
    rm -f "$MDPI_DEST";
    checkResult $?;
  else
    echo ">> File '$MDPI_DEST' already exist."; # compat with existing mipmap-mdpi/module_app_icon_foreground.png
  fi
fi
if [ ! -f "$MDPI_DEST" ]; then
  echoDebug "> Converting XXXHDPI to MDPI..."
  requireCommand "inkscape"; # used by imagemagick to convert SVG properly
  requireCommand "convert" "imagemagick";
  convert $XXXHDPI_DEST -resize 25% $MDPI_DEST
  checkResult $?;
  echoDebug "> Converting XXXHDPI to MDPI... DONE"
fi

echo -e "\n>> Creating Module Adaptive Launcher Icon... DONE";
