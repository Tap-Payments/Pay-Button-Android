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
# Tap Card Form
#########################################

-keep class company.tap.tapcardformkit.** { *; }


#########################################
# ML Kit
#########################################

-keep class com.google.mlkit.** { *; }


#########################################
# Retrofit
#########################################

-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations


#########################################
# Your SDK models
#########################################

-keep class company.tap.tappaybutton.models.** { *; }