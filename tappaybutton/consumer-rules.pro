#########################################
# General
#########################################

-keepclassmembers class * {
    static java.lang.String *;
}


#########################################
# Gson
#########################################

-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }


#########################################
# WebView JavaScript Interface
#########################################

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}


#########################################
# Tap Card Form SDK
#########################################

-keep class company.tap.tapcardformkit.** { *; }


#########################################
# Tap Pay Button SDK
#########################################

-keep class company.tap.tappaybutton.models.** { *; }


#########################################
# ML Kit
#########################################

-keep class com.google.mlkit.** { *; }


#########################################
# Retrofit
#########################################

-keep class retrofit2.** { *; }


#########################################
# OkHttp
#########################################

-keep class okhttp3.** { *; }


#########################################
# Okio
#########################################

-keep class okio.** { *; }


#########################################
# Retrofit Gson Converter
#########################################

-keep class retrofit2.converter.gson.** { *; }