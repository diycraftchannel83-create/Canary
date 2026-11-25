# Keep generic signatures and runtime annotations for Firestore POJO mapping
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep all Firestore model classes & @Keep annotated members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep class com.example.data.model.** { *; }

# Keep Google Sign-In / Credential Manager & Play Services classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Keep BroadcastReceiver & Application registered in AndroidManifest.xml
-keep class com.example.notifications.BreedingReminderReceiver { *; }
-keep class com.example.KenariTasikApplication { *; }

# Keep Google Mobile Ads SDK callbacks
-keep class com.google.android.gms.ads.** { *; }


