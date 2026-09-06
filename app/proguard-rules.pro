# bca_mobile ProGuard/R8 Rules

# Kotlin Serialization (used by Navigation type-safe routes)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable route classes
-keep,includedescriptorclasses class id.bca.bcamobile.**$$serializer { *; }
-keepclassmembers class id.bca.bcamobile.** {
    *** Companion;
}
-keepclasseswithmembers class id.bca.bcamobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}