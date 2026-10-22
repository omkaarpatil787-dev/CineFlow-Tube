# ============================================================================
# CineFlow Tube - Production R8 / ProGuard Optimization Rules
# Version: 1.0.0
# ============================================================================

# --- Source and Line Information for Stack Traces ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Universal Native Method Bindings ---
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- NextLib Media3 Extensions & FFmpeg Native Audio/Video Decoders ---
-keep class io.github.anilbeesetti.nextlib.** { *; }
-dontwarn io.github.anilbeesetti.nextlib.**
-keepclasseswithmembernames class io.github.anilbeesetti.nextlib.** {
    native <methods>;
}
-keep public class io.github.anilbeesetti.nextlib.media3ext.** {
    public <init>(...);
    public *;
}

# --- AndroidX Media3 (ExoPlayer, Session, UI, Common, HLS, DASH) ---
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# --- Jetpack Compose Runtime & State ---
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# --- Kotlin Coroutines & Serialization ---
-dontwarn kotlinx.coroutines.**
-keepattributes *Annotation*,InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp & Networking ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Coil Image Loading ---
-dontwarn coil.**
-keep class coil.** { *; }

# --- CineFlow Tube Data Models ---
-keep class com.cineflow.tube.data.model.** { *; }
