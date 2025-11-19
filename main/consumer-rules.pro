# Consumer ProGuard rules for Split Android SDK
# These rules are automatically applied to apps that depend on this library

# Suppress warnings for java.beans classes (not available on Android)
# These are referenced by snakeyaml but not actually used on Android
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
