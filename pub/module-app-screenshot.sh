#!/bin/bash
echo ">> Capturing Module App Screenshot '$@'...";

if [[ "$#" -ne 3 ]]; then
    echo "> Wrong $# parameters '$@'!";
    echo "- Ex: 'app-android: ../commons-android/pub/module-app-screenshot.sh en phone 1'";
    exit 1;
fi

LANG=$1;
TYPE=$2;
NUMBER=$3;

DEBUG=false;
# DEBUG=true; # DEBUG

DEVICE_REBOOT_ALLOWED=false;
# DEVICE_REBOOT_ALLOWED=true; # use to switch time format (12/24), time-zone...

if [[ -z "${LANG}" ]]; then
    echo "> No lang provided '$LANG'!";
    exit 1;
fi
if [[ -z "${TYPE}" ]]; then
    echo "> No device type provided '$TYPE'!";
    exit 1;
fi
if [[ -z "${NUMBER}" ]]; then
    echo "> No screenshot number provided '$NUMBER'!";
    exit 1;
fi
echo " - lang: '$LANG'";
echo " - type: '$TYPE'";
echo " - number: '$NUMBER'";

if [[ "${LANG}" != "en-US" && "${LANG}" != "fr-FR" ]]; then
    echo "> Invalid lang provided '$LANG'!";
    exit 1;
fi

if [[ "${TYPE}" != "phone" ]]; then
    echo "> Invalid type provided '$TYPE'!";
    exit 1;
fi

DEST_DIR="src/main/play/listings/$LANG/graphics/$TYPE-screenshots";

if [ ! -d $DEST_DIR ]; then
    echo "> Destination directory does NOT exist '$DEST_DIR'!";
    exit 1;
fi
DEST_PATH="$DEST_DIR/$FILE_NAME";
echo " - destination: '$DEST_PATH'";

MAIN_PKG="org.mtransit.android";
if [ "$DEBUG" = true ] ; then
    MAIN_PKG="$MAIN_PKG.debug"; #DEBUG
fi
SPLASH_SCREEN_ACTIVITY="org.mtransit.android.ui.SplashScreenActivity";

RES_DIR=src/main/res;
DEBUG_RES_DIR=src/debug/res;
AGENCY_RTS_FILE=$RES_DIR/values/gtfs_rts_values_gen.xml;
AGENCY_BIKE_FILE=$RES_DIR/values/bike_station_values.xml;
AGENCY_TIME_ZONE="";
if [ -f $AGENCY_RTS_FILE ]; then
    echo " - agency file: '$AGENCY_BIKE_FILE'.";
    AGENCY_TIME_ZONE=$(grep -E "<string name=\"gtfs_rts_timezone\">(.*)+</string>$" $AGENCY_RTS_FILE | cut -d ">" -f2 | cut -d "<" -f1);
    AGENCY_RTS_FILE=$RES_DIR/values/gtfs_rts_values.xml;
    if [ "$DEBUG" = true ] ; then
        AGENCY_RTS_FILE=$DEBUG_RES_DIR/values/gtfs_rts_values.xml;
    fi
    FILTER_AGENCY_AUTHORIY=$(grep -E "<string name=\"gtfs_rts_authority\">(.*)+</string>$" $AGENCY_RTS_FILE | cut -d ">" -f2 | cut -d "<" -f1);
elif [ -f $AGENCY_BIKE_FILE ]; then
    echo " - agency file: '$AGENCY_BIKE_FILE'.";
    AGENCY_TIME_ZONE=""; # does NOT matter for bike
    if [ "$DEBUG" = true ] ; then
        AGENCY_BIKE_FILE=$DEBUG_RES_DIR/values/bike_station_values.xml;
    fi
    FILTER_AGENCY_AUTHORIY=$(grep -E "<string name=\"bike_station_authority\">(.*)+</string>$" $AGENCY_BIKE_FILE | cut -d ">" -f2 | cut -d "<" -f1);
else
    echo " > No agency file! (rts:$AGENCY_RTS_FILE|bike:$AGENCY_BIKE_FILE)";
    exit -1;
fi
if [[ -z "${FILTER_AGENCY_AUTHORIY}" ]]; then
    echo "> No agency authority found '$FILTER_AGENCY_AUTHORIY'!";
    exit 1;
fi

echo " - agency authority: '$FILTER_AGENCY_AUTHORIY'";

FILTER_SCREEN="home"
if [[ "$NUMBER" -eq 1 ]]; then
    FILTER_SCREEN="home"
elif [[ "$NUMBER" -eq 2 ]]; then
    FILTER_SCREEN="poi"
elif [[ "$NUMBER" -eq 3 ]]; then
    FILTER_SCREEN="browse"
else
    echo "> Unexpected screen number '$NUMBER'!";
    exit 1;
fi

if [[ -z "${ANDROID_HOME}" ]]; then
    echo "> ANDROID_HOME not set '$ANDROID_HOME'!";
    exit 1;
fi
ADB="$ANDROID_HOME/platform-tools/adb";
if [[ ! -f "$ADB" ]]; then
    echo "> adb '$ADB' not fount!";
    exit 1;
fi

echo "> ADB devices: ";
echo "----------";
$ADB devices -l;
echo "----------";

echo "> Setting demo mode...";
DEVICE_AUTO_TIME=$($ADB shell settings get global auto_time);
DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone);
if [[ ! -z "$AGENCY_TIME_ZONE" ]]; then
    echo " - agency time-zone: '$AGENCY_TIME_ZONE'";
    DEVICE_DATE_TIME=$(date --date='TZ="$DEVICE_TIME_ZONE"');
    AGENCY_DATE_TIME=$(date --date='TZ="$AGENCY_TIME_ZONE"');
    if [ "$AGENCY_DATE_TIME" != "$DEVICE_DATE_TIME" ]; then
        if [ "$DEVICE_REBOOT_ALLOWED" = true ] ; then
            $ADB shell settings set global auto_time 0; # turn-off automatic time
            $ADB shell setprop persist.sys.timezone "$AGENCY_TIME_ZONE";
            $ADB reboot;
            $ADB wait-for-device;
            DEVICE_TIME_ZONE=$($ADB shell getprop persist.sys.timezone);
            DEVICE_DATE_TIME=$(date --date='TZ="$DEVICE_TIME_ZONE"');
        fi;
        if [ "$AGENCY_DATE_TIME" != "$DEVICE_DATE_TIME" ]; then
            echo "> Wrong time zone '$DEVICE_TIME_ZONE' ($DEVICE_DATE_TIME) for agency time zone '$AGENCY_TIME_ZONE' ($AGENCY_DATE_TIME)!";
            $ADB shell am start -a android.settings.DATE_SETTINGS;
            exit 1;
        fi
    fi
fi

TIME_FORMAT=$($ADB shell settings get system time_12_24);
if [[ "${LANG}" == "en-US" && "${TIME_FORMAT}" != "12" ]]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ] ; then
        $ADB shell settings put system time_12_24 12;
        $ADB reboot;
        $ADB wait-for-device;
        TIME_FORMAT=$($ADB shell settings get system time_12_24);
    fi;
    if [[ "${LANG}" == "en-US" && "${TIME_FORMAT}" != "12" ]]; then
        echo "> Wrong time format '$TIME_FORMAT' for language '$LANG'!";
        $ADB shell am start -a android.settings.DATE_SETTINGS;
        exit 1
    else 
        echo "> Good time format '$TIME_FORMAT' for language '$LANG'.";
    fi
elif [[ "${LANG}" == "fr-FR" && "${TIME_FORMAT}" != "24" ]]; then
    if [ "$DEVICE_REBOOT_ALLOWED" = true ] ; then
        $ADB shell settings put system time_12_24 12;
        $ADB reboot;
        $ADB wait-for-device;
        TIME_FORMAT=$($ADB shell settings get system time_12_24);
    fi;
    if [[ "${LANG}" == "fr-FR" && "${TIME_FORMAT}" != "24" ]]; then
        echo ">> Wrong time format '$TIME_FORMAT' for language '$LANG'!";
        $ADB shell am start -a android.settings.DATE_SETTINGS;
        exit 1
    else 
        echo ">> Good time format '$TIME_FORMAT' for language '$LANG'.";
    fi
else 
    echo ">> Good time format '$TIME_FORMAT' for language '$LANG'.";
fi
DEMO_ALLOWED=$($ADB shell settings get global sysui_demo_allowed);
if [[ $DEMO_ALLOWED -ne 1 ]]; then
    echo ">> demo was NOT already allowed ($DEMO_ALLOWED).";
    $ADB shell settings put global sysui_demo_allowed 1;
else
    echo ">> demo was already allowed ($DEMO_ALLOWED).";
fi
$ADB shell am broadcast -a com.android.systemui.demo -e command enter;
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false;
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100;
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4;
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4;
$ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false;
$ADB shell am broadcast -a com.android.systemui.demo -e command status -e location show;
echo "> Setting demo mode... DONE";

echo "> Stop app...";
$ADB shell am force-stop $MAIN_PKG;
echo "> Stop app... DONE";

echo "> Starting app...";
$ADB shell am start -n $MAIN_PKG/$SPLASH_SCREEN_ACTIVITY \
--es "filter_agency_authority" "$FILTER_AGENCY_AUTHORIY" \
--es "filter_screen" "$FILTER_SCREEN" \
--es "force_lang" "$LANG" \
;
echo "> Starting app... DONE";

SLEEP_IN_SEC=10;
echo "> Waiting for UI ($SLEEP_IN_SEC seconds)...";
sleep $SLEEP_IN_SEC; # wait for UI to be ready
echo "> Waiting for UI ($SLEEP_IN_SEC seconds)... DONE";


echo "> Capturing screen shot...";
FILE_NAME="$NUMBER.png";
DEVICE_PATH="/sdcard/$FILE_NAME";
$ADB shell screencap -p $DEVICE_PATH;
$ADB pull $DEVICE_PATH $DEST_PATH;
$ADB shell rm $DEVICE_PATH;
echo "> Capturing screen shot... DONE";

echo "> Resetting demo mode...";
$ADB shell am broadcast -a com.android.systemui.demo -e command exit;
$ADB shell settings put global sysui_demo_allowed $DEMO_ALLOWED;
echo "> Resetting demo mode... DONE";

echo "> Stop app...";
$ADB shell am force-stop $MAIN_PKG; # app is in invalid state, stop to remove all from memory
echo "> Stop app... DONE";

echo ">> Capturing Module App Screenshot '$@'... DONE";