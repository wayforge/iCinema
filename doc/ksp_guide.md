# KSP (Kotlin Symbol Processing) 配置指南

## 概述
本项目使用 KSP (Kotlin Symbol Processing) 作为注解处理器，主要用于 Hilt 依赖注入框架的代码生成。

## 配置详情

### 1. 项目级配置 (build.gradle.kts)
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get() apply false
    id("com.google.dagger.hilt.android") version libs.versions.hilt.get() apply false
    id("com.google.devtools.ksp") version libs.versions.ksp.get() apply false  // KSP 插件
}
```

### 2. 模块级配置 (app/build.gradle.kts)
```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")           // 启用 KSP
    id("com.google.dagger.hilt.android")    // Hilt 依赖注入
}

dependencies {
    // Hilt 依赖
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                 // 使用 KSP 处理 Hilt 注解
    implementation(libs.hilt.navigation.compose)
}
```

### 3. 版本管理 (libs.versions.toml)
```toml
[versions]
ksp = "1.9.10-1.0.13"
hilt = "2.48"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
dagger-hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

## KSP vs Annotation Processor (KAPT)

相比传统的 Kotlin 注解处理器(KAPT)，KSP 具有以下优势：
- 更快的处理速度
- 更好的增量编译支持
- 与 Kotlin 编译器更紧密的集成

## 常见用例

目前项目中主要使用 KSP 来处理：
- Hilt 依赖注入注解 (`@HiltAndroidApp`, `@Inject`, `@Module`, 等)
- 生成 Hilt 相关的工厂类和绑定代码

## 故障排除

如果遇到 KSP 相关问题：
1. 清理并重新构建项目: `./gradlew clean && ./gradlew build`
2. 检查 Hilt 注解是否正确使用
3. 确保 KSP 插件版本与 Kotlin 版本兼容