# kotlinx.serialization: keep the generated serializers of every model in :core.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class dk.jagtfokus.core.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class dk.jagtfokus.core.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class dk.jagtfokus.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
