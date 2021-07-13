#!/bin/bash
echo ">> Capturing Module App Screenshot '$@'...";

if [[ "$#" -ne 3 ]]; then
    echo "> Wrong $# parameters '$@'!";
    echo "- Ex: 'app-android: ../commons-android/pub/module-app-screenshot.sh en phone 1'";
    exit -1;
fi

LANG=$1;
TYPE=$2;
NUMBER=$3;

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
SPLASH_SCREEN_ACTIVITY="org.mtransit.android.ui.SplashScreenActivity";

RES_DIR=src/main/res;
AGENCY_RTS_FILE=$RES_DIR/values/gtfs_rts_values_gen.xml;
AGENCY_BIKE_FILE=$RES_DIR/values/bike_station_values.xml;
FILTER_TYPE=-1;
if [ -f $AGENCY_RTS_FILE ]; then
    echo "> Agency file: '$AGENCY_BIKE_FILE'.";
    # https://github.com/mtransitapps/parser/blob/master/src/main/java/org/mtransit/parser/gtfs/data/GRouteType.kt
    FILTER_TYPE=$(grep -E "<integer name=\"gtfs_rts_agency_type\">[0-9]+</integer>$" $AGENCY_RTS_FILE | tr -dc '0-9');
elif [ -f $AGENCY_BIKE_FILE ]; then
    echo "> Agency file: '$AGENCY_BIKE_FILE'.";
    FILTER_TYPE=100;
else
    echo " > No agency file! (rts:$AGENCY_RTS_FILE|bike:$AGENCY_BIKE_FILE)";
    exit -1;
fi
if [ $FILTER_TYPE -eq -1 ] ; then
    echo " > No type found for agency!";
    exit -1;
fi
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
$ADB devices -l;

echo "> Setting demo mode...";
$ADB shell settings put global sysui_demo_allowed 1;
$ADB shell am broadcast -a com.android.systemui.demo -e command enter;
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false;
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100;
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4;
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4;
$ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false;
echo "> Setting demo mode... DONE";

echo "> Stop app...";
$ADB shell am force-stop $MAIN_PKG;
echo "> Stop app... DONE";

echo "> Starting app...";
$ADB shell am start -n $MAIN_PKG/$SPLASH_SCREEN_ACTIVITY \
--es "filter_agency_authority" "$FILTER_AGENCY_AUTHORIY" \
--es "filter_location" "$FILTER_LOCATION" \
--es "filter_screen" "$FILTER_SCREEN" \
--es "filter_uuid" "$FILTER_UUID" \
--es "filter_type" "$FILTER_TYPE" \
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
echo "> Resetting demo mode... DONE";


echo ">> Capturing Module App Screenshot '$@'... DONE";
