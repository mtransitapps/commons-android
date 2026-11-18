#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd);

source "${ROOT_DIR}/commons/sh/commons.sh";

echo ">> Capturing Module App Screenshot '$*'..."

if [[ "$#" -ne 3 ]]; then
  echo "> Wrong $# parameters '$*'!"
  echo "- Ex: 'app-android: ../commons-android/pub/module-app-screenshot.sh en-US phone 1'"
  echo "- Ex: 'app-android: ../commons-android/pub/module-app-screenshot.sh fr-FR phone 2'"
  exit 1 #error
fi

setIsCI;

LANG=$1
TYPE=$2
NUMBER=$3

DEBUG=false
# DEBUG=true; # DEBUG

DEVICE_REBOOT_ALLOWED=false
# DEVICE_REBOOT_ALLOWED=true; # use to switch time format (12/24), time-zone...

if [[ -z "${LANG}" ]]; then
  echo "> No lang provided '$LANG'!"
  exit 1 #error
fi
if [[ -z "${TYPE}" ]]; then
  echo "> No device type provided '$TYPE'!"
  exit 1 #error
fi
if [[ -z "${NUMBER}" ]]; then
  echo "> No screenshot number provided '$NUMBER'!"
  exit 1 #error
fi
echo " - lang: '$LANG'"
echo " - type: '$TYPE'"
echo " - number: '$NUMBER'"

if [[ "${LANG}" != "en-US" && "${LANG}" != "fr-FR" ]]; then
  echo "> Invalid lang provided '$LANG'!"
  exit 1 #error
fi

if [[ "${TYPE}" != "phone" ]]; then
  echo "> Invalid type provided '$TYPE'!"
  exit 1 #error
fi

APP_ANDROID_DIR="$ROOT_DIR/app-android"
GRAPHICS_DIR="$APP_ANDROID_DIR/src/main/play/listings/$LANG/graphics"
mkdir -p "$GRAPHICS_DIR";
DEST_DIR="$GRAPHICS_DIR/$TYPE-screenshots"
mkdir -p "$DEST_DIR";

if [ ! -d "$DEST_DIR" ]; then
  echo "> Destination directory does NOT exist '$DEST_DIR'!"
  exit 1 #error
fi

MAIN_PKG="org.mtransit.android"
if [ "$DEBUG" = true ]; then
  MAIN_PKG="$MAIN_PKG.debug" #DEBUG
fi
SPLASH_SCREEN_ACTIVITY="org.mtransit.android.ui.SplashScreenActivity"

MAIN_DIR="$APP_ANDROID_DIR/src/main"
DEBUG_DIR="$APP_ANDROID_DIR/src/debug"
if [[ ! -d "$DEBUG_DIR" ]]; then
  DEBUG_DIR="$MAIN_DIR"
fi
RES_DIR=$MAIN_DIR/res
CONFIG_DIR="${ROOT_DIR}/config";

CONFIG_PKG_FILE=$CONFIG_DIR/pkg
MODULE_PKG=""
if [ -f $CONFIG_PKG_FILE ]; then
  echo " - PKG config: '$CONFIG_PKG_FILE'."
  MODULE_PKG=$(cat $CONFIG_PKG_FILE)
  if [ "$DEBUG" = true ]; then
    MODULE_PKG="$MODULE_PKG.debug" #DEBUG
  fi
else
  echo " > No PKG config file! (file:$CONFIG_PKG_FILE)"
  exit 1 #error
fi
if [[ -z "${MODULE_PKG}" ]]; then
  echo "> No module APK found '$MODULE_PKG'!"
  exit 1 #error
fi

PROJECT_PKG_FILE="$CONFIG_DIR/pkg"
PKG=$(cat $PROJECT_PKG_FILE)
if [ "$DEBUG" = true ]; then
  PKG="$PKG.debug"
fi

requireCommand "xmllint" "libxml2-utils";

GTFS_RDS_VALUES_GEN_FILE="$RES_DIR/values/gtfs_rts_values_gen.xml" # do not change to avoid breaking compat w/ old modules
BIKE_STATION_VALUES_FILE="$RES_DIR/values/bike_station_values.xml"
AGENCY_TIME_ZONE=""
if [ -f $GTFS_RDS_VALUES_GEN_FILE ]; then
  echo " - using agency file: '$GTFS_RDS_VALUES_GEN_FILE'."
  AGENCY_TIME_ZONE=$(xmllint --xpath "//resources/string[@name='gtfs_rts_timezone']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
  FILTER_AGENCY_AUTHORITY="$PKG.gtfs"
elif [ -f $BIKE_STATION_VALUES_FILE ]; then
  echo " - using agency file: '$BIKE_STATION_VALUES_FILE'."
  AGENCY_TIME_ZONE="" # does NOT matter for bike
  FILTER_AGENCY_AUTHORITY="$PKG.bike"
else
  echo "> No agency file! (rds:$GTFS_RDS_VALUES_GEN_FILE|bike:$BIKE_STATION_VALUES_FILE)"
  exit 1 #error
fi
if [[ -z "${FILTER_AGENCY_AUTHORITY}" ]]; then
  echo "> No agency authority found '$FILTER_AGENCY_AUTHORITY'!"
  exit 1 #error
fi

echo " - agency authority: '$FILTER_AGENCY_AUTHORITY'"

FILTER_SCREEN="home"
if [[ "$NUMBER" -eq 1 ]]; then
  FILTER_SCREEN="home"
elif [[ "$NUMBER" -eq 2 ]]; then
  FILTER_SCREEN="poi"
elif [[ "$NUMBER" -eq 3 ]]; then
  FILTER_SCREEN="browse"
else
  echo "> Unexpected screen number '$NUMBER'!"
  exit 1 #error
fi

if [[ -z "${ANDROID_HOME}" ]]; then
  echo "> ANDROID_HOME not set '$ANDROID_HOME'!"
  exit 1 #error
fi
ADB="$ANDROID_HOME/platform-tools/adb"
if [[ ! -f "$ADB" ]]; then
  echo "> adb '$ADB' not fount!"
  exit 1 #error
fi

echo "> ADB devices: "
echo "----------"
$ADB devices -l
echo "----------"

MAIN_APP_INSTALLED=$($ADB shell pm list packages | grep -i "${MAIN_PKG}$")
if [[ "${MAIN_APP_INSTALLED}" != "package:$MAIN_PKG" ]]; then
  echo "> Main app not installed '$MAIN_PKG'!"
  if [ "$DEBUG" != true ]; then
    $ADB shell am start -a android.intent.action.VIEW -d "market://details?id=$MAIN_PKG"
  fi
  exit 1 #error
else
  echo "> Main app '$MAIN_PKG' installed."
fi

MODULE_APP_INSTALLED=$($ADB shell pm list packages | grep -i "${MODULE_PKG}$")
if [[ "${MODULE_APP_INSTALLED}" != "package:$MODULE_PKG" ]]; then
  echo "> Module app not installed '$MODULE_PKG'!"
  if [ "$DEBUG" != true ]; then
    $ADB shell am start -a android.intent.action.VIEW -d "market://details?id=$MODULE_PKG"
  fi
  exit 1 #error
else
  echo "> Module app '$MODULE_PKG' installed."
fi

echo "> Setting demo mode..."

# NOT needed anymore: time-zone SHOULD be overridden inside app.
# shellcheck disable=SC2034
if [[ "not-needed" == "yes" && -n "$AGENCY_TIME_ZONE" ]]; then
  # detects different time-zones && out-dated timezone rules #November2026
  echo " - agency time-zone: '$AGENCY_TIME_ZONE'"
  DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone | tr -d '\r')
  echo " - device time-zone: '$DEVICE_TIME_ZONE'"
  HOST_DATE_DEVICE_TZ=$(TZ=":$DEVICE_TIME_ZONE" date +%Y-%m-%d_%H:%M)
  HOST_DATE_AGENCY_TZ=$(TZ=":$AGENCY_TIME_ZONE" date +%Y-%m-%d_%H:%M)
  echo " - host date at '$AGENCY_TIME_ZONE': '$HOST_DATE_AGENCY_TZ'."
  echo " - host date at '$DEVICE_TIME_ZONE': '$HOST_DATE_DEVICE_TZ'."
  if [ "$HOST_DATE_AGENCY_TZ" != "$HOST_DATE_DEVICE_TZ" ]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
      $ADB shell settings set global auto_time 0 # turn-off automatic time
      $ADB shell setprop persist.sys.timezone "$AGENCY_TIME_ZONE"
      $ADB reboot
      $ADB wait-for-device
      DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone | tr -d '\r')
      HOST_DATE_DEVICE_TZ=$(TZ=":$DEVICE_TIME_ZONE" date +%Y-%m-%d_%H:%M)
      HOST_DATE_AGENCY_TZ=$(TZ=":$AGENCY_TIME_ZONE" date +%Y-%m-%d_%H:%M)
      echo " - host date at '$DEVICE_TIME_ZONE': '$HOST_DATE_DEVICE_TZ'."
      echo " - host date at '$AGENCY_TIME_ZONE': '$HOST_DATE_AGENCY_TZ'."
    fi
    if [ "$HOST_DATE_AGENCY_TZ" != "$HOST_DATE_DEVICE_TZ" ]; then
      echo "> Wrong time zone '$DEVICE_TIME_ZONE' ($HOST_DATE_DEVICE_TZ) for agency time zone '$AGENCY_TIME_ZONE' ($HOST_DATE_AGENCY_TZ)!"
      $ADB shell am start -a android.settings.DATE_SETTINGS
      exit 1 #error
    fi
  fi
fi

FORCE_TIME_FORMAT=""
if [[ "${LANG}" == "en-US" ]]; then
  FORCE_TIME_FORMAT="12"
elif [[ "${LANG}" == "fr-FR" ]]; then
  FORCE_TIME_FORMAT="24"
else
  echo ">> Force time format '$FORCE_TIME_FORMAT' for language '$LANG'."
fi

dateComponentsToMinutes() {
  if [[ ! "$1" =~ ^([0-9]{4})-([0-9]{2})-([0-9]{2})_([0-9]{2}):([0-9]{2})$ ]]; then
    echo "> Invalid date components '$1'!" >&2
    return 1
  fi
  local year=$((10#${BASH_REMATCH[1]}))
  local month=$((10#${BASH_REMATCH[2]}))
  local day=$((10#${BASH_REMATCH[3]}))
  local hour=$((10#${BASH_REMATCH[4]}))
  local minute=$((10#${BASH_REMATCH[5]}))
  # Count Gregorian days from March so leap days fall at the end of each year.
  if (( month <= 2 )); then
    year=$((year - 1))
    month=$((month + 9))
  else
    month=$((month - 3))
  fi
  local days=$((365 * year + year / 4 - year / 100 + year / 400 + (153 * month + 2) / 5 + day))
  echo "$((days * 1440 + hour * 60 + minute))"
}

echo "Agency time zone: '$AGENCY_TIME_ZONE'"
# detects different time-zones && out-dated timezone rules #November2026
if [[ -n "$AGENCY_TIME_ZONE" ]]; then
  DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone | tr -d '\r')
  echo "Device time zone: '$DEVICE_TIME_ZONE'"
  if [[ "$DEVICE_TIME_ZONE" != "$AGENCY_TIME_ZONE" ]]; then
    echo "> Device time zone '$DEVICE_TIME_ZONE' does not match expected agency time zone '$AGENCY_TIME_ZONE'!"
    exit 1 #error
  fi
  DEVICE_DATE_COMPONENTS=$($ADB shell date +%Y-%m-%d_%H:%M | tr -d '\r')
  EXPECTED_DATE_COMPONENTS=$(TZ="$AGENCY_TIME_ZONE" date +%Y-%m-%d_%H:%M)
  echo "Device date components: '$DEVICE_DATE_COMPONENTS'"
  echo "Expected date components: '$EXPECTED_DATE_COMPONENTS'"
  DEVICE_DATE_MINUTES=$(dateComponentsToMinutes "$DEVICE_DATE_COMPONENTS") || exit 1
  EXPECTED_DATE_MINUTES=$(dateComponentsToMinutes "$EXPECTED_DATE_COMPONENTS") || exit 1
  DATE_DIFFERENCE_MINUTES=$((DEVICE_DATE_MINUTES - EXPECTED_DATE_MINUTES))
  if (( DATE_DIFFERENCE_MINUTES < 0 )); then
    DATE_DIFFERENCE_MINUTES=$((-DATE_DIFFERENCE_MINUTES))
  fi
  MAX_DATE_DIFFERENCE_MINUTES=10
  echo "Device date difference: '$DATE_DIFFERENCE_MINUTES' minutes (maximum: '$MAX_DATE_DIFFERENCE_MINUTES')."
  if (( DATE_DIFFERENCE_MINUTES > MAX_DATE_DIFFERENCE_MINUTES )); then
    echo "> Current device date components '$DEVICE_DATE_COMPONENTS' differ from expected components '$EXPECTED_DATE_COMPONENTS' by more than $MAX_DATE_DIFFERENCE_MINUTES minutes for agency time zone '$AGENCY_TIME_ZONE'!"
    exit 1 #error
  fi
fi

# DISABLED: overriding current time inside main app doesn't load static/real-time schedule for overridden time
if [[ "disabled" == "yes" ]]; then
  if [[ -n "$AGENCY_TIME_ZONE" ]]; then
    DATE_TIME_IN_SEC=$(TZ="$AGENCY_TIME_ZONE" date --date='07:00 today' +%s)
  else
    DATE_TIME_IN_SEC=$(date --date='07:00 today' +%s)
  fi
  echo " > set app time to: $DATE_TIME_IN_SEC secs."
fi

# needed: time format is visible in the status bar.
TIME_FORMAT=$($ADB shell settings get system time_12_24)
echo "TIME_FORMAT:'$TIME_FORMAT'."
AVD_NAME=$(adb shell getprop ro.boot.qemu.avd_name || "")
echo "AVD_NAME:'$AVD_NAME'."
if [[ "needed" == "needed" && "${LANG}" == "en-US" ]]; then
  if [[ "${TIME_FORMAT}" != "12" && -n "$AVD_NAME" ]]; then
    $ADB -e shell settings put system time_12_24 12
    $ADB -e shell am force-stop com.android.settings
    $ADB -e shell am start -a android.settings.DATE_SETTINGS
    sleep 30 # sleep 30 seconds
    TIME_FORMAT=$($ADB shell settings get system time_12_24)
    echo "TIME_FORMAT:'$TIME_FORMAT'."
  fi
  if [[ "${TIME_FORMAT}" != "12" ]]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
      $ADB shell settings put system time_12_24 12
      $ADB reboot
      $ADB wait-for-device
      TIME_FORMAT=$($ADB shell settings get system time_12_24)
      echo "TIME_FORMAT:'$TIME_FORMAT'."
    fi
  fi
  if [[ "${TIME_FORMAT}" != "12" ]]; then
    echo "> Wrong time format '$TIME_FORMAT' for language '$LANG'!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1
  else
    echo "> Good time format '$TIME_FORMAT' for language '$LANG'."
  fi
elif [[ "needed" == "needed" && "${LANG}" == "fr-FR" ]]; then
  if [[ "${TIME_FORMAT}" != "24" && -n "$AVD_NAME" ]]; then
    $ADB -e shell settings put system time_12_24 24
    $ADB -e shell am force-stop com.android.settings
    $ADB -e shell am start -a android.settings.DATE_SETTINGS
    sleep 30 # sleep 30 seconds
    TIME_FORMAT=$($ADB shell settings get system time_12_24)
    echo "TIME_FORMAT:'$TIME_FORMAT'."
  fi
  if [[ "${TIME_FORMAT}" != "24" ]]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
      $ADB shell settings put system time_12_24 24
      $ADB reboot
      $ADB wait-for-device
      TIME_FORMAT=$($ADB shell settings get system time_12_24)
      echo "TIME_FORMAT:'$TIME_FORMAT'."
    fi
  fi
  if [[ "${TIME_FORMAT}" != "24" ]]; then
    echo ">> Wrong time format '$TIME_FORMAT' for language '$LANG'!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1
  else
    echo ">> Good time format '$TIME_FORMAT' for language '$LANG'."
  fi
else
  echo ">> Good time format '$TIME_FORMAT' for language '$LANG' (unexpected)."
fi

ORIGINAL_FONT_SCALE=$($ADB shell settings get system font_scale)
FONT_SCALE=$ORIGINAL_FONT_SCALE
if [[ "${FONT_SCALE}" != "1.0" ]]; then
  # try without rebooting 1st:
  $ADB shell settings put system font_scale 1.0
  FONT_SCALE=$($ADB shell settings get system font_scale)
fi
if [[ "${FONT_SCALE}" != "1.0" ]]; then
  if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
    $ADB shell settings put system font_scale 1.0
    $ADB reboot
    $ADB wait-for-device
    FONT_SCALE=$($ADB shell settings get system font_scale)
  fi
  if [[ "${FONT_SCALE}" != "1.0" ]]; then
    echo "> Wrong font scale '$FONT_SCALE'!"
    $ADB shell am start -a android.settings.DISPLAY_SETTINGS
    exit 1
  else
    echo "> Good font scale '$FONT_SCALE'."
  fi
else
  echo "> Good font scale '$FONT_SCALE'."
fi

# https://android.googlesource.com/platform/frameworks/base/+/main/packages/SystemUI/docs/demo_mode.md

DEMO_ALLOWED=$($ADB shell settings get global sysui_demo_allowed)
if [[ $DEMO_ALLOWED -ne 1 ]]; then
  echo ">> demo was NOT already allowed ($DEMO_ALLOWED)."
  $ADB shell settings put global sysui_demo_allowed 1
else
  echo ">> demo was already allowed ($DEMO_ALLOWED)."
fi
$ADB shell am broadcast -a com.android.systemui.demo --es command enter
$ADB shell am broadcast -a com.android.systemui.demo --es command battery \
  --es level 100 \
  --es plugged false  \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command network \
  --es mobile hide \
  --es datatype 5g \
  --es level 4 \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command network \
  --es wifi show \
  --es fully true \
  --es level 4 \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command notifications \
  --es visible false \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command status \
  --es location show \
  ;

# IF changing app time THEN change visible time in status bar
if [[ "disabled" == "yes" && -n "$DATE_TIME_IN_SEC" ]]; then
  DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone | tr -d '\r')
  HOST_DATE_DEVICE_TZ=$(TZ=":$DEVICE_TIME_ZONE" date)
  HOST_DATE_DEVICE_TZ_IN_SEC=$(TZ="$DEVICE_TIME_ZONE" date --date='07:00 today' +%s)
  HOST_DATE_DEVICE_TZ_IN_MS="${HOST_DATE_DEVICE_TZ_IN_SEC}000";
  echo "> Device date time (status bar): $HOST_DATE_DEVICE_TZ_IN_SEC secs."
  $ADB shell am broadcast -a com.android.systemui.demo --es command clock \
    --es millis ${HOST_DATE_DEVICE_TZ_IN_MS} \
    ;
fi
echo "> Setting demo mode... DONE"

echo "> Stop app..."
$ADB shell am force-stop $MAIN_PKG
echo "> Stop app... DONE"

echo "> Starting app..."
$ADB shell am start -n $MAIN_PKG/$SPLASH_SCREEN_ACTIVITY \
  --es "filter_agency_authority" "$FILTER_AGENCY_AUTHORITY" \
  --es "filter_screen" "$FILTER_SCREEN" \
  --es "force_lang" "$LANG" \
  --es "force_timestamp_sec" "$DATE_TIME_IN_SEC" \
  --es "force_tz" "$AGENCY_TIME_ZONE" \
  --es "force_time" "$FORCE_TIME_FORMAT" \
  ;
echo "> Starting app... DONE"

SLEEP_IN_SEC=10
if [ "$IS_CI" = true ]; then
  SLEEP_IN_SEC=30
fi
echo "> Waiting for UI ($SLEEP_IN_SEC seconds)..."
sleep $SLEEP_IN_SEC # wait for UI to be ready
echo "> Waiting for UI ($SLEEP_IN_SEC seconds)... DONE"

echo "> Capturing screen shot..."
FILE_NAME="$NUMBER.png"
DEVICE_PATH="/sdcard/$FILE_NAME"
DEST_PATH="$DEST_DIR/$FILE_NAME"
echo " - destination: '$DEST_PATH'"
$ADB shell screencap -p "$DEVICE_PATH"
$ADB pull "$DEVICE_PATH" "$DEST_PATH"
$ADB shell rm "$DEVICE_PATH"
echo "> Capturing screen shot... DONE"

echo "> Resetting demo mode..."
$ADB shell am broadcast -a com.android.systemui.demo --es command exit
$ADB shell settings put system font_scale $ORIGINAL_FONT_SCALE
$ADB shell settings put global sysui_demo_allowed "$DEMO_ALLOWED"
echo "> Resetting demo mode... DONE"

echo "> Stop app..."
$ADB shell am force-stop $MAIN_PKG # app is in invalid state, stop to remove all from memory
echo "> Stop app... DONE"

echo ">> Capturing Module App Screenshot '$*'... DONE"
