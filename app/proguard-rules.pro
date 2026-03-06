# ProGuard rules for SafeLand

# Keep model classes for serialization
-keepclassmembers class com.safeland.chat.model.** {
    *;
}

# BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }

# Prevent warnings
-dontwarn java.lang.invoke.StringConcatFactory
