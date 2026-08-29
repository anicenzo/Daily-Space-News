# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\aniru\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimization and shrinking
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.ani.dailyspacenews.** { *; } # Keep your data models
-keepattributes *Annotation*

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# AdMob & Play Services
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.ads.** { public *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.**

# AppLovin (Preparing for integration)
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**
