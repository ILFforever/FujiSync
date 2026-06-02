# Keep PTP/USB data classes (fields accessed via reflection by metadata-extractor)
-keepclassmembers class com.paeki.fujirecipes.data.** { *; }

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# metadata-extractor
-keep class com.drew.** { *; }
