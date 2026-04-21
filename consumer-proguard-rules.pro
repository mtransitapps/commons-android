# GTFS Realtime - START
-dontwarn com.google.transit.realtime.**
# GTFS Realtime - END

# Protobuf - START
# https://github.com/protocolbuffers/protobuf/blob/main/java/lite.md#r8-rule-to-make-production-app-builds-work
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
# Protobuf - END