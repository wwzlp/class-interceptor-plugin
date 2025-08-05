package com.ondraw.detector

import java.io.Serializable

/**
 * OnDraw 性能问题类型枚举
 */
enum class OnDrawIssueType(val displayName: String, val severity: IssueSeverity) {
    OBJECT_CREATION("对象创建", IssueSeverity.HIGH),
    STRING_OPERATION("字符串操作", IssueSeverity.MEDIUM),
    COLLECTION_OPERATION("集合操作", IssueSeverity.MEDIUM),
    FILE_IO("文件I/O操作", IssueSeverity.CRITICAL),
    NETWORK_OPERATION("网络操作", IssueSeverity.CRITICAL),
    DATABASE_OPERATION("数据库操作", IssueSeverity.CRITICAL),
    BITMAP_DECODE("图片解码", IssueSeverity.HIGH),
    COMPLEX_CALCULATION("复杂计算", IssueSeverity.MEDIUM),
    REFLECTION("反射调用", IssueSeverity.HIGH),
    SYSTEM_CALL("系统调用", IssueSeverity.LOW),
    CUSTOM_PATTERN("自定义模式", IssueSeverity.MEDIUM)
}

/**
 * 问题严重程度
 */
enum class IssueSeverity(val displayName: String, val color: String) {
    CRITICAL("严重", "#FF0000"),
    HIGH("高", "#FF6600"),
    MEDIUM("中", "#FFAA00"),
    LOW("低", "#00AA00")
}

/**
 * OnDraw 性能问题数据类
 */
data class OnDrawIssue(
    val type: OnDrawIssueType,
    val className: String,
    val methodName: String,
    val lineNumber: Int,
    val message: String,
    val suggestion: String,
    val location: String
) : Serializable {
    /**
     * 获取问题的严重程度
     */
    val severity: IssueSeverity
        get() = type.severity
    
    /**
     * 获取问题的显示名称
     */
    val displayName: String
        get() = type.displayName
    
    /**
     * 获取简短的类名（不包含包名）
     */
    val simpleClassName: String
        get() = className.substringAfterLast('.')
    
    /**
     * 获取完整的方法签名
     */
    val fullMethodName: String
        get() = "$className.$methodName"
    
    /**
     * 转换为 JSON 格式的 Map
     */
    fun toJsonMap(): Map<String, Any> {
        return mapOf(
            "type" to type.name,
            "displayName" to displayName,
            "severity" to severity.name,
            "severityDisplay" to severity.displayName,
            "className" to className,
            "simpleClassName" to simpleClassName,
            "methodName" to methodName,
            "fullMethodName" to fullMethodName,
            "lineNumber" to lineNumber,
            "message" to message,
            "suggestion" to suggestion,
            "location" to location
        )
    }
    
    /**
     * 转换为控制台输出格式
     */
    fun toConsoleString(): String {
        return "[${severity.displayName}] $displayName: $message\n" +
               "  位置: $location\n" +
               "  建议: $suggestion"
    }
    
    override fun toString(): String {
        return "OnDrawIssue(type=$type, location=$location, message='$message')"
    }
}