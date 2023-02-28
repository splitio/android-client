# Please include these rules in your project
# in order to make Split code work properly when
# using proguard
-dontwarn android.test.**
-dontwarn org.junit.**
-dontwarn com.google.common.**
-keep class io.split.android.client.utils.deserializer.EventDeserializer { *; }
-keep class io.split.android.client.dtos.** { *; }
-keep class io.split.android.client.storage.db.** { *; }
-keep class io.split.android.client.service.sseclient.EventStreamParser { *; }
-keep class io.split.android.client.service.sseclient.SseAuthToken { *; }
-keep class io.split.android.client.service.sseclient.SseJwtToken { *; }
-keep class io.split.android.client.service.sseclient.SseAuthenticationResponse { *; }
-keep class io.split.android.client.service.sseclient.notifications.** { *; }
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
