#!/bin/bash
echo ">> Convering Module App Icon to Launcher Icon...";

SOURCE="src/main/play/listings/en-US/graphics/icon/1.png";
echo " - source: '$SOURCE'";

if [ ! -f $SOURCE ]; then
    echo "> Missing Play listing app icon at '$SOURCE'!";
    exit -1;
fi

TEMP="temp_module_app_icon.png";
echo " - temp: '$TEMP'";

WIDTH=512;
echo " - width: $WIDTH";
HEIGHT=512;
echo " - height: $HEIGHT";

IMAGE_MAGIC_VERSION=$(convert -version);
echo "> ImageMagic version: $IMAGE_MAGIC_VERSION";
# https://imagemagick.org/script/command-line-options.php

# READ CENTER X,Y
CX=$(convert $SOURCE -format "%[fx:int(w/2)]" info:)
CY=$(convert $SOURCE -format "%[fx:int(h/2)]" info:)
# FIND POINT ON CIRCLE CIRCUMFERENCE
PT="0,$CY"
[ $CX -gt $CY ] && PT="$CX,0";

echo " - center X,Y: $CX,$CY";
echo " - point: $PT";

echo "> Cliping circle...";
convert $SOURCE \
\( +clone -fill black -colorize 100% -fill white -draw "circle $CX,$CY $PT" -alpha off \) \
-compose copyopacity -composite \
-trim +repage \
$TEMP;
echo "> Cliping circle... DONE";

# 75% in https://developer.android.com/google-play/resources/icon-design-specifications
# 66-85% in https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive
# 90% in https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
SCALE="90";
WIDTH_CONTENT=$(($WIDTH * $SCALE / 100));
HEIGHT_CONTENT=$(($HEIGHT * $SCALE / 100));

echo " - scale: ${WIDTH_CONTENT} x ${HEIGHT_CONTENT}";

echo "> Adding padding...";
convert $TEMP \
-background transparent -gravity center \
-scale ${WIDTH_CONTENT}x${HEIGHT_CONTENT} \
-extent ${WIDTH}x${HEIGHT} \
$TEMP;
echo "> Adding padding... DONE";

echo "> Generating mipmap DPI...";
# convert "$TEMP" -resize '36x36' -unsharp 1x4 "src/main/res/mipmap-ldpi/module_app_icon.png" # NOT SUPPORTED
convert "$TEMP" -resize '48x48' -unsharp 1x4 "src/main/res/mipmap-mdpi/module_app_icon.png";
convert "$TEMP" -resize '72x72' -unsharp 1x4 "src/main/res/mipmap-hdpi/module_app_icon.png";
convert "$TEMP" -resize '96x96' -unsharp 1x4 "src/main/res/mipmap-xhdpi/module_app_icon.png";
convert "$TEMP" -resize '144x144' -unsharp 1x4 "src/main/res/mipmap-xxhdpi/module_app_icon.png";
convert "$TEMP" -resize '192x192' -unsharp 1x4 "src/main/res/mipmap-xxxhdpi/module_app_icon.png";
echo "> Generating mipmap DPI... DONE";

echo "> Cleaning temporary file '$TEMP'...";
rm $TEMP;
echo "> Cleaning temporary file '$TEMP'... DONE";

echo ">> Convering Module App Icon to Launcher Icon... DONE";
