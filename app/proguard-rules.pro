# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

#-------------------------------------------------
# General Settings
#-------------------------------------------------

# Keep metadata for better debugging while still obfuscating
-keepattributes Signature,Exceptions,InnerClasses,*Annotation*,EnclosingMethod,SourceFile,LineNumberTable

# Keep the BuildConfig class
-keep class com.supernova.wheresmydevice.BuildConfig { *; }

#-------------------------------------------------
# Critical Android Components - DO NOT OBFUSCATE
#-------------------------------------------------

# Keep all activities, services, receivers, providers
-keep public class * extends android.app.Activity
-keep public class * extends androidx.activity.ComponentActivity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep specific components explicitly 
-keep public class com.supernova.wheresmydevice.MainActivity { *; }
-keep public class com.supernova.wheresmydevice.SettingsActivity { *; }
-keep public class com.supernova.wheresmydevice.PermissionsActivity { *; }
-keep public class com.supernova.wheresmydevice.CommandSettingsActivity { *; }
-keep public class com.supernova.wheresmydevice.WhitelistActivity { *; }
-keep public class com.supernova.wheresmydevice.AboutActivity { *; }
-keep public class com.supernova.wheresmydevice.receiver.SmsReceiver { *; }
-keep public class com.supernova.wheresmydevice.service.OverlayDisplayService { *; }

#-------------------------------------------------
# Jetpack Compose Rules
#-------------------------------------------------

# Keep Compose-related classes
-keep class androidx.compose.** { *; }

# Keep composable functions
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep generated code from Compose compiler
-keepclassmembers class * {
    <init>(androidx.compose.runtime.Composer, int);
}

#-------------------------------------------------
# For Reflection-based code
#-------------------------------------------------

# Keep classes that are accessed via reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Application class
-keep public class com.supernova.wheresmydevice.** extends android.app.Application { *; }

# Keep the utils package
-keep class com.supernova.wheresmydevice.utils.** { *; }

#-------------------------------------------------
# Kotlin-specific rules
#-------------------------------------------------

# Keep Kotlin serialization
-keepattributes Runtime*,Annotation*,*Annotation*
-keepclassmembers class kotlinx.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

#-------------------------------------------------
# Third-party libraries
#-------------------------------------------------

# Material Components
-keep class com.google.android.material.** { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

#-------------------------------------------------
# Keep names for clearer stacktraces
#-------------------------------------------------

# RemoteViews
-keepclassmembers class * extends android.widget.RemoteViews { *; }

# Make crash reports more readable
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove toast messages in release builds if needed
# -assumenosideeffects class android.widget.Toast {
#    public static *** makeText(...);
#    public void show(...);
# }

# Keep dialog-related classes that may use reflection
-keep class com.supernova.wheresmydevice.dialogs.** { *; }

# Keep any classes that use reflection
-keepclassmembers class * {
    public static java.lang.Class getClass(java.lang.String);
    public static java.lang.Class class$(java.lang.String);
}

# Keep any classes referenced via Class.forName()
-keepclassmembers class * {
    public static void main(java.lang.String[]);
}

# Preserve the special static methods that are required in all enumeration classes
-keepclassmembers enum * { 
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep permission-related classes
-keep class com.supernova.wheresmydevice.utils.PermissionManager { *; }
-keep class com.supernova.wheresmydevice.utils.OverlayPermissionResultTracker { *; }

# Keep Android XML layout accessors
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}