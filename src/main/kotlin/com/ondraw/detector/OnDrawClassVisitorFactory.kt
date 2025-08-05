package com.ondraw.detector

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import java.io.File
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * OnDraw 类访问器工厂
 * 负责创建 ASM ClassVisitor 来分析每个 class 文件
 */
abstract class OnDrawClassVisitorFactory : AsmClassVisitorFactory<OnDrawClassVisitorFactory.Parameters>, Serializable {
    
    companion object {
        // 使用线程安全的集合来收集所有检测到的问题
        private val allDetectedIssues = ConcurrentHashMap<String, MutableList<OnDrawIssue>>()
        
        /**
         * 创建 ClassVisitor（用于 Transform API）
         */
        fun createClassVisitor(
            nextClassVisitor: ClassVisitor,
            extension: ClassInterceptorExtension,
            variantName: String
        ): ClassVisitor {
            return nextClassVisitor
        }
        
        /**
         * 创建带分析功能的 ClassVisitor（用于 Transform API）
         */
        fun createClassVisitor(
            nextClassVisitor: ClassVisitor,
            className: String,
            extension: ClassInterceptorExtension,
            variantName: String
        ): ClassVisitor {
            // 检查是否应该跳过这个类
            if (shouldSkipClass(className, extension)) {
                return nextClassVisitor
            }
            
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 分析类: $className")
            }
            
            return OnDrawClassAnalyzer(
                nextClassVisitor,
                className,
                extension
            ) { issue ->
                // 收集问题到全局列表中
                addIssue(variantName, issue)
                
                // 如果启用了 verbose，也输出到控制台
                if (extension.verbose.get()) {
                    println("[OnDrawClassInterceptor] 发现问题: ${issue.type} 在 ${issue.location}")
                }
            }
        }
        
        /**
         * 添加检测到的问题
         */
        fun addIssue(variantName: String, issue: OnDrawIssue) {
            allDetectedIssues.computeIfAbsent(variantName) { mutableListOf() }.add(issue)
        }
        
        /**
         * 获取所有检测到的问题
         */
        fun getAllIssues(): Map<String, List<OnDrawIssue>> {
            return allDetectedIssues.mapValues { it.value.toList() }
        }
        
        /**
         * 清空所有问题（用于新的构建）
         */
        fun clearAllIssues() {
            allDetectedIssues.clear()
            reportedVariants.clear()
        }
        
        // 用于跟踪已生成报告的变体，避免重复生成
        private val reportedVariants = mutableSetOf<String>()
        
        /**
         * 生成报告
         */
        fun generateReports(extension: ClassInterceptorExtension, variantName: String, projectDir: File) {
            // 避免重复生成报告
            if (reportedVariants.contains(variantName)) {
                return
            }
            reportedVariants.add(variantName)
            
            val issues = allDetectedIssues[variantName] ?: emptyList()
            
            if (issues.isEmpty()) {
                if (extension.verbose.get()) {
                    println("[OnDrawClassInterceptor] 变体 $variantName: 没有检测到任何问题")
                }
                return
            }
            
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 变体 $variantName: 检测到 ${issues.size} 个问题，开始生成报告...")
            }
            
            val reportGenerator = OnDrawReportGenerator(extension)
            
            // 生成 JSON 报告
            if (extension.generateJsonReport.get()) {
                val reportDir = File(projectDir, extension.reportOutputDir.get())
                val jsonFile = File(reportDir, "ondraw-report-${variantName}.json")
                reportGenerator.generateJsonReport(issues, jsonFile)
            }
            
            // 生成 HTML 报告
            if (extension.generateHtmlReport.get()) {
                val reportDir = File(projectDir, extension.reportOutputDir.get())
                val htmlFile = File(reportDir, "ondraw-report-${variantName}.html")
                reportGenerator.generateHtmlReport(issues, htmlFile)
            }
            
            // 控制台报告（总是生成）
            println("\n=== OnDraw 性能问题报告 (${variantName}) ===")
            issues.groupBy { it.className }.forEach { (className, classIssues) ->
                println("\n类: $className")
                classIssues.forEach { issue ->
                    println("  - ${issue.type}: ${issue.message} (位置: ${issue.location})")
                }
            }
            println("\n总计: ${issues.size} 个问题")
        }
        
        /**
         * 检查是否应该跳过某个类
         */
        private fun shouldSkipClass(className: String, extension: ClassInterceptorExtension): Boolean {
            // 将 ASM 格式的类名（使用斜杠）转换为点分隔格式进行匹配
            val dotClassName = className.replace('/', '.')
            
            // 检查包名排除模式
            extension.excludePackagePatterns.get().forEach { pattern ->
                if (matchesPattern(dotClassName, pattern)) {
                    return true
                }
            }
            
            // 检查类名排除模式
            extension.excludeClassPatterns.get().forEach { pattern ->
                if (matchesPattern(dotClassName, pattern)) {
                    return true
                }
            }
            
            return false
        }
        
        /**
         * 简单的通配符模式匹配
         */
        private fun matchesPattern(text: String, pattern: String): Boolean {
            val regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            return text.matches(Regex(regex))
        }
    }
    
    /**
     * 插件参数接口
     */
    interface Parameters : InstrumentationParameters, Serializable {
        @get:Input
        val extension: Property<ClassInterceptorExtension>
        
        // Logger 不能序列化，在工厂内部获取
        
        @get:Input
        val variantName: Property<String>
    }
    
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val extension = parameters.get().extension.get()
        val className = classContext.currentClassData.className
        
        // 检查是否应该跳过这个类
        if (shouldSkipClass(className, extension)) {
            return nextClassVisitor
        }
        
        if (extension.verbose.get()) {
            println("[OnDrawClassInterceptor] 分析类: $className")
        }
        
        val variantName = parameters.get().variantName.get()
        
        return OnDrawClassAnalyzer(
            nextClassVisitor,
            className,
            extension
        ) { issue ->
            // 收集问题到全局列表中
            addIssue(variantName, issue)
            
            // 如果启用了 verbose，也输出到控制台
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 发现问题: ${issue.type} 在 ${issue.location}")
            }
        }
    }
    
    override fun isInstrumentable(classData: ClassData): Boolean {
        val extension = parameters.get().extension.get()
        val className = classData.className
        
        // 使用统一的过滤逻辑
        if (shouldSkipClass(className, extension)) {
            return false
        }
        
        // 将 ASM 格式的类名转换为点分隔格式进行检查
        val dotClassName = className.replace('/', '.')
        
        // 基本过滤：只处理可能包含 onDraw 方法的类
        val isLikelyToHaveOnDraw = dotClassName.contains("Activity") || 
                                  dotClassName.contains("Fragment") || 
                                  dotClassName.contains("View") ||
                                  dotClassName.contains("Custom")
        
        if (extension.verbose.get()) {
            println("[OnDrawClassInterceptor] 检查是否可检测: $dotClassName -> $isLikelyToHaveOnDraw")
        }
        
        return isLikelyToHaveOnDraw
    }
    

}