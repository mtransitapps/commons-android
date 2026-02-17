#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR="$SCRIPT_DIR/../..";
source ${ROOT_DIR}/commons/commons.sh;
echo ">> Capturing Module App Screenshot '$*'..."

if [[ "$#" -ne 3 ]]; then
  echo "> Wrong $# parameters '$*'!"
  echo "- Ex: 'app-android: ../commons-android/pub/module-app-screenshot.sh en-US phone 1'"
  echo "- Ex: 'app-android: ../commons-android/pub/module-app-screenshot.sh fr-FR phone 2'"
  exit 1 #error
fi

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

PROJECT_DIR="$SCRIPT_DIR/../.."; # pub -> commons-android
APP_ANDROID_DIR="$PROJECT_DIR/app-android"
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
CONFIG_DIR=$PROJECT_DIR/config

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
DEVICE_AUTO_TIME=$($ADB shell settings get global auto_time)
DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone)
if [[ "not-needed" == "yes" && -n "$AGENCY_TIME_ZONE" ]]; then
  echo " - agency time-zone: '$AGENCY_TIME_ZONE'"
  DEVICE_DATE_TIME=$(TZ=":$DEVICE_TIME_ZONE" date)
  AGENCY_DATE_TIME=$(TZ=":$AGENCY_TIME_ZONE" date)
  if [ "$AGENCY_DATE_TIME" != "$DEVICE_DATE_TIME" ]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
      $ADB shell settings set global auto_time 0 # turn-off automatic time
      $ADB shell setprop persist.sys.timezone "$AGENCY_TIME_ZONE"
      $ADB reboot
      $ADB wait-for-device
      DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone)
      DEVICE_DATE_TIME=$(date --date="TZ=$DEVICE_TIME_ZONE")
    fi
    if [ "$AGENCY_DATE_TIME" != "$DEVICE_DATE_TIME" ]; then
      echo "> Wrong time zone '$DEVICE_TIME_ZONE' ($DEVICE_DATE_TIME) for agency time zone '$AGENCY_TIME_ZONE' ($AGENCY_DATE_TIME)!"
      $ADB shell am start -a android.settings.DATE_SETTINGS
      exit 1 #error
    fi
  fi
fi

TIME_FORMAT=$($ADB shell settings get system time_12_24)
FORCE_TIME_FORMAT=""
if [[ "${LANG}" == "en-US" ]]; then
  FORCE_TIME_FORMAT="12"
elif [[ "${LANG}" == "fr-FR" ]]; then
  FORCE_TIME_FORMAT="24"
else
  echo ">> Good time format '$TIME_FORMAT' for language '$LANG'."
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

# NOT needed anymore: time format SHOULD be overridden inside app.
TIME_FORMAT=$($ADB shell settings get system time_12_24)
if [[ "not-needed" == "yes" && "${LANG}" == "en-US" && "${TIME_FORMAT}" != "12" ]]; then
  if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
    $ADB shell settings put system time_12_24 12
    $ADB reboot
    $ADB wait-for-device
    TIME_FORMAT=$($ADB shell settings get system time_12_24)
  fi
  if [[ "${LANG}" == "en-US" && "${TIME_FORMAT}" != "12" ]]; then
    echo "> Wrong time format '$TIME_FORMAT' for language '$LANG'!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1
  else
    echo "> Good time format '$TIME_FORMAT' for language '$LANG'."
  fi
elif [[ "not-needed" == "yes" && "${LANG}" == "fr-FR" && "${TIME_FORMAT}" != "24" ]]; then
  if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
    $ADB shell settings put system time_12_24 12
    $ADB reboot
    $ADB wait-for-device
    TIME_FORMAT=$($ADB shell settings get system time_12_24)
  fi
  if [[ "${LANG}" == "fr-FR" && "${TIME_FORMAT}" != "24" ]]; then
    echo ">> Wrong time format '$TIME_FORMAT' for language '$LANG'!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1
  else
    echo ">> Good time format '$TIME_FORMAT' for language '$LANG'."
  fi
else
  echo ">> Good time format '$TIME_FORMAT' for language '$LANG'."
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
  --es wifi show \
  --es fully true \
  --es level 4 \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command network \
  --es mobile show \
  --es datatype none \
  --es level 4 \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command notifications \
  --es visible false \
  ;
$ADB shell am broadcast -a com.android.systemui.demo --es command status \
  --es location show \
  ;

# IF changing app time THEN change visible time in status bar
if [[ -n "$DATE_TIME_IN_SEC" ]]; then
  DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone)
  DEVICE_DATE_TIME=$(TZ=":$DEVICE_TIME_ZONE" date)
  DEVICE_DATE_TIME_IN_SEC=$(TZ="$DEVICE_TIME_ZONE" date --date='07:00 today' +%s)
  DEVICE_DATE_TIME_IN_MS="${DEVICE_DATE_TIME_IN_SEC}000";
  echo "> Device date time (status bar): $DEVICE_DATE_TIME_IN_SEC secs."
  $ADB shell am broadcast -a com.android.systemui.demo --es command clock \
    --es millis ${DEVICE_DATE_TIME_IN_MS} \
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
