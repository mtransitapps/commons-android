#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR="${SCRIPT_DIR}/..";

source ${ROOT_DIR}/commons/sh/commons.sh

echo ">> Building...";

setGradleArgs;
echo "GRADLE_ARGS: $GRADLE_ARGS";

DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="$ROOT_DIR/settings.gradle.all";
if [[ -f ${CUSTOM_SETTINGS_GRADLE_FILE} ]]; then
	$ROOT_DIR/gradlew -c ${CUSTOM_SETTINGS_GRADLE_FILE} :${DIRECTORY}:clean :${DIRECTORY}:assemble ${GRADLE_ARGS};
	RESULT=$?;
else
	$ROOT_DIR/gradlew :${DIRECTORY}:clean :${DIRECTORY}:assemble ${GRADLE_ARGS};
	RESULT=$?;
fi

echo ">> Building... DONE";
exit ${RESULT};
