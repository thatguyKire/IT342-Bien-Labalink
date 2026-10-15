# --- LabaLink release shrinking rules ---

# API models are (de)serialised by Gson via reflection: keep names/fields.
-keep class edu.cit.bien.labalink.model.** { *; }

# Retrofit service interface and generic signatures.
-keep interface edu.cit.bien.labalink.api.LabaLinkApi { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Gson TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Strip verbose/debug logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
