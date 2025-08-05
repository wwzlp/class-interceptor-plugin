plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.ondraw.detector"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:4.2.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("com.google.code.gson:gson:2.8.9")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

gradlePlugin {
    plugins {
        create("onDrawClassInterceptor") {
            id = "com.ondraw.detector.class-interceptor"
            implementationClass = "com.ondraw.detector.ClassInterceptorPlugin"
            displayName = "OnDraw Class Interceptor Plugin"
            description = "Intercepts all class files during Android compilation to detect onDraw performance issues"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("OnDraw Class Interceptor Plugin")
                description.set("A Gradle plugin that intercepts class files during Android compilation to detect onDraw performance issues")
                url.set("https://github.com/your-repo/ondraw-detector")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

/**
 * 创建任务：从.m2本地仓库拷贝完整的Maven仓库结构到本地repo目录
 */
tasks.register<Copy>("copyToLocalRepo") {
    description = "Copy Maven artifacts from .m2 repository to local repo directory"
    group = "publishing"
    
    dependsOn("publishToMavenLocal")
    
    // 从.m2本地仓库拷贝完整的Maven仓库结构
    from("${System.getProperty("user.home")}/.m2/repository/com/ondraw/detector")
    into(layout.projectDirectory.dir("repo/com/ondraw/detector"))
    
    doLast {
        println("[OnDrawClassInterceptor] Maven仓库结构已从.m2拷贝到 repo 目录")
    }
}

/**
 * 让构建任务自动执行拷贝
 */
tasks.named("build") {
    finalizedBy("copyToLocalRepo")
}

tasks.named("publishToMavenLocal") {
    finalizedBy("copyToLocalRepo")
}