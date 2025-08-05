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
        
        // 使用线程安全的集合来收集所有类分析结果
        private val allClassResults = ConcurrentHashMap<String, MutableList<ClassAnalysisResult>>()
        
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
                extension,
                { issue ->
                    // 收集问题到全局列表中
                    addIssue(variantName, issue)
                    
                    // 如果启用了 verbose，也输出到控制台
                    if (extension.verbose.get()) {
                        println("[OnDrawClassInterceptor] 发现问题: ${issue.type} 在 ${issue.location}")
                    }
                },
                { classResult ->
                    // 收集类分析结果
                    addClassResult(variantName, classResult)
                }
            )
        }
        
        /**
         * 添加检测到的问题
         */
        fun addIssue(variantName: String, issue: OnDrawIssue) {
            allDetectedIssues.computeIfAbsent(variantName) { mutableListOf() }.add(issue)
        }
        
        /**
         * 添加类分析结果（带去重逻辑）
         */
        fun addClassResult(variantName: String, classResult: ClassAnalysisResult) {
            val resultsList = allClassResults.computeIfAbsent(variantName) { mutableListOf() }
            
            // 检查是否已经存在相同类名的结果，避免重复添加
            val existingResult = resultsList.find { it.className == classResult.className }
            if (existingResult == null) {
                resultsList.add(classResult)
            } else {
                // 如果类已存在，合并问题列表（避免丢失问题）
                val mergedIssues = (existingResult.issues + classResult.issues).distinctBy { "${it.type}-${it.location}-${it.message}" }
                val mergedMethods = existingResult.implementedMethods + classResult.implementedMethods
                
                // 创建合并后的结果
                val mergedResult = ClassAnalysisResult(
                    className = classResult.className,
                    implementedMethods = mergedMethods,
                    issues = mergedIssues
                )
                
                // 替换原有结果
                val index = resultsList.indexOf(existingResult)
                resultsList[index] = mergedResult
            }
        }
        
        /**
         * 获取所有检测到的问题
         */
        fun getAllIssues(): Map<String, List<OnDrawIssue>> {
            return allDetectedIssues.mapValues { it.value.toList() }
        }
        
        /**
         * 获取所有类分析结果
         */
        fun getAllClassResults(): Map<String, List<ClassAnalysisResult>> {
            return allClassResults.mapValues { it.value.toList() }
        }
        
        /**
         * 清空所有问题（用于新的构建）
         */
        fun clearAllIssues() {
            allDetectedIssues.clear()
            allClassResults.clear()
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
            val classResults = allClassResults[variantName] ?: emptyList()
            
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 变体 $variantName: 分析了 ${classResults.size} 个类，检测到 ${issues.size} 个问题，开始生成报告...")
            }
            
            val reportGenerator = OnDrawReportGenerator(extension)
            
            // 生成 JSON 报告
            if (extension.generateJsonReport.get()) {
                val reportDir = File(projectDir, extension.reportOutputDir.get())
                val jsonFile = File(reportDir, "ondraw-report-${variantName}.json")
                reportGenerator.generateJsonReportFromClassResults(classResults, jsonFile)
            }
            
            // 生成 HTML 报告
            if (extension.generateHtmlReport.get()) {
                val reportDir = File(projectDir, extension.reportOutputDir.get())
                val htmlFile = File(reportDir, "ondraw-report-${variantName}.html")
                reportGenerator.generateHtmlReportFromClassResults(classResults, htmlFile)
            }
            
            // 控制台报告（总是生成）
            generateConsoleReport(variantName, classResults)
        }
        
        /**
         * 生成控制台报告
         */
        private fun generateConsoleReport(variantName: String, classResults: List<ClassAnalysisResult>) {
            println("\n=== OnDraw 性能分析报告 (${variantName}) ===")
            
            val classesWithIssues = classResults.filter { it.hasIssues }
            val classesWithoutIssues = classResults.filter { !it.hasIssues }
            val totalIssues = classResults.sumOf { it.issueCount }
            
            println("\n📊 统计信息:")
            println("  - 分析的类总数: ${classResults.size}")
            println("  - 有问题的类: ${classesWithIssues.size}")
            println("  - 无问题的类: ${classesWithoutIssues.size}")
            println("  - 问题总数: $totalIssues")
            
            if (classesWithIssues.isNotEmpty()) {
                println("\n🚨 发现问题的类:")
                classesWithIssues.sortedByDescending { it.issueCount }.forEach { classResult ->
                    println("\n类: ${classResult.className} (${classResult.methodDisplayNames})")
                    println("  问题数量: ${classResult.issueCount}")
                    classResult.issues.forEach { issue ->
                        println("    - ${issue.type}: ${issue.message} (位置: ${issue.location})")
                    }
                }
            }
            
            if (classesWithoutIssues.isNotEmpty()) {
                println("\n✅ 无问题的类:")
                classesWithoutIssues.forEach { classResult ->
                    println("  - ${classResult.className} (${classResult.methodDisplayNames})")
                }
            }
            
            println("\n总计: 分析了 ${classResults.size} 个类，发现 $totalIssues 个问题")
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
            extension,
            { issue ->
                // 收集问题到全局列表中
                addIssue(variantName, issue)
                
                // 如果启用了 verbose，也输出到控制台
                if (extension.verbose.get()) {
                    println("[OnDrawClassInterceptor] 发现问题: ${issue.type} 在 ${issue.location}")
                }
            },
            { classResult ->
                // 收集类分析结果
                addClassResult(variantName, classResult)
            }
        )
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