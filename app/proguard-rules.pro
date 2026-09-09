# JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# ARC-X JNI classes
-keep class com.arcx.** {
    *;
}

# Mundo Virtual Engine
-keep class com.mundo.** {
    *;
}
-dontwarn com.mundo.**

# FreeReflection
-keep class me.weishu.reflection.** {
    *;
}
-dontwarn me.weishu.reflection.**

# BlackReflection
-keep class com.github.CodingGay.BlackReflection.** {
    *;
}
-dontwarn com.github.CodingGay.BlackReflection.**

# LSParanoid
-keep @org.lsposed.lsparanoid.Obfuscate class * {
    *;
}

# Annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod