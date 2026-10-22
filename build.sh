#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR="${SCRIPT_DIR}/..";

source ${ROOT_DIR}/commons/sh/commons.sh

echo ">> Building...";

setGradleArgs;
echo "GRADLE_ARGS: $GRADLE_ARGS";

DIRECTORY=$(basename ${PWD});
$ROOT_DIR/gradlew :${DIRECTORY}:clean :${DIRECTORY}:assemble ${GRADLE_ARGS};
RESULT=$?;

echo ">> Building... DONE";
exit ${RESULT};
