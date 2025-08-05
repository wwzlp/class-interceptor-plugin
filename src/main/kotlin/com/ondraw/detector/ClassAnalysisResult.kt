package com.ondraw.detector

import java.io.Serializable

/**
 * 绘制方法类型枚举
 */
enum class DrawMethodType(val methodName: String, val displayName: String) {
    ON_DRAW("onDraw", "onDraw"),
    DISPATCH_DRAW("dispatchDraw", "dispatchDraw")
}

/**
 * 类分析结果数据类
 * 记录每个类的分析结果，包括发现的问题和实现的绘制方法
 */
data class ClassAnalysisResult(
    val className: String,
    val implementedMethods: Set<DrawMethodType>,
    val issues: List<OnDrawIssue>
) : Serializable {
    
    /**
     * 获取简短的类名（不包含包名）
     */
    val simpleClassName: String
        get() {
            // 移除 .class 后缀
            val nameWithoutSuffix = if (className.endsWith(".class")) {
                className.substringBeforeLast(".class")
            } else {
                className
            }
            // 提取最后一个包分隔符后的类名
            return nameWithoutSuffix.substringAfterLast('.')
        }
    
    /**
     * 获取问题数量
     */
    val issueCount: Int
        get() = issues.size
    
    /**
     * 是否有问题
     */
    val hasIssues: Boolean
        get() = issues.isNotEmpty()
    
    /**
     * 是否实现了绘制方法
     */
    val hasDrawMethods: Boolean
        get() = implementedMethods.isNotEmpty()
    
    /**
     * 获取实现的方法名称列表
     */
    val methodNames: List<String>
        get() = implementedMethods.map { it.methodName }
    
    /**
     * 获取实现的方法显示名称
     */
    val methodDisplayNames: String
        get() = implementedMethods.joinToString(", ") { it.displayName }
    
    /**
     * 按严重程度分组的问题
     */
    val issuesBySeverity: Map<IssueSeverity, List<OnDrawIssue>>
        get() = issues.groupBy { it.severity }
    
    /**
     * 按问题类型分组的问题
     */
    val issuesByType: Map<OnDrawIssueType, List<OnDrawIssue>>
        get() = issues.groupBy { it.type }
    
    /**
     * 转换为 JSON 格式的 Map
     */
    fun toJsonMap(): Map<String, Any> {
        return mapOf(
            "className" to className,
            "simpleClassName" to simpleClassName,
            "implementedMethods" to implementedMethods.map { it.methodName },
            "methodDisplayNames" to methodDisplayNames,
            "issueCount" to issueCount,
            "hasIssues" to hasIssues,
            "hasDrawMethods" to hasDrawMethods,
            "issues" to issues.map { it.toJsonMap() },
            "issuesBySeverity" to issuesBySeverity.mapKeys { it.key.name }.mapValues { entry ->
                entry.value.map { it.toJsonMap() }
            }
        )
    }
    
    /**
     * 获取状态描述
     */
    fun getStatusDescription(): String {
        return when {
            !hasDrawMethods -> "未实现绘制方法"
            !hasIssues -> "实现了绘制方法，无性能问题"
            else -> "发现 $issueCount 个性能问题"
        }
    }
    
    override fun toString(): String {
        return "ClassAnalysisResult(className='$className', methods=$methodDisplayNames, issues=${issues.size})"
    }
}

/**
 * 分析结果汇总
 */
data class AnalysisSummary(
    val totalClasses: Int,
    val classesWithDrawMethods: Int,
    val classesWithIssues: Int,
    val classesWithoutIssues: Int,
    val totalIssues: Int,
    val issuesBySeverity: Map<IssueSeverity, Int>,
    val issuesByType: Map<OnDrawIssueType, Int>
) : Serializable {
    
    /**
     * 转换为 JSON 格式的 Map
     */
    fun toJsonMap(): Map<String, Any> {
        return mapOf(
            "totalClasses" to totalClasses,
            "classesWithDrawMethods" to classesWithDrawMethods,
            "classesWithIssues" to classesWithIssues,
            "classesWithoutIssues" to classesWithoutIssues,
            "totalIssues" to totalIssues,
            "issuesBySeverity" to issuesBySeverity.mapKeys { it.key.name },
            "issuesByType" to issuesByType.mapKeys { it.key.name }
        )
    }
}