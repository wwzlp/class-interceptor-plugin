package com.ondraw.detector

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * OnDraw 检测报告生成器
 * 支持生成 JSON 和 HTML 格式的报告
 */
class OnDrawReportGenerator(
    private val extension: ClassInterceptorExtension
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * 生成 JSON 格式报告
     */
    fun generateJsonReport(issues: List<OnDrawIssue>, outputFile: File) {
        try {
            outputFile.parentFile?.mkdirs()
            
            val reportData = mapOf(
                "metadata" to mapOf(
                    "generatedAt" to dateFormat.format(Date()),
                    "totalIssues" to issues.size,
                    "criticalIssues" to issues.count { it.severity == IssueSeverity.CRITICAL },
                    "highIssues" to issues.count { it.severity == IssueSeverity.HIGH },
                    "mediumIssues" to issues.count { it.severity == IssueSeverity.MEDIUM },
                    "lowIssues" to issues.count { it.severity == IssueSeverity.LOW }
                ),
                "summary" to generateSummary(issues),
                "issues" to issues.map { it.toJsonMap() }
            )
            
            val jsonContent = formatJson(reportData)
            outputFile.writeText(jsonContent)
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] JSON 报告已生成: ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 生成 JSON 报告失败: ${e.message}")
            }
        }
    }
    
    /**
     * 生成 HTML 格式报告
     */
    fun generateHtmlReport(issues: List<OnDrawIssue>, outputFile: File) {
        try {
            outputFile.parentFile?.mkdirs()
            
            val htmlContent = generateHtmlContent(issues)
            outputFile.writeText(htmlContent)
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] HTML 报告已生成: ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 生成 HTML 报告失败: ${e.message}")
            }
        }
    }
    
    /**
     * 生成问题摘要
     */
    private fun generateSummary(issues: List<OnDrawIssue>): Map<String, Any> {
        val groupedByType = issues.groupBy { it.type }
        val groupedByClass = issues.groupBy { it.className }
        val groupedBySeverity = issues.groupBy { it.severity }
        
        return mapOf(
            "byType" to groupedByType.entries.associate { it.key.name to it.value.size },
            "byClass" to groupedByClass.mapValues { it.value.size },
            "bySeverity" to groupedBySeverity.entries.associate { it.key.name to it.value.size },
            "topProblematicClasses" to groupedByClass.entries
                .sortedByDescending { it.value.size }
                .take(10)
                .associate { it.key to it.value.size },
            "mostCommonIssues" to groupedByType.entries
                .sortedByDescending { it.value.size }
                .take(5)
                .associate { it.key.displayName to it.value.size }
        )
    }
    
    /**
     * 格式化 JSON 输出
     */
    private fun formatJson(data: Map<String, Any>, indent: Int = 0): String {
        val indentStr = "  ".repeat(indent)
        val nextIndentStr = "  ".repeat(indent + 1)
        
        return when {
            data.isEmpty() -> "{}"
            else -> {
                val entries = data.entries.joinToString(",\n") { (key, value) ->
                    val formattedValue = when (value) {
                        is Map<*, *> -> formatJson(value as Map<String, Any>, indent + 1)
                        is List<*> -> formatJsonArray(value, indent + 1)
                        is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                        is Number -> value.toString()
                        is Boolean -> value.toString()
                        else -> "\"$value\""
                    }
                    "$nextIndentStr\"$key\": $formattedValue"
                }
                "{\n$entries\n$indentStr}"
            }
        }
    }
    
    /**
     * 格式化 JSON 数组
     */
    private fun formatJsonArray(list: List<*>, indent: Int): String {
        val indentStr = "  ".repeat(indent)
        val nextIndentStr = "  ".repeat(indent + 1)
        
        return when {
            list.isEmpty() -> "[]"
            else -> {
                val items = list.joinToString(",\n") { item ->
                    val formattedItem = when (item) {
                        is Map<*, *> -> formatJson(item as Map<String, Any>, indent + 1)
                        is String -> "\"${item.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                        is Number -> item.toString()
                        is Boolean -> item.toString()
                        else -> "\"$item\""
                    }
                    "$nextIndentStr$formattedItem"
                }
                "[\n$items\n$indentStr]"
            }
        }
    }
    
    /**
     * 生成 HTML 报告内容
     */
    private fun generateHtmlContent(issues: List<OnDrawIssue>): String {
        val summary = generateSummary(issues)
        val currentTime = dateFormat.format(Date())
        
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OnDraw 性能检测报告</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
            color: #333;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 2.5em;
            font-weight: 300;
        }
        .header .subtitle {
            margin-top: 10px;
            opacity: 0.9;
            font-size: 1.1em;
        }
        .summary {
            padding: 30px;
            border-bottom: 1px solid #eee;
        }
        .summary h2 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }
        .stat-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            border-left: 4px solid #667eea;
        }
        .stat-number {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
        }
        .stat-label {
            color: #666;
            margin-top: 5px;
        }
        .issues {
            padding: 30px;
        }
        .issues h2 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }
        .issue {
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
            margin: 15px 0;
            overflow: hidden;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .issue-header {
            padding: 15px 20px;
            background: #f8f9fa;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .issue-type {
            font-weight: bold;
            font-size: 1.1em;
        }
        .severity {
            padding: 4px 12px;
            border-radius: 20px;
            color: white;
            font-size: 0.9em;
            font-weight: bold;
        }
        .severity-critical { background-color: #FF0000; }
        .severity-high { background-color: #FF6600; }
        .severity-medium { background-color: #FFAA00; }
        .severity-low { background-color: #00AA00; }
        .issue-body {
            padding: 20px;
        }
        .issue-location {
            color: #666;
            font-family: 'Courier New', monospace;
            background: #f1f1f1;
            padding: 8px 12px;
            border-radius: 4px;
            margin: 10px 0;
        }
        .issue-message {
            margin: 15px 0;
            line-height: 1.6;
        }
        .issue-suggestion {
            background: #e8f5e8;
            border-left: 4px solid #4caf50;
            padding: 15px;
            margin: 15px 0;
            border-radius: 0 4px 4px 0;
        }
        .issue-suggestion strong {
            color: #2e7d32;
        }
        .footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            border-top: 1px solid #eee;
        }
        .no-issues {
            text-align: center;
            padding: 60px 20px;
            color: #666;
        }
        .no-issues .icon {
            font-size: 4em;
            color: #4caf50;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>OnDraw 性能检测报告</h1>
            <div class="subtitle">生成时间: $currentTime</div>
        </div>
        
        <div class="summary">
            <h2>检测摘要</h2>
            <div class="stats">
                <div class="stat-card">
                    <div class="stat-number">${issues.size}</div>
                    <div class="stat-label">总问题数</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #FF0000;">${issues.count { it.severity == IssueSeverity.CRITICAL }}</div>
                    <div class="stat-label">严重问题</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #FF6600;">${issues.count { it.severity == IssueSeverity.HIGH }}</div>
                    <div class="stat-label">高危问题</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #FFAA00;">${issues.count { it.severity == IssueSeverity.MEDIUM }}</div>
                    <div class="stat-label">中等问题</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #00AA00;">${issues.count { it.severity == IssueSeverity.LOW }}</div>
                    <div class="stat-label">低危问题</div>
                </div>
            </div>
        </div>
        
        <div class="issues">
            <h2>详细问题列表</h2>
            ${generateIssuesHtml(issues)}
        </div>
        
        <div class="footer">
            <p>此报告由 OnDraw 类拦截器插件自动生成</p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * 生成问题列表的 HTML
     */
    private fun generateIssuesHtml(issues: List<OnDrawIssue>): String {
        if (issues.isEmpty()) {
            return """
                <div class="no-issues">
                    <div class="icon">✅</div>
                    <h3>恭喜！未发现 onDraw 性能问题</h3>
                    <p>您的代码在 onDraw 方法中没有检测到明显的性能问题。</p>
                </div>
            """.trimIndent()
        }
        
        return issues.sortedWith(
            compareByDescending<OnDrawIssue> { it.severity.ordinal }
                .thenBy { it.className }
                .thenBy { it.lineNumber }
        ).joinToString("\n") { issue ->
            """
                <div class="issue">
                    <div class="issue-header">
                        <div class="issue-type">${issue.displayName}</div>
                        <div class="severity severity-${issue.severity.name.lowercase()}">${issue.severity.displayName}</div>
                    </div>
                    <div class="issue-body">
                        <div class="issue-location">${issue.location}</div>
                        <div class="issue-message">${escapeHtml(issue.message)}</div>
                        <div class="issue-suggestion">
                            <strong>建议:</strong> ${escapeHtml(issue.suggestion)}
                        </div>
                    </div>
                </div>
            """.trimIndent()
        }
    }
    
    /**
     * HTML 转义
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}