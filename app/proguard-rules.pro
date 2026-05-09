# Regras ProGuard para o SAC.
# Quando minifyEnabled for ativado no release, adicionar regras específicas
# para Retrofit, Room e kotlinx.serialization aqui.

# kotlinx.serialization — preserva as classes anotadas com @Serializable
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
