package com.ondraw.detector

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.Serializable
import javax.inject.Inject

/**
 * OnDraw 类拦截器插件扩展配置
 */
abstract class ClassInterceptorExtension @Inject constructor() : Serializable {
    
    /**
     * 是否启用详细日志
     */
    abstract val verbose: Property<Boolean>
    
    /**
     * 是否检测对象创建
     */
    abstract val detectObjectCreation: Property<Boolean>
    
    /**
     * 是否检测方法调用
     */
    abstract val detectMethodCalls: Property<Boolean>
    
    /**
     * 是否检测文件IO操作
     */
    abstract val detectFileIO: Property<Boolean>
    
    /**
     * 是否检测网络操作
     */
    abstract val detectNetworkOps: Property<Boolean>
    
    /**
     * 是否检测耗时操作
     */
    abstract val detectTimeConsumingOps: Property<Boolean>
    
    /**
     * 自定义检测的方法调用模式
     */
    abstract val customMethodPatterns: SetProperty<String>
    
    /**
     * 排除的类名模式
     */
    abstract val excludeClassPatterns: SetProperty<String>
    
    /**
     * 排除的包名模式
     */
    abstract val excludePackagePatterns: SetProperty<String>
    
    /**
     * 报告输出目录
     */
    abstract val reportOutputDir: Property<String>
    
    /**
     * 是否生成JSON报告
     */
    abstract val generateJsonReport: Property<Boolean>
    
    /**
     * 是否生成HTML报告
     */
    abstract val generateHtmlReport: Property<Boolean>
    
    init {
        // 设置默认值
        verbose.convention(true)
        detectObjectCreation.convention(true)
        detectMethodCalls.convention(true)
        detectFileIO.convention(true)
        detectNetworkOps.convention(true)
        detectTimeConsumingOps.convention(true)
        generateJsonReport.convention(true)
        generateHtmlReport.convention(true)
        reportOutputDir.convention("build/reports/ondraw-detector")
        
        // 默认排除的包
        excludePackagePatterns.convention(setOf(
            "android.**",
            "androidx.**",
            "java.**",
            "javax.**",
            "kotlin.**",
            "kotlinx.**",
            "org.jetbrains.**",
            "com.google.**",
            "com.android.**"
        ))
        
        // 默认排除的类
        excludeClassPatterns.convention(setOf(
            "**/*\$\$serializer",
            "**/*\$Companion",
            "**/*\$WhenMappings",
            "**/BuildConfig",
            "**/R\$*",
            "**/R",
            "R\$*",
            "R",
            "*.R",
            "*.R\$*"
        ))
    }
}