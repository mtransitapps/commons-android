#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd);

source "${ROOT_DIR}/commons/sh/commons.sh"

echo ">> Building...";

setGradleArgs;
echo "GRADLE_ARGS: $GRADLE_ARGS";

"${ROOT_DIR}/gradlew" :commons-android:clean :commons-android:assemble ${GRADLE_ARGS};
RESULT=$?;

echo ">> Building... DONE";
exit ${RESULT};
