# Standard Android rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class com.moneybag.nativeapp.data.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.moneybag.nativeapp.data.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.ads.** { *; }

# NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# ViewBinding
-keep class com.moneybag.nativeapp.databinding.** { *; }
