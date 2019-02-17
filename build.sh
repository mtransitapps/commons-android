#!/bin/bash
echo ">> Building...";
DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="../settings.gradle.all";
if [[ -f ${CUSTOM_SETTINGS_GRADLE_FILE} ]]; then
	../gradlew -c ${CUSTOM_SETTINGS_GRADLE_FILE} :${DIRECTORY}:clean :${DIRECTORY}:assemble;
	RESULT=$?;
else
	../gradlew :${DIRECTORY}:clean :${DIRECTORY}:assemble;
	RESULT=$?;
fi
echo ">> Building... DONE";
exit ${RESULT};
