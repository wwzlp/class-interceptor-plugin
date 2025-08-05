# OnDraw Class Interceptor Plugin 使用说明

## 概述

OnDraw Class Interceptor Plugin 是一个 Gradle 插件，用于在 Android 编译过程中拦截所有类文件，检测 onDraw 性能问题。

## 系统要求

- **Java 版本**: Java 11 或更高版本
- **Gradle 版本**: 6.7.1 或更高版本
- **Android Gradle Plugin**: 7.0.4（插件内置）
- **Kotlin**: 1.6.21（插件内置）

## 版本兼容性

本插件已针对不同的 Gradle 版本进行了优化：

- **Gradle 6.7.1+**: 使用 Android Gradle Plugin 7.0.4 + Kotlin 1.6.21
- **Gradle 7.0+**: 完全兼容，推荐使用
- **Gradle 8.0+**: 完全兼容，推荐使用

**注意**: 如果你的项目使用较老的 Gradle 版本（如 6.7.1），插件会自动使用兼容的依赖版本。

## 安装和配置

### 1. 配置本地 Maven 仓库

在你的 Android 项目根目录的 `build.gradle` 或 `build.gradle.kts` 文件中添加本地 Maven 仓库：

#### Kotlin DSL (build.gradle.kts)
```kotlin
allprojects {
    repositories {
        // 添加本地 Maven 仓库
        maven {
            url = uri("path/to/class-interceptor-plugin/repo")
        }
        google()
        mavenCentral()
    }
}
```

#### Groovy DSL (build.gradle)
```groovy
allprojects {
    repositories {
        // 添加本地 Maven 仓库
        maven {
            url 'path/to/class-interceptor-plugin/repo'
        }
        google()
        mavenCentral()
    }
}
```

**注意**: 请将 `path/to/class-interceptor-plugin/repo` 替换为实际的插件 repo 目录路径。

### 2. 添加插件依赖

在项目根目录的 `build.gradle` 或 `build.gradle.kts` 文件中添加插件依赖：

#### Kotlin DSL (build.gradle.kts)
```kotlin
buildscript {
    dependencies {
        classpath("com.ondraw.detector:class-interceptor-plugin:1.0.0")
    }
}
```

#### Groovy DSL (build.gradle)
```groovy
buildscript {
    dependencies {
        classpath 'com.ondraw.detector:class-interceptor-plugin:1.0.0'
    }
}
```

### 3. 应用插件

在你的 app 模块的 `build.gradle` 或 `build.gradle.kts` 文件中应用插件：

#### Kotlin DSL (app/build.gradle.kts)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.ondraw.detector.class-interceptor") // 添加这一行
}
```

#### Groovy DSL (app/build.gradle)
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.ondraw.detector.class-interceptor' // 添加这一行
}
```

### 4. 配置插件（可选）

你可以在 app 模块的 `build.gradle` 文件中配置插件行为：

#### Kotlin DSL
```kotlin
onDrawClassInterceptor {
    // 启用或禁用插件
    enabled = true
    
    // 配置输出目录
    outputDir = "${project.buildDir}/ondraw-reports"
    
    // 配置要检测的类名模式
    includePatterns = listOf("**/*View.class", "**/*Activity.class")
    
    // 配置要排除的类名模式
    excludePatterns = listOf("**/R.class", "**/BuildConfig.class")
}
```

#### Groovy DSL
```groovy
onDrawClassInterceptor {
    // 启用或禁用插件
    enabled true
    
    // 配置输出目录
    outputDir "${project.buildDir}/ondraw-reports"
    
    // 配置要检测的类名模式
    includePatterns = ['**/*View.class', '**/*Activity.class']
    
    // 配置要排除的类名模式
    excludePatterns = ['**/R.class', '**/BuildConfig.class']
}
```

## 使用方法

1. 配置完成后，正常编译你的 Android 项目：
   ```bash
   ./gradlew assembleDebug
   ```

2. 插件会在编译过程中自动拦截类文件并检测 onDraw 相关的性能问题。

3. 检测结果会输出到控制台和配置的输出目录中。

## 常见问题解决方案

### 1. Java 版本兼容性问题

**错误信息**: `Unsupported class file major version 61`

**解决方案**:
- 确保你的项目使用 Java 11 或更高版本
- 检查 `JAVA_HOME` 环境变量是否指向正确的 Java 版本
- 在项目的 `gradle.properties` 文件中添加：
  ```properties
  org.gradle.java.home=/path/to/java11
  ```

### 2. 插件未找到错误

**错误信息**: `Plugin [id: 'com.ondraw.detector.class-interceptor'] was not found`

**解决方案**:
- 确保本地 Maven 仓库路径配置正确
- 确保插件依赖已正确添加到 buildscript 中
- 检查插件版本号是否正确

### 3. 构建缓存问题

**解决方案**:
- 清理项目缓存：
  ```bash
  ./gradlew clean
  ```
- 清理 Gradle 缓存：
  ```bash
  rm -rf ~/.gradle/caches
  ```

### 4. 权限问题

**错误信息**: `Permission denied`

**解决方案**:
- 确保对插件 repo 目录有读取权限
- 在 macOS/Linux 上，可能需要调整文件权限：
  ```bash
  chmod -R 755 /path/to/class-interceptor-plugin/repo
  ```

### 5. Gradle 版本兼容性

**错误信息**: `The current Gradle version X.X.X is not compatible with the Kotlin Gradle plugin`

**解决方案**:
- 本插件支持 Gradle 6.7.1 及以上版本
- 对于 Gradle 6.7.1 项目，插件使用兼容的依赖版本：
  - Android Gradle Plugin: 7.0.4
  - Kotlin Gradle Plugin: 1.6.21
- 在 `gradle/wrapper/gradle-wrapper.properties` 中检查 Gradle 版本：
  ```properties
  # Gradle 6.7.1 示例
  distributionUrl=https\://services.gradle.org/distributions/gradle-6.7.1-bin.zip
  
  # Gradle 7.6 示例（推荐）
  distributionUrl=https\://services.gradle.org/distributions/gradle-7.6-bin.zip
  ```

### 6. Kotlin 版本冲突

**错误信息**: `Failed to apply plugin 'kotlin-android'`

**解决方案**:
- 确保项目中的 Kotlin 版本与插件兼容
- 对于 Gradle 6.7.1 项目，建议使用 Kotlin 1.6.21
- 在项目根目录的 `build.gradle.kts` 中指定 Kotlin 版本：
  ```kotlin
  buildscript {
      dependencies {
          classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
      }
  }
  ```

## 版本历史

### v1.0.0
- 初始版本
- 支持基本的 onDraw 性能检测
- 兼容 Java 11 和 Android Gradle Plugin 7.4.2

## 技术支持

如果遇到其他问题，请检查：

1. **Java 版本**: 使用 `java -version` 检查当前 Java 版本
2. **Gradle 版本**: 使用 `./gradlew --version` 检查 Gradle 版本
3. **插件文件**: 确保 `repo` 目录中包含所有必要的文件
4. **日志信息**: 使用 `./gradlew assembleDebug --info` 获取详细日志

## 示例项目结构

```
your-android-project/
├── app/
│   ├── build.gradle.kts (应用插件)
│   └── src/
├── build.gradle.kts (配置仓库和依赖)
├── settings.gradle.kts
└── class-interceptor-plugin/
    └── repo/ (本地 Maven 仓库)
        └── com/ondraw/detector/
            ├── class-interceptor-plugin/
            └── class-interceptor/
```

通过遵循以上步骤，你应该能够成功在你的 Android 项目中集成和使用 OnDraw Class Interceptor Plugin。