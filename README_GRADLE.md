# Gradle Configuration

## Java Version Compatibility

This project uses Gradle 8.5 which supports Java up to version 19. If you're using Java 21, you have two options:

### Option 1: Use Java 17 (Recommended)
Configure Android Studio to use Java 17:
1. File → Settings → Build, Execution, Deployment → Build Tools → Gradle
2. Set "Gradle JDK" to Java 17 (or download it if not available)

### Option 2: Use Gradle 9.0 (If you must use Java 21)
Update `gradle/wrapper/gradle-wrapper.properties`:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-milestone-1-bin.zip
```

## Current Configuration
- Gradle: 8.5
- Android Gradle Plugin: 8.2.2
- Compile SDK: 34
- Min SDK: 26
- Target SDK: 34

