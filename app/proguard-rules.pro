# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep SNMP4J classes
-keep class org.snmp4j.** { *; }
-keep class com.example.** { *; }

# Keep all classes in snmp4j packages
-keep class org.snmp4j.** { *; }
-dontwarn org.snmp4j.**

# Keep logging classes
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# Keep Android service classes
-keep class com.example.snmpagent.** { *; }

