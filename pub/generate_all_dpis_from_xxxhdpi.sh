#!/bin/bash
set -euo pipefail # -x
SCRIPT_DIR="$(dirname "$0")"
ROOT_DIR="$SCRIPT_DIR/../..";
source ${ROOT_DIR}/commons/commons.sh;

if [[ "$#" -ne 1 ]]; then
  echo "> Wrong $# parameters '$*'!"
  exit 1 #error
fi

SOURCE=$1
echo "SOURCE: $SOURCE."

if [[ ! -f "$SOURCE" ]]; then
  echo "> File '$SOURCE' does not exist!"
  exit 1 #error
fi

SOURCE_DIR=$(dirname "$SOURCE")
echo "SOURCE_DIR: $SOURCE_DIR."

SOURCE_NAME=$(basename "$SOURCE")
echo "SOURCE_NAME: $SOURCE_NAME."

SOURCE_DIR_NAME=$(basename "$SOURCE_DIR")
echo "SOURCE_DIR_NAME: $SOURCE_DIR_NAME."

if [[ "$SOURCE_DIR_NAME" != "drawable-xxxhdpi" ]]; then
  echo "> Wrong source drawable DPI '$SOURCE_DIR_NAME'!"
  exit 1 #error
fi

RES_DIR=$(dirname "$SOURCE_DIR")
echo "RES_DIR: $RES_DIR."

RES_DIR_NAME=$(basename "$RES_DIR")
echo "RES_DIR_NAME: $RES_DIR_NAME."

if [[ "$RES_DIR_NAME" != "res" ]]; then
  echo "> Wrong source resource directory '$RES_DIR_NAME'!"
  exit 1 #error
fi

requireCommand "convert" "imagemagick";

mkdir -p "$RES_DIR/drawable-xxhdpi"
convert $SOURCE -resize 75% $RES_DIR/drawable-xxhdpi/$SOURCE_NAME
mkdir -p "$RES_DIR/drawable-xhdpi"
convert $SOURCE -resize 50% $RES_DIR/drawable-xhdpi/$SOURCE_NAME
mkdir -p "$RES_DIR/drawable-hdpi"
convert $SOURCE -resize 37.5% $RES_DIR/drawable-hdpi/$SOURCE_NAME
mkdir -p "$RES_DIR/drawable-mdpi"
convert $SOURCE -resize 25% $RES_DIR/drawable-mdpi/$SOURCE_NAME

echo "Done"
exit 0 #ok
