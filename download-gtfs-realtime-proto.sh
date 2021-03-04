#!/bin/bash
source ../commons/commons.sh
echo ">> Downloading GTFS Real-Time proto..."
URL="https://developers.google.com/transit/gtfs-realtime/gtfs-realtime.proto"
PROTO_FILE="src/main/proto/gtfs-realtime.proto"
download "${URL}" "${PROTO_FILE}"
checkResult $?
echo ">> Downloading GTFS Real-Time proto... DONE"
