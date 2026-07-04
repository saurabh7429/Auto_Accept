# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room entities
-keep class com.rideautoacceptor.data.model.** { *; }

# Keep DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Keep accessibility service
-keep class com.rideautoacceptor.service.** { *; }
