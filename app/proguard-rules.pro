# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:/android-sdks/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn kotlin.**
-dontwarn org.w3c.dom.events.*
-dontwarn org.jetbrains.kotlin.di.InjectorForRuntimeDescriptorLoader

-keep class kotlin.** { *; }
#-keep class kotlin.reflect.** { *; }
#-keep class org.jetbrains.kotlin.** { *; }

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

-keepattributes InnerClasses
-keep class com.github.mikephil.charting.** { *; }
# -keep class *

-keepattributes SourceFile,LineNumberTable
