# ── OmniFiles ProGuard / R8 Rules ────────────────────────────────────────

# Keep the app entry point
-keep class com.omnilabs.omfiles.OmniFilesApp { *; }
-keep class com.omnilabs.omfiles.MainActivity { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keepclassmembers class * { @dagger.hilt.android.AndroidEntryPoint *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-keep @androidx.compose.runtime.Composable class * { *; }

# Data classes
-keep class com.omnilabs.omfiles.domain.model.** { *; }
-keep class com.omnilabs.omfiles.data.local.** { *; }
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Apache Commons Compress
-dontwarn org.apache.commons.logging.**
-keep class org.apache.commons.compress.** { *; }

# junrar
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**

# Gson / serialization
-keepattributes Signature
-keepattributes *Annotation*

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# Remove debug symbols
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile,!LineNumberTable
