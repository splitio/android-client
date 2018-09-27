# Please include these rules in your project
# in order to make Split code work properly when
# using proguard
-dontwarn android.test.**
-dontwarn org.junit.**
-dontwarn com.google.common.**
-keep class io.split.android.client.dtos.* { *; }