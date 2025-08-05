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
     * 生成 JSON 报告（新版本，支持类分析结果）
     */
    fun generateJsonReportFromClassResults(classResults: List<ClassAnalysisResult>, outputFile: File) {
        try {
            outputFile.parentFile?.mkdirs()
            
            val summary = generateAnalysisSummary(classResults)
            val reportData = mapOf(
                "metadata" to mapOf(
                    "generatedAt" to dateFormat.format(Date()),
                    "totalClasses" to classResults.size,
                    "classesWithDrawMethods" to classResults.count { it.hasDrawMethods },
                    "classesWithIssues" to classResults.count { it.hasIssues },
                    "classesWithoutIssues" to classResults.count { it.hasDrawMethods && !it.hasIssues },
                    "totalIssues" to classResults.sumOf { it.issueCount }
                ),
                "summary" to summary.toJsonMap(),
                "classResults" to classResults.map { it.toJsonMap() }
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
     * 生成 JSON 格式报告（兼容旧版本）
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
     * 生成 HTML 报告（新版本，支持类分析结果）
     */
    fun generateHtmlReportFromClassResults(classResults: List<ClassAnalysisResult>, outputFile: File) {
        try {
            outputFile.parentFile?.mkdirs()
            
            val htmlContent = generateTreeHtmlContent(classResults)
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
     * 生成 HTML 格式报告（兼容旧版本）
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
     * 生成分析摘要
     */
    private fun generateAnalysisSummary(classResults: List<ClassAnalysisResult>): AnalysisSummary {
        val totalClasses = classResults.size
        val classesWithDrawMethods = classResults.count { it.hasDrawMethods }
        val classesWithIssues = classResults.count { it.hasIssues }
        val classesWithoutIssues = classesWithDrawMethods - classesWithIssues
        val totalIssues = classResults.sumOf { it.issueCount }
        
        val allIssues = classResults.flatMap { it.issues }
        val issuesBySeverity = allIssues.groupBy { it.severity }.mapValues { it.value.size }
        val issuesByType = allIssues.groupBy { it.type }.mapValues { it.value.size }
        
        return AnalysisSummary(
            totalClasses = totalClasses,
            classesWithDrawMethods = classesWithDrawMethods,
            classesWithIssues = classesWithIssues,
            classesWithoutIssues = classesWithoutIssues,
            totalIssues = totalIssues,
            issuesBySeverity = issuesBySeverity,
            issuesByType = issuesByType
        )
    }
    
    /**
     * 生成问题摘要（兼容旧版本）
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
     * 生成树形 HTML 报告内容
     */
    private fun generateTreeHtmlContent(classResults: List<ClassAnalysisResult>): String {
        val summary = generateAnalysisSummary(classResults)
        val currentTime = dateFormat.format(Date())
        
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OnDraw 性能分析报告</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
            color: #333;
        }
        .container {
            max-width: 1400px;
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
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
        .classes-section {
            padding: 30px;
        }
        .classes-section h2 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }
        .class-tree {
            margin: 20px 0;
        }
        .class-node {
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
            margin: 10px 0;
            overflow: hidden;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .class-header {
            padding: 15px 20px;
            background: #f8f9fa;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: all 0.3s ease;
        }
        .class-header.clickable {
            cursor: pointer;
        }
        .class-header.clickable:hover {
            background: #e9ecef;
            transform: translateY(-1px);
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .class-header.expanded {
            background: #e3f2fd;
            border-bottom-color: #2196f3;
        }
        .class-info {
            display: flex;
            align-items: center;
            gap: 15px;
        }
        .class-name {
            font-weight: bold;
            font-size: 1.1em;
            color: #333;
        }
        .class-methods {
            color: #666;
            font-size: 0.9em;
        }
        .class-stats {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .severity-badge {
            padding: 3px 8px;
            border-radius: 12px;
            color: white;
            font-size: 0.75em;
            font-weight: bold;
            text-shadow: 0 1px 1px rgba(0,0,0,0.2);
        }
        .severity-badge.severity-critical { background-color: #FF0000; }
        .severity-badge.severity-high { background-color: #FF6600; }
        .severity-badge.severity-medium { background-color: #FFAA00; }
        .severity-badge.severity-low { background-color: #00AA00; }
        .issue-count {
            color: white;
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 0.9em;
            font-weight: bold;
            text-shadow: 0 1px 2px rgba(0,0,0,0.2);
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .issue-count.no-issues {
            background: linear-gradient(135deg, #4caf50, #45a049);
        }
        .issue-count.has-issues {
            background: linear-gradient(135deg, #f44336, #e53935);
        }
        .expand-icon {
            font-size: 1.2em;
            color: #667eea;
            font-weight: bold;
            transition: transform 0.3s ease;
            user-select: none;
        }
        .expand-icon.expanded {
            transform: rotate(90deg);
            color: #2196f3;
        }
        .class-issues {
            display: none;
            padding: 20px;
            background: #fafafa;
        }
        .class-issues.expanded {
            display: block;
        }
        .issue {
            background: white;
            border: 1px solid #ddd;
            border-radius: 6px;
            margin: 10px 0;
            overflow: hidden;
        }
        .issue-header {
            padding: 12px 15px;
            background: #f8f9fa;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .issue-type {
            font-weight: bold;
            font-size: 1em;
        }
        .severity {
            padding: 3px 10px;
            border-radius: 15px;
            color: white;
            font-size: 0.8em;
            font-weight: bold;
        }
        .severity-critical { background-color: #FF0000; }
        .severity-high { background-color: #FF6600; }
        .severity-medium { background-color: #FFAA00; }
        .severity-low { background-color: #00AA00; }
        .issue-body {
            padding: 15px;
        }
        .issue-location {
            color: #666;
            font-family: 'Courier New', monospace;
            background: #f1f1f1;
            padding: 6px 10px;
            border-radius: 4px;
            margin: 8px 0;
            font-size: 0.9em;
        }
        .issue-message {
            margin: 10px 0;
            line-height: 1.5;
        }
        .issue-suggestion {
            background: #e8f5e8;
            border-left: 3px solid #4caf50;
            padding: 12px;
            margin: 10px 0;
            border-radius: 0 4px 4px 0;
            font-size: 0.9em;
        }
        .issue-suggestion strong {
            color: #2e7d32;
        }
        .no-classes {
            text-align: center;
            padding: 60px 20px;
            color: #666;
        }
        .no-classes .icon {
            font-size: 4em;
            color: #4caf50;
            margin-bottom: 20px;
        }
        .footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            border-top: 1px solid #eee;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>OnDraw 性能分析报告</h1>
            <div class="subtitle">生成时间: $currentTime</div>
        </div>
        
        <div class="summary">
            <h2>分析摘要</h2>
            <div class="stats">
                <div class="stat-card">
                    <div class="stat-number">${summary.totalClasses}</div>
                    <div class="stat-label">分析的类总数</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #4caf50;">${summary.classesWithDrawMethods}</div>
                    <div class="stat-label">实现绘制方法的类</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #f44336;">${summary.classesWithIssues}</div>
                    <div class="stat-label">有问题的类</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #4caf50;">${summary.classesWithoutIssues}</div>
                    <div class="stat-label">无问题的类</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" style="color: #ff9800;">${summary.totalIssues}</div>
                    <div class="stat-label">问题总数</div>
                </div>
            </div>
        </div>
        
        <div class="classes-section">
            <h2>类分析详情</h2>
            ${generateClassTreeHtml(classResults)}
        </div>
        
        <div class="footer">
            <p>此报告由 OnDraw 类拦截器插件自动生成</p>
        </div>
    </div>
    
    <script>
        function toggleClass(element) {
            const header = element;
            const issues = header.nextElementSibling;
            const icon = header.querySelector('.expand-icon');
            
            if (issues.classList.contains('expanded')) {
                issues.classList.remove('expanded');
                header.classList.remove('expanded');
                icon.classList.remove('expanded');
            } else {
                issues.classList.add('expanded');
                header.classList.add('expanded');
                icon.classList.add('expanded');
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * 生成 HTML 报告内容（兼容旧版本）
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
     * 生成类树形结构的 HTML
     */
    private fun generateClassTreeHtml(classResults: List<ClassAnalysisResult>): String {
        if (classResults.isEmpty()) {
            return """
                <div class="no-classes">
                    <div class="icon">📋</div>
                    <h3>未找到实现绘制方法的类</h3>
                    <p>在分析的代码中没有发现实现 onDraw 或 dispatchDraw 方法的类。</p>
                </div>
            """.trimIndent()
        }
        
        // 按问题严重程度和数量排序，有问题的类在前，无问题的类在后
        val sortedResults = classResults.sortedWith(
            compareByDescending<ClassAnalysisResult> { it.hasIssues }
                .thenByDescending { result -> 
                    // 按最高严重程度排序
                    result.issues.maxOfOrNull { it.severity.ordinal } ?: -1
                }
                .thenByDescending { it.issueCount }
                .thenBy { it.simpleClassName }
        )
        
        return sortedResults.joinToString("\n") { classResult ->
            val issueCountClass = if (classResult.hasIssues) "has-issues" else "no-issues"
            val issueCountText = if (classResult.hasIssues) "${classResult.issueCount} 个问题" else "无问题"
            val headerClass = if (classResult.hasIssues) "class-header clickable" else "class-header"
            val onClickHandler = if (classResult.hasIssues) "onclick=\"toggleClass(this)\"" else ""
            
            // 获取最高严重程度的问题信息
            val maxSeverityBadge = if (classResult.hasIssues) {
                val maxSeverity = classResult.issues.maxByOrNull { it.severity.ordinal }?.severity
                maxSeverity?.let { severity ->
                    "<div class=\"severity-badge severity-${severity.name.lowercase()}\">${severity.displayName}</div>"
                } ?: ""
            } else ""
            
            """
                <div class="class-node">
                    <div class="$headerClass" $onClickHandler>
                        <div class="class-info">
                            <div class="class-name">${escapeHtml(classResult.simpleClassName)}</div>
                            <div class="class-methods">(${classResult.methodDisplayNames})</div>
                        </div>
                        <div class="class-stats">
                            $maxSeverityBadge
                            <div class="issue-count $issueCountClass">$issueCountText</div>
                            ${if (classResult.hasIssues) "<div class=\"expand-icon\">▶</div>" else ""}
                        </div>
                    </div>
                    ${if (classResult.hasIssues) generateClassIssuesHtml(classResult.issues) else ""}
                </div>
            """.trimIndent()
        }
    }
    
    /**
     * 生成类的问题列表 HTML
     */
    private fun generateClassIssuesHtml(issues: List<OnDrawIssue>): String {
        return """
            <div class="class-issues">
                ${issues.sortedWith(
                    compareByDescending<OnDrawIssue> { it.severity.ordinal }
                        .thenBy { it.lineNumber }
                ).joinToString("\n") { issue ->
                    """
                        <div class="issue">
                            <div class="issue-header">
                                <div class="issue-type">${escapeHtml(issue.displayName)}</div>
                                <div class="severity severity-${issue.severity.name.lowercase()}">${issue.severity.displayName}</div>
                            </div>
                            <div class="issue-body">
                                <div class="issue-location">${escapeHtml(issue.location)}</div>
                                <div class="issue-message">${escapeHtml(issue.message)}</div>
                                <div class="issue-suggestion">
                                    <strong>建议:</strong> ${escapeHtml(issue.suggestion)}
                                </div>
                            </div>
                        </div>
                    """.trimIndent()
                }}
            </div>
        """.trimIndent()
    }
    
    /**
     * 生成问题列表的 HTML（兼容旧版本）
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