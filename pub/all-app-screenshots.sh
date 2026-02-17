#!/bin/bash
SCRIPT_DIR="$(dirname "$0")"
ROOT_DIR="$SCRIPT_DIR/../.."
source ${ROOT_DIR}/commons/commons.sh

echo ">> Capturing All App Screenshots..."

setGitProjectName

if [[ $PROJECT_NAME == "mtransit-for-android" ]]; then
  echo ">> Capturing All App Screenshots... SKIP ('$PROJECT_NAME' screenshots not supported yet)"
  exit 1 # error
fi

APP_ANDROID_DIR="$ROOT_DIR/app-android"
LISTINGS_DIR="$APP_ANDROID_DIR/src/main/play/listings"

for DIR in $LISTINGS_DIR/*; do
    if [[ -d "$DIR" ]]; then
        LANG=$(basename $DIR)
        echo " - lang: '$LANG'"
        GRAPHICS_DIR="$LISTINGS_DIR/$LANG/graphics"
        mkdir -p "$GRAPHICS_DIR"
        for KIND_DIR in $GRAPHICS_DIR/*-screenshots; do
            if [[ -d "$KIND_DIR" ]]; then
                KIND=$(basename $KIND_DIR)
                KIND="${KIND%"-screenshots"}"
                echo " - kind: '$KIND'"
                # 3 screenshots minimum
                for i in {1..3}; do
                    [[ -f "$KIND_DIR/${i}.png" ]] || touch "$KIND_DIR/${i}.png"
                done
                for IDX_FILE in $KIND_DIR/*.png; do
                    if [[ -f "$IDX_FILE" ]]; then
                        IDX=$(basename $IDX_FILE)
                        IDX="${IDX%.png}"
                        echo " - idx: '$IDX'"
                        ${SCRIPT_DIR}/module-app-screenshot.sh "$LANG" "$KIND" "$IDX"
                        checkResult $?
                    fi
                done
            fi
        done
    fi
done

echo ">> Capturing All App Screenshots... DONE"
