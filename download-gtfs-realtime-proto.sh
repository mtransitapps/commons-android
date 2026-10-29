#!/bin/bash
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd);
    
source "${ROOT_DIR}/commons/sh/commons.sh";

# Keep in sync with ".github/workflows/mt-download-gtfs-rt-proto.yml"
echo ">> Downloading GTFS Real-Time proto..."
# URL="https://developers.google.com/static/transit/gtfs-realtime/gtfs-realtime.proto"
# URL="https://raw.githubusercontent.com/google/transit/master/gtfs-realtime/proto/gtfs-realtime.proto"
URL="https://gtfs.org/documentation/realtime/gtfs-realtime.proto"
PROTO_FILE="${SCRIPT_DIR}/src/main/proto/gtfs-realtime.proto"
download "${URL}" "${PROTO_FILE}"
checkResult $?
echo ">> Downloading GTFS Real-Time proto... DONE"
