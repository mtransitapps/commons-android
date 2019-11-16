#!/bin/bash
source ../commons/commons.sh
echo ">> Building...";
setGradleArgs;
echo "GRADLE_ARGS: $GRADLE_ARGS";
DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="../settings.gradle.all";
if [[ -f ${CUSTOM_SETTINGS_GRADLE_FILE} ]]; then
	../gradlew -c ${CUSTOM_SETTINGS_GRADLE_FILE} :${DIRECTORY}:clean :${DIRECTORY}:assemble ${GRADLE_ARGS};
	RESULT=$?;
else
	../gradlew :${DIRECTORY}:clean :${DIRECTORY}:assemble ${GRADLE_ARGS};
	RESULT=$?;
fi
echo ">> Building... DONE";
exit ${RESULT};
