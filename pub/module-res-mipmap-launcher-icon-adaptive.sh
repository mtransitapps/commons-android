#!/bin/bash
SCRIPT_DIR="$(dirname "$0")"
ROOT_DIR="$SCRIPT_DIR/../.."
source "${ROOT_DIR}/commons/commons.sh"
echo ">> Converting Module Adaptive App Icon to Launcher Icon...";

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

ROOT_DIR="${SCRIPT_DIR}/../..";
APP_ANDROID_DIR="$ROOT_DIR/app-android";
SRC_DIR="${APP_ANDROID_DIR}/src";
MAIN_DIR="${SRC_DIR}/main";
RES_DIR="${MAIN_DIR}/res";

FILE_NAME_XML="module_app_icon.xml"
MIPMAP_ANYDPI="${RES_DIR}/mipmap-anydpi-v26"
FILE_XML="${MIPMAP_ANYDPI}/${FILE_NAME_XML}";

VALUES_DIR="${RES_DIR}/values"
COLOR_FILE_NAME_XML="module_app_icon_color.xml"
COLOR_FILE_XML="${VALUES_DIR}/${COLOR_FILE_NAME_XML}";

mkdir -p "${MIPMAP_ANYDPI}";
checkResult $?;
mkdir -p "${VALUES_DIR}";
checkResult $?;

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

DEST="$RES_DIR/mipmap-xxxhdpi/$DEST_FILE_NAME"
echoDebug " - png: $DEST"
rm -rf "$DEST";
checkResult $?;

requireCommand "convert" "imagemagick";

echoDebug "> Converting SVG to PNG..."
convert \
  -background none \
  "$SOURCE" \
  -gravity center \
  -scale 348x348 \
  -extent 432x432 \
 "$DEST";
RESULT=$?
if [[ ${RESULT} -ne 0 ]]; then
  echo "> Error converting SVG to PNG!"
  exit ${RESULT}
fi
echoDebug "> Converting SVG to PNG... DONE"

echoDebug "> Converting XXXHDPI to other DPIs..."
mkdir -p "$RES_DIR/mipmap-xxhdpi"
convert $DEST -resize 75% $RES_DIR/mipmap-xxhdpi/$DEST_FILE_NAME
checkResult $?;
mkdir -p "$RES_DIR/mipmap-xhdpi"
convert $DEST -resize 50% $RES_DIR/mipmap-xhdpi/$DEST_FILE_NAME
checkResult $?;
mkdir -p "$RES_DIR/mipmap-hdpi"
convert $DEST -resize 37.5% $RES_DIR/mipmap-hdpi/$DEST_FILE_NAME
checkResult $?;
mkdir -p "$RES_DIR/mipmap-mdpi"
convert $DEST -resize 25% $RES_DIR/mipmap-mdpi/$DEST_FILE_NAME
checkResult $?;
echoDebug "> Converting XXXHDPI to other DPIs... DONE"

rm -f "${COLOR_FILE_XML}";
checkResult $?;
touch "${COLOR_FILE_XML}";
checkResult $?;
cat >>"${COLOR_FILE_XML}" <<EOL
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="module_app_icon_color">#${COLOR}</color>
</resources>
EOL

if [[ ${DEBUGING} = true ]]; then
    echoDebug "---------------------------------------------------------------------------------------------------------------"
    cat "${COLOR_FILE_XML}"; #DEBUG
    echoDebug "---------------------------------------------------------------------------------------------------------------"
fi

rm -f "${FILE_XML}";
checkResult $?;
touch "${FILE_XML}";
checkResult $?;
cat >>"${FILE_XML}" <<EOL
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/module_app_icon_color" />
    <foreground android:drawable="@mipmap/module_app_icon_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
EOL

if [[ ${DEBUGING} = true ]]; then
    echoDebug "---------------------------------------------------------------------------------------------------------------"
    cat "${FILE_XML}"; #DEBUG
    echoDebug "---------------------------------------------------------------------------------------------------------------"
fi


echo -e "\n>> Converting Module Adaptive App Icon to Launcher Icon... DONE";
