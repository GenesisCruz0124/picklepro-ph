# Supabase-kt / Ktor / kotlinx.serialization reflect on DTOs at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class com.gentech.picklepro.**.*Dto {
    *;
}
-keep,includedescriptorclasses class com.gentech.picklepro.**$$serializer { *; }
-keepclassmembers class com.gentech.picklepro.** {
    *** Companion;
}
-keepclasseswithmembers class com.gentech.picklepro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
