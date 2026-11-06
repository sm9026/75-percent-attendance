# Add project specific ProGuard rules here.
-keepattributes *Annotation*,InnerClasses,Signature
-keep class com.attendanceradar.app.data.backup.** { *; }
-keep class com.attendanceradar.app.data.local.entity.** { *; }
-dontwarn kotlinx.serialization.**
