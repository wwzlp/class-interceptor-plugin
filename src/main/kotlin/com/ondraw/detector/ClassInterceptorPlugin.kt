package com.ondraw.detector

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * OnDraw 类拦截器插件
 * 在 Android 编译过程中拦截所有 class 文件进行 onDraw 性能检测
 */
class ClassInterceptorPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // 创建扩展配置
        val extension = project.extensions.create(
            "onDrawClassInterceptor", 
            ClassInterceptorExtension::class.java
        )
        
        // 在项目评估后输出插件应用信息（此时可以访问扩展配置）
        project.afterEvaluate {
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 插件开始应用")
            }
        }
        
        // 确保项目应用了 Android 插件
        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByType(AppExtension::class.java)
            android.registerTransform(OnDrawTransform(extension))
        }
        
        project.plugins.withId("com.android.library") {
            val android = project.extensions.getByType(LibraryExtension::class.java)
            android.registerTransform(OnDrawTransform(extension))
        }
    }
    

}