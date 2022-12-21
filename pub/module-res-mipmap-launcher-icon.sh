#!/bin/bash
echo ">> Converting Module App Icon to Launcher Icon...";

SOURCE="src/main/play/listings/en-US/graphics/icon/1.png";
if [ ! -f $SOURCE ]; then
    echo "> Missing Play listing app icon at '$SOURCE'!";
    exit 1; #error
fi

echo "> Creating temporary file...";
echo " - source: '$SOURCE'";
TEMP="temp_module_app_icon.png";
echo " - temp: '$TEMP'";
cp $SOURCE $TEMP;
echo "> Creating temporary file... DONE";

IMAGE_MAGIC_VERSION=$(convert -version);
echo "> ImageMagic version: $IMAGE_MAGIC_VERSION";
convert -version;
# https://imagemagick.org/script/command-line-options.php

# 75% in https://developer.android.com/google-play/resources/icon-design-specifications
# 66-85% in https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive
# 90% in https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html

echo "> Cropping...";
WIDTH=512;
echo " - width: $WIDTH";
HEIGHT=512;
echo " - height: $HEIGHT";
CROP="77"; # 66 for bikes ?
CROP_WIDTH=$((WIDTH * CROP / 100));
CROP_HEIGHT=$((HEIGHT * CROP / 100));
OFFSET_X=$(((WIDTH - CROP_WIDTH) / 2 ));
OFFSET_Y=$(((HEIGHT - CROP_HEIGHT) / 2 ));
echo " - crop $CROP% width,height: ${CROP_WIDTH}x${CROP_HEIGHT}";
echo " - offset +X+Y: +${OFFSET_X}+${OFFSET_Y}";
convert $TEMP \
-crop ${CROP_WIDTH}x${CROP_HEIGHT}+${OFFSET_X}+${OFFSET_Y} \
$TEMP;
echo "> Cropping circle... DONE";

echo "> Clipping circle...";
# READ CENTER X,Y
CX=$(convert $TEMP -format "%[fx:int(w/2)]" info:)
CY=$(convert $TEMP -format "%[fx:int(h/2)]" info:)
# FIND POINT ON CIRCLE CIRCUMFERENCE
PT="0,$CY"
[ "$CX" -gt "$CY" ] && PT="$CX,0";
echo " - center X,Y: $CX,$CY";
echo " - point: $PT";
convert $TEMP \
\( +clone -fill black -colorize 100% -fill white -draw "circle $CX,$CY $PT" -alpha off \) \
-compose copyopacity -composite \
-trim +repage \
$TEMP;
echo "> Clipping circle... DONE";

echo "> Adding padding...";
SCALE="90";
WIDTH_CONTENT=$((CROP_WIDTH * SCALE / 100));
HEIGHT_CONTENT=$((CROP_HEIGHT * SCALE / 100));
echo " - scale: $SCALE% ${WIDTH_CONTENT} x ${HEIGHT_CONTENT}";
convert $TEMP \
-background transparent -gravity center \
-scale ${WIDTH_CONTENT}x${HEIGHT_CONTENT} \
-extent ${CROP_WIDTH}x${CROP_HEIGHT} \
$TEMP;
echo "> Adding padding... DONE";

echo "> Generating mipmap DPI...";
PARAM="-unsharp 1x4 -strip"; # TODO ?
RES_DIR="src/main/res";
MIPMAP_NAME="module_app_icon";
# convert "$TEMP" -resize '36x36!' $PARAM "$RES_DIR/mipmap-ldpi/$MIPMAP_NAME.png" # NOT SUPPORTED
convert "$TEMP" -resize '48x48!' "$PARAM" "$RES_DIR/mipmap-mdpi/$MIPMAP_NAME.png";
convert "$TEMP" -resize '72x72!' "$PARAM" "$RES_DIR/mipmap-hdpi/$MIPMAP_NAME.png";
convert "$TEMP" -resize '96x96!' "$PARAM" "$RES_DIR/mipmap-xhdpi/$MIPMAP_NAME.png";
convert "$TEMP" -resize '144x144!' "$PARAM" "$RES_DIR/mipmap-xxhdpi/$MIPMAP_NAME.png";
convert "$TEMP" -resize '192x192!' "$PARAM" "$RES_DIR/mipmap-xxxhdpi/$MIPMAP_NAME.png";
echo "> Generating mipmap DPI... DONE";

echo "> Cleaning temporary file '$TEMP'...";
rm $TEMP;
echo "> Cleaning temporary file '$TEMP'... DONE";

# TODO later $RES_DIR/mipmap-anydpi-v26/$MIPMAP_NAME.xml + $RES_DIR/mipmap-*/$MIPMAP_NAME_foreground.xml

echo ">> Converting Module App Icon to Launcher Icon... DONE";
