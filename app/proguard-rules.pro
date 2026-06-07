# Keep Media3
-keep class androidx.media3.** { *; }
# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.rombaro.tv.**$$serializer { *; }
-keepclassmembers class com.rombaro.tv.** {
    *** Companion;
}
-keepclasseswithmembers class com.rombaro.tv.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
