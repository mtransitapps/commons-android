#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR="$SCRIPT_DIR/../.."
source "${ROOT_DIR}/commons/commons.sh"
echo ">> Capturing Main App Screenshot '$*'..."

# WIP: just opens the app and waits fox X seconds to take screenshot in demo mode.
# en-US: in Toronto area
# fr-FR: in Montréal area
# fr-CA: in Montréal area

if [[ "$#" -ne 3 ]]; then
  echo "> Wrong $# parameters '$*'!"
  echo "- Ex: 'app-android: ../commons-android/pub/main-app-screenshot.sh en-US phone 1'"
  echo "- Ex: 'app-android: ../commons-android/pub/main-app-screenshot.sh fr-FR phone 2'"
  echo "- Ex: 'app-android: ../commons-android/pub/main-app-screenshot.sh fr-CA phone 3'"
  exit 1 #error
fi

# Canada only for now: en-US = en-CA, fr-FR = fr-CA

TIMEZONE_EN="America/Toronto"
GPS_LAT_EN=43.6638328 # Toronto (UofT)
GPS_LNG_EN=-79.3953405 # Toronto (UofT)
MODULES_PKG_EN=(
  "org.mtransit.android.ca_gtha_go_transit_bus:https://github.com/mtransitapps/ca-gtha-go-transit-bus-android"
  "org.mtransit.android.ca_gtha_go_transit_train:https://github.com/mtransitapps/ca-gtha-go-transit-train-android"
  "org.mtransit.android.ca_toronto_share_bike:https://github.com/mtransitapps/ca-toronto-share-bike-android"
  "org.mtransit.android.ca_toronto_ttc_bus:https://github.com/mtransitapps/ca-toronto-ttc-bus-android"
  "org.mtransit.android.ca_toronto_ttc_light_rail:https://github.com/mtransitapps/ca-toronto-ttc-light-rail-android"
  "org.mtransit.android.ca_toronto_ttc_subway:https://github.com/mtransitapps/ca-toronto-ttc-subway-android"
  "org.mtransit.android.ca_gta_up_express_train:https://github.com/mtransitapps/ca-gta-up-express-train-android"
  "org.mtransit.android.ca_via_rail_train:https://github.com/mtransitapps/ca-via-rail-train-android"
  "org.mtransit.android.ca_york_region_yrt_viva_bus:https://github.com/mtransitapps/ca-york-region-yrt-viva-bus-android"
)

TIMEZONE_FR="America/Montreal"
GPS_LAT_FR=45.5230433 # Montréal office
GPS_LNG_FR=-73.5814131 # Montréal office
MODULES_PKG_FR=(
  "org.mtransit.android.ca_montreal_amt_train:https://github.com/mtransitapps/ca-montreal-amt-train-android"
  "org.mtransit.android.ca_montreal_stm_bus:https://github.com/mtransitapps/ca-montreal-stm-bus-android"
  "org.mtransit.android.ca_montreal_stm_subway:https://github.com/mtransitapps/ca-montreal-stm-subway-android"
  "org.mtransit.android.ca_montreal_bixi_bike:https://github.com/mtransitapps/ca-montreal-bixi-bike-android"
  "org.mtransit.android.ca_montreal_rem_light_rail:https://github.com/mtransitapps/ca-montreal-rem-light-rail-android"
  "org.mtransit.android.ca_longueuil_rtl_bus:https://github.com/mtransitapps/ca-longueuil-rtl-bus-android"
  "org.mtransit.android.ca_laval_stl_bus:https://github.com/mtransitapps/ca-laval-stl-bus-android"
)

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

if [[ "${LANG}" != "en-US" && "${LANG}" != "fr-FR" && "${LANG}" != "fr-CA" ]]; then
  echo "> Invalid lang provided '$LANG'!"
  exit 1 #error
fi

if [[ "${TYPE}" != "phone" ]]; then
  echo "> Invalid type provided '$TYPE'!"
  exit 1 #error
fi

PROJECT_DIR="$SCRIPT_DIR/../.."; # pub -> commons-android
APP_ANDROID_DIR="$PROJECT_DIR/app-android"
DEST_DIR="$APP_ANDROID_DIR/src/main/play/listings/$LANG/graphics/$TYPE-screenshots"

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


# requireCommand "xmllint" "libxml2-utils";
#
# GTFS_RDS_VALUES_GEN_FILE="$RES_DIR/values/gtfs_rts_values_gen.xml" # do not change to avoid breaking compat w/ old modules
# BIKE_STATION_VALUES_FILE="$RES_DIR/values/bike_station_values.xml"
# AGENCY_TIME_ZONE=""
# if [ -f $GTFS_RDS_VALUES_GEN_FILE ]; then
#   echo " - using agency file: '$GTFS_RDS_VALUES_GEN_FILE'."
#   AGENCY_TIME_ZONE=$(xmllint --xpath "//resources/string[@name='gtfs_rts_timezone']/text()" "$GTFS_RDS_VALUES_GEN_FILE")
#   FILTER_AGENCY_AUTHORITY="$PKG.gtfs"
# elif [ -f $BIKE_STATION_VALUES_FILE ]; then
#   echo " - using agency file: '$BIKE_STATION_VALUES_FILE'."
#   AGENCY_TIME_ZONE="" # does NOT matter for bike
#   FILTER_AGENCY_AUTHORITY="$PKG.bike"
# else
#   echo "> No agency file! (rds:$GTFS_RDS_VALUES_GEN_FILE|bike:$BIKE_STATION_VALUES_FILE)"
#   exit 1 #error
# fi
# if [[ -z "${FILTER_AGENCY_AUTHORITY}" ]]; then
#   echo "> No agency authority found '$FILTER_AGENCY_AUTHORITY'!"
#   exit 1 #error
# fi
# 
# echo " - agency authority: '$FILTER_AGENCY_AUTHORITY'"

if ! [[ "$NUMBER" =~ ^[1-8]$ ]]; then
  echo "> Invalid screenshot number '$NUMBER'!"
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

EXPECTED_MODULE_ENTRIES=()
if [[ "${LANG}" == "en-US" ]]; then
  EXPECTED_MODULE_ENTRIES=("${MODULES_PKG_EN[@]}")
elif [[ "${LANG}" == "fr-FR" || "${LANG}" == "fr-CA" ]]; then
  EXPECTED_MODULE_ENTRIES=("${MODULES_PKG_FR[@]}")
else
  echo "> No expected module list for language '$LANG'!"
  exit 1 #error
fi

EXPECTED_MODULES=()
declare -A MODULE_REPO_BY_PKG=()
for MODULE_ENTRY in "${EXPECTED_MODULE_ENTRIES[@]}"; do
  if [[ "$MODULE_ENTRY" != *:* ]]; then
    echo "> Invalid module entry '$MODULE_ENTRY' (expected 'pkg:url_repo')."
    exit 1 #error
  fi
  MODULE_PKG="${MODULE_ENTRY%%:*}"
  MODULE_REPO_URL="${MODULE_ENTRY#*:}"
  if [[ -z "$MODULE_PKG" || -z "$MODULE_REPO_URL" ]]; then
    echo "> Invalid module entry '$MODULE_ENTRY' (empty pkg/url_repo)."
    exit 1 #error
  fi
  EXPECTED_MODULES+=("$MODULE_PKG")
  MODULE_REPO_BY_PKG["$MODULE_PKG"]="$MODULE_REPO_URL"
done

EXPECTED_PACKAGES=("$MAIN_PKG" "${EXPECTED_MODULES[@]}")
mapfile -t INSTALLED_MT_PACKAGES < <(
  $ADB shell pm list packages \
    | sed -n 's/^package://p' \
    | tr -d '\r' \
    | grep '^org\.mtransit\.android' \
    | grep -v '\.debug'
)

declare -A EXPECTED_SET=()
declare -A INSTALLED_SET=()
for PKG in "${EXPECTED_PACKAGES[@]}"; do
  EXPECTED_SET["$PKG"]=1
done
for PKG in "${INSTALLED_MT_PACKAGES[@]}"; do
  INSTALLED_SET["$PKG"]=1
done

MISSING_PACKAGES=()
EXTRA_PACKAGES=()
for PKG in "${!EXPECTED_SET[@]}"; do
  if [[ -z "${INSTALLED_SET[$PKG]}" ]]; then
    MISSING_PACKAGES+=("$PKG")
  fi
done
for PKG in "${!INSTALLED_SET[@]}"; do
  if [[ -z "${EXPECTED_SET[$PKG]}" ]]; then
    EXTRA_PACKAGES+=("$PKG")
  fi
done

if [[ "${#MISSING_PACKAGES[@]}" -gt 0 || "${#EXTRA_PACKAGES[@]}" -gt 0 ]]; then
  echo "> Installed org.mtransit.android package set is invalid for '$LANG'."
  echo "> Expected packages ($((${#EXPECTED_PACKAGES[@]}))):"
  printf '%s\n' "${EXPECTED_PACKAGES[@]}" | sort
  echo "> Installed packages ($((${#INSTALLED_MT_PACKAGES[@]}))):"
  printf '%s\n' "${INSTALLED_MT_PACKAGES[@]}" | sort

  REMEDIATION_ATTEMPTED=false

  if [[ "${#MISSING_PACKAGES[@]}" -gt 0 ]]; then
    echo "> Missing packages found, attempting download+install..."
    printf '%s\n' "${MISSING_PACKAGES[@]}" | sort
    echo "> Missing module repositories:"
    for PKG in "${MISSING_PACKAGES[@]}"; do
      if [[ -n "${MODULE_REPO_BY_PKG[$PKG]}" ]]; then
        echo "$PKG:${MODULE_REPO_BY_PKG[$PKG]}"
      fi
    done | sort

    for PKG in "${MISSING_PACKAGES[@]}"; do
      MODULE_REPO_URL="${MODULE_REPO_BY_PKG[$PKG]}"
      if [[ -z "$MODULE_REPO_URL" ]]; then
        echo " - no repository mapping for missing package '$PKG' (skip auto-install)."
        continue
      fi

      echo " - downloading latest APK for '$PKG' from '$MODULE_REPO_URL'..."
      if ! "$ROOT_DIR/download_latest_apk.sh" "$MODULE_REPO_URL"; then
        echo " - failed to download latest APK for '$PKG'."
        continue
      fi

      if ! APK_FILE_LIST=$(gh release view -R "$MODULE_REPO_URL" --json assets --jq '.assets[] | select(.name | endswith(".apk")) | .name'); then
        echo " - failed to fetch release APK list for '$PKG'."
        continue
      fi

      read -r MODULE_APK_FILE <<< "$APK_FILE_LIST"
      if [[ -z "$MODULE_APK_FILE" || ! -s "$MODULE_APK_FILE" ]]; then
        echo " - downloaded APK file missing or empty for '$PKG' ('$MODULE_APK_FILE')."
        continue
      fi

      echo " - installing '$PKG' from '$MODULE_APK_FILE'..."
      if ! $ADB install -r -d "$MODULE_APK_FILE"; then
        echo " - failed to install '$PKG'."
        continue
      fi
      REMEDIATION_ATTEMPTED=true
    done
  fi

  if [[ "${#EXTRA_PACKAGES[@]}" -gt 0 ]]; then
    echo "> Unexpected extra packages found, uninstalling and re-validating..."
    printf '%s\n' "${EXTRA_PACKAGES[@]}" | sort
    for PKG in "${EXTRA_PACKAGES[@]}"; do
      echo " - uninstalling '$PKG'..."
      $ADB uninstall "$PKG" || true
    done
    REMEDIATION_ATTEMPTED=true
  fi

  if [[ "$REMEDIATION_ATTEMPTED" == true ]]; then
    mapfile -t INSTALLED_MT_PACKAGES < <(
      $ADB shell pm list packages \
        | sed -n 's/^package://p' \
        | tr -d '\r' \
        | grep '^org\.mtransit\.android' \
        | grep -v '\.debug'
    )

    declare -A INSTALLED_SET=()
    for PKG in "${INSTALLED_MT_PACKAGES[@]}"; do
      INSTALLED_SET["$PKG"]=1
    done

    MISSING_PACKAGES=()
    EXTRA_PACKAGES=()
    for PKG in "${!EXPECTED_SET[@]}"; do
      if [[ -z "${INSTALLED_SET[$PKG]}" ]]; then
        MISSING_PACKAGES+=("$PKG")
      fi
    done
    for PKG in "${!INSTALLED_SET[@]}"; do
      if [[ -z "${EXPECTED_SET[$PKG]}" ]]; then
        EXTRA_PACKAGES+=("$PKG")
      fi
    done

    if [[ "${#MISSING_PACKAGES[@]}" -eq 0 && "${#EXTRA_PACKAGES[@]}" -eq 0 ]]; then
      echo "> Package set fixed after remediation."
    fi
  fi

  if [[ "${#MISSING_PACKAGES[@]}" -gt 0 || "${#EXTRA_PACKAGES[@]}" -gt 0 ]]; then
    echo "> Installed org.mtransit.android package set is still invalid for '$LANG'."
    echo "> Expected packages ($((${#EXPECTED_PACKAGES[@]}))):"
    printf '%s\n' "${EXPECTED_PACKAGES[@]}" | sort
    echo "> Installed packages ($((${#INSTALLED_MT_PACKAGES[@]}))):"
    printf '%s\n' "${INSTALLED_MT_PACKAGES[@]}" | sort
    if [[ "${#MISSING_PACKAGES[@]}" -gt 0 ]]; then
      echo "> Missing packages:"
      printf '%s\n' "${MISSING_PACKAGES[@]}" | sort
      echo "> Missing module repositories:"
      for PKG in "${MISSING_PACKAGES[@]}"; do
        if [[ -n "${MODULE_REPO_BY_PKG[$PKG]}" ]]; then
          echo "$PKG:${MODULE_REPO_BY_PKG[$PKG]}"
        fi
      done | sort
    fi
    if [[ "${#EXTRA_PACKAGES[@]}" -gt 0 ]]; then
      echo "> Unexpected extra packages:"
      printf '%s\n' "${EXTRA_PACKAGES[@]}" | sort
    fi
    exit 1 #error
  fi
fi

echo "> Installed org.mtransit.android package set is valid for '$LANG'."

echo "> Setting demo mode..."
# shellcheck disable=SC2034
DEVICE_AUTO_TIME=$($ADB shell settings get global auto_time)
DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone)
if [[ -n "$AGENCY_TIME_ZONE" ]]; then
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

AVD_NAME=$(adb shell getprop ro.boot.qemu.avd_name || "")

# change device main language and locale based on the selected language
CURRENT_LANG=$($ADB shell getprop persist.sys.locale || "")
if [[ "$CURRENT_LANG" != "$LANG" ]]; then
  if [[ "working" == "not-realy" && -n "$AVD_NAME" ]]; then
    echo "> Changing emulator device language from '$CURRENT_LANG' to '$LANG'..."
    $ADB root; # needed to change language
    $ADB shell "setprop persist.sys.locale $LANG; setprop ctl.restart zygote"
    sleep 5 # wait for the device to apply the new locale
    CURRENT_LANG=$($ADB shell getprop persist.sys.locale || "")
  fi
  echo "> Device language is now '$CURRENT_LANG'."
  if [[ "$CURRENT_LANG" != "$LANG" ]]; then
    echo "> Failed to change device language to '$LANG'."
    $ADB shell am start -a android.settings.LOCALE_SETTINGS
    exit 1 #error
  fi
fi

FORCE_TIME_FORMAT=""
if [[ "${LANG}" == "en-US" ]]; then
  FORCE_TIME_FORMAT="12"
elif [[ "${LANG}" == "fr-FR" || "${LANG}" == "fr-CA" ]]; then
  FORCE_TIME_FORMAT="24"
else
  echo ">> Force time format '$FORCE_TIME_FORMAT' for language '$LANG'."
fi

TIME_FORMAT=$($ADB shell settings get system time_12_24)
echo "TIME_FORMAT: '$TIME_FORMAT'."
echo "AVD_NAME: '$AVD_NAME'."
if [[ "${LANG}" == "en-US" ]]; then
  if [[ "${TIME_FORMAT}" != "${FORCE_TIME_FORMAT}" && -n "$AVD_NAME" ]]; then
    $ADB -e shell settings put system time_12_24 "$FORCE_TIME_FORMAT"
    $ADB -e shell am force-stop com.android.settings
    $ADB -e shell am start -a android.settings.DATE_SETTINGS
    sleep 30 # sleep 30 seconds
    TIME_FORMAT=$($ADB shell settings get system time_12_24)
    echo "TIME_FORMAT:'$TIME_FORMAT'."
  fi
  if [[ "${TIME_FORMAT}" != "${FORCE_TIME_FORMAT}" ]]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
      $ADB shell settings put system time_12_24 "$FORCE_TIME_FORMAT"
      $ADB reboot
      $ADB wait-for-device
      TIME_FORMAT=$($ADB shell settings get system time_12_24)
      echo "TIME_FORMAT:'$TIME_FORMAT'."
    fi
  fi
  if [[ "${TIME_FORMAT}" != "${FORCE_TIME_FORMAT}" ]]; then
    echo "> Wrong time format '$TIME_FORMAT' for language '$LANG'!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1
  else
    echo "> Good time format '$TIME_FORMAT' for language '$LANG'."
  fi
elif [[ "${LANG}" == "fr-FR" || "${LANG}" == "fr-CA" ]]; then
  if [[ "${TIME_FORMAT}" != "${FORCE_TIME_FORMAT}" && -n "$AVD_NAME" ]]; then
    $ADB -e shell settings put system time_12_24 "$FORCE_TIME_FORMAT"
    $ADB -e shell am force-stop com.android.settings
    $ADB -e shell am start -a android.settings.DATE_SETTINGS
    sleep 30 # sleep 30 seconds
    TIME_FORMAT=$($ADB shell settings get system time_12_24)
    echo "TIME_FORMAT:'$TIME_FORMAT'."
  fi
  if [[ "${TIME_FORMAT}" != "${FORCE_TIME_FORMAT}" ]]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
      $ADB shell settings put system time_12_24 "$FORCE_TIME_FORMAT"
      $ADB reboot
      $ADB wait-for-device
      TIME_FORMAT=$($ADB shell settings get system time_12_24)
      echo "TIME_FORMAT:'$TIME_FORMAT'."
    fi
  fi
  if [[ "${TIME_FORMAT}" != "${FORCE_TIME_FORMAT}" ]]; then
    echo ">> Wrong time format '$TIME_FORMAT' for language '$LANG'!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1
  else
    echo ">> Good time format '$TIME_FORMAT' for language '$LANG'."
  fi
else
  echo ">> Good time format '$TIME_FORMAT' for language '$LANG' (unexpected)."
fi
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
echo "> Setting demo mode... DONE"

echo "> Grant location permission to main app..."
$ADB shell pm grant "$MAIN_PKG" android.permission.ACCESS_FINE_LOCATION
$ADB shell pm grant "$MAIN_PKG" android.permission.ACCESS_COARSE_LOCATION
echo "> Grant location permission to main app... DONE"

GPS_LAT=0.0;
GPS_LNG=0.0;
if [[ "${LANG}" == "en-US" ]]; then
  GPS_LAT=$GPS_LAT_EN
  GPS_LNG=$GPS_LNG_EN
elif [[ "${LANG}" == "fr-FR" || "${LANG}" == "fr-CA" ]]; then
  GPS_LAT=$GPS_LAT_FR
  GPS_LNG=$GPS_LNG_FR
else
  echo "> No GPS Lat/Long for '$LANG'!"
  exit 1 #error
fi
echo "> Setting GPS to ${GPS_LAT}, ${GPS_LNG}..."
# $ADB shell am start -a android.settings.LOCATION_SOURCE_SETTINGS
$ADB emu geo fix "$GPS_LNG" "$GPS_LAT"
echo "> Setting GPS to ${GPS_LAT}, ${GPS_LNG}... DONE"

TIMEZONE="";
if [[ "${LANG}" == "en-US" ]]; then
  TIMEZONE=$TIMEZONE_EN
elif [[ "${LANG}" == "fr-FR" || "${LANG}" == "fr-CA" ]]; then
  TIMEZONE=$TIMEZONE_FR
else
  echo "> No timezone for '$LANG'!"
  exit 1 #error
fi
echo "> Setting Timezone to '${TIMEZONE}'..."
DEVICE_AUTO_TIME=$($ADB shell settings get global auto_time)
echo ">> Device auto time: '$DEVICE_AUTO_TIME'."
DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone)
echo ">> Device time zone: '$DEVICE_TIME_ZONE'."
DEVICE_DATE_TIME=$(TZ=":$DEVICE_TIME_ZONE" date)
echo ">> Device date time: '$DEVICE_DATE_TIME'."
AGENCY_DATE_TIME=$(TZ=":$TIMEZONE" date)
echo ">> Agency date time: '$AGENCY_DATE_TIME'."
if [ "$AGENCY_DATE_TIME" != "$DEVICE_DATE_TIME" ]; then
  if [ "$DEVICE_REBOOT_ALLOWED" = true ]; then
    $ADB shell settings set global auto_time 0 # turn-off automatic time
    $ADB shell setprop persist.sys.timezone "$TIMEZONE"
    $ADB reboot
    $ADB wait-for-device
    DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone)
    DEVICE_DATE_TIME=$(date --date="TZ=$DEVICE_TIME_ZONE")
  fi
  if [ "$AGENCY_DATE_TIME" != "$DEVICE_DATE_TIME" ]; then
    echo "> Wrong time zone '$DEVICE_TIME_ZONE' ($DEVICE_DATE_TIME) for agency time zone '$TIMEZONE' ($AGENCY_DATE_TIME)!"
    $ADB shell am start -a android.settings.DATE_SETTINGS
    exit 1 #error
  fi
fi
echo "> Setting Timezone to '${TIMEZONE}'... DONE"

echo "> Stop app..."
$ADB shell am force-stop $MAIN_PKG
echo "> Stop app... DONE"

if [[ "not-needed" == "yes" ]]; then
  echo "> Launch main app and wait for initialization..."
  # Launch the main app once to let it initialize (download data, etc.)
  $ADB shell monkey -p "$MAIN_PKG" -c android.intent.category.LAUNCHER 1
  INIT_DURATION_IN_SEC=30
  echo "> Launch main app and wait for initialization, waiting $INIT_DURATION_IN_SEC seconds for initialization..."
  sleep $INIT_DURATION_IN_SEC
  echo "> Launch main app and wait for initialization... DONE"
  echo "> Stop app..."
  $ADB shell am force-stop $MAIN_PKG
  echo "> Stop app... DONE"
fi

echo "> Starting app..."
$ADB shell am start -n $MAIN_PKG/$SPLASH_SCREEN_ACTIVITY \
  --es "force_lang" "$LANG" \
  --es "force_tz" "$TIMEZONE" \
  --es "force_time" "$FORCE_TIME_FORMAT" \
  ;
# --es "filter_screen" "$FILTER_SCREEN" \
# --es "filter_agency_authority" "$FILTER_AGENCY_AUTHORITY" \
echo "> Starting app... DONE"

SLEEP_IN_SEC=30
echo "> Navigate to screen to get ready for screenshot in $SLEEP_IN_SEC seconds..."
SLEEP_STEP_IN_SEC=5
ELAPSED_IN_SEC=0
while [[ "$ELAPSED_IN_SEC" -lt "$SLEEP_IN_SEC" ]]; do
  sleep "$SLEEP_STEP_IN_SEC" # wait for UI to be ready
  ELAPSED_IN_SEC=$((ELAPSED_IN_SEC + SLEEP_STEP_IN_SEC))
  echo "> Waiting for screenshot readiness... ${ELAPSED_IN_SEC}/${SLEEP_IN_SEC}s"
done
echo "> Capturing screenshot..."
FILE_NAME="$NUMBER.png"
NOW=$(date +%Y-%m-%d_%H-%M-%S);
# FILE_NAME="$NOW.png" # DEBUG
DEVICE_PATH="/sdcard/$FILE_NAME"
DEST_PATH="$DEST_DIR/$FILE_NAME"
echo " - destination: '$DEST_PATH'"
$ADB shell screencap -p "$DEVICE_PATH"
$ADB pull "$DEVICE_PATH" "$DEST_PATH"
$ADB shell rm "$DEVICE_PATH"
echo "> Capturing screenshot... DONE"

echo "> Resetting demo mode..."
$ADB shell am broadcast -a com.android.systemui.demo -e command exit
$ADB shell settings put global sysui_demo_allowed "$DEMO_ALLOWED"
echo "> Resetting demo mode... DONE"

echo "> Stop app..."
$ADB shell am force-stop $MAIN_PKG # app is in invalid state, stop to remove all from memory
echo "> Stop app... DONE"

echo ">> Capturing Module App Screenshot '$*'... DONE"
