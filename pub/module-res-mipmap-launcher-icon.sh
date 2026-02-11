#!/bin/bash
SCRIPT_DIR="$(dirname "$0")"
source "${SCRIPT_DIR}/../../commons/commons.sh"
echo ">> Converting Module App Icon to Launcher Icon...";

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

SOURCE="$APP_ANDROID_DIR/src/main/play/listings/en-US/graphics/icon/1.png";
if [ ! -f $SOURCE ]; then
    echo "> Missing Play listing app icon at '$SOURCE'!";
    exit 1; #error
fi

echoDebug "> Creating temporary file...";
echoDebug " - source: '$SOURCE'";
TEMP="temp_module_app_icon.png";
echoDebug " - temp: '$TEMP'";
cp $SOURCE $TEMP;
echoDebug "> Creating temporary file... DONE";

requireCommand "convert" "imagemagick";

IMAGE_MAGIC_VERSION=$(convert -version);
echoDebug "> ImageMagic version: $IMAGE_MAGIC_VERSION";
# https://imagemagick.org/script/command-line-options.php

# 75% in https://developer.android.com/google-play/resources/icon-design-specifications
# 66-85% in https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive
# 90% in https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html

echoDebug "> Cropping...";
WIDTH=512;
echoDebug " - width: $WIDTH";
HEIGHT=512;
echoDebug " - height: $HEIGHT";
CROP="77"; # 66 for bikes ?
CROP_WIDTH=$((WIDTH * CROP / 100));
CROP_HEIGHT=$((HEIGHT * CROP / 100));
OFFSET_X=$(((WIDTH - CROP_WIDTH) / 2 ));
OFFSET_Y=$(((HEIGHT - CROP_HEIGHT) / 2 ));
echoDebug " - crop $CROP% width,height: ${CROP_WIDTH}x${CROP_HEIGHT}";
echoDebug " - offset +X+Y: +${OFFSET_X}+${OFFSET_Y}";
convert $TEMP \
-crop ${CROP_WIDTH}x${CROP_HEIGHT}+${OFFSET_X}+${OFFSET_Y} \
$TEMP;
checkResult $?
echoDebug "> Cropping circle... DONE";

echoDebug "> Clipping circle...";
# READ CENTER X,Y
CX=$(convert $TEMP -format "%[fx:int(w/2)]" info:)
CY=$(convert $TEMP -format "%[fx:int(h/2)]" info:)
# FIND POINT ON CIRCLE CIRCUMFERENCE
PT="0,$CY"
[ "$CX" -gt "$CY" ] && PT="$CX,0";
echoDebug " - center X,Y: $CX,$CY";
echoDebug " - point: $PT";
convert $TEMP \
\( +clone -fill black -colorize 100% -fill white -draw "circle $CX,$CY $PT" -alpha off \) \
-compose copyopacity -composite \
-trim +repage \
$TEMP;
checkResult $?
echoDebug "> Clipping circle... DONE";

echoDebug "> Adding padding...";
SCALE="90";
WIDTH_CONTENT=$((CROP_WIDTH * SCALE / 100));
HEIGHT_CONTENT=$((CROP_HEIGHT * SCALE / 100));
echoDebug " - scale: $SCALE% ${WIDTH_CONTENT} x ${HEIGHT_CONTENT}";
convert $TEMP \
-background transparent -gravity center \
-scale ${WIDTH_CONTENT}x${HEIGHT_CONTENT} \
-extent ${CROP_WIDTH}x${CROP_HEIGHT} \
$TEMP;
checkResult $?
echoDebug "> Adding padding... DONE";

echoDebug "> Generating mipmap DPI...";
PARAM="-unsharp 1x4 -strip"; # TODO ?
RES_DIR="$APP_ANDROID_DIR/src/main/res";
MIPMAP_NAME="module_app_icon";
# convert ${TEMP} -resize '36x36!' ${PARAM} ${RES_DIR}/mipmap-ldpi/${MIPMAP_NAME}.png" # NOT SUPPORTED
convert ${TEMP} -resize '48x48!' ${PARAM} ${RES_DIR}/mipmap-mdpi/${MIPMAP_NAME}.png;
checkResult $?
convert ${TEMP} -resize '72x72!' ${PARAM} ${RES_DIR}/mipmap-hdpi/${MIPMAP_NAME}.png;
checkResult $?
convert ${TEMP} -resize '96x96!' ${PARAM} ${RES_DIR}/mipmap-xhdpi/${MIPMAP_NAME}.png;
checkResult $?
convert ${TEMP} -resize '144x144!' ${PARAM} ${RES_DIR}/mipmap-xxhdpi/${MIPMAP_NAME}.png;
checkResult $?
convert ${TEMP} -resize '192x192!' ${PARAM} ${RES_DIR}/mipmap-xxxhdpi/${MIPMAP_NAME}.png;
checkResult $?
echoDebug "> Generating mipmap DPI... DONE";

echoDebug "> Cleaning temporary file '$TEMP'...";
rm $TEMP;
echoDebug "> Cleaning temporary file '$TEMP'... DONE";

# TODO later $RES_DIR/mipmap-anydpi-v26/$MIPMAP_NAME.xml + $RES_DIR/mipmap-*/$MIPMAP_NAME_foreground.xml

echo -e "\n>> Converting Module App Icon to Launcher Icon... DONE";
