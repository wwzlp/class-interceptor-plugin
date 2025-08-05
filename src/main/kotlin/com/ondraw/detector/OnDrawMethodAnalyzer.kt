package com.ondraw.detector

import org.objectweb.asm.*

/**
 * 绘制方法分析器
 * 分析 onDraw 和 dispatchDraw 方法内部的指令，检测性能问题
 */
class OnDrawMethodAnalyzer(
    nextVisitor: MethodVisitor?,
    private val className: String,
    private val methodName: String,
    private val extension: ClassInterceptorExtension,
    private val onIssueFound: (OnDrawIssue) -> Unit
) : MethodVisitor(Opcodes.ASM7, nextVisitor) {
    
    private var lineNumber = 0
    private val detectedIssues = mutableListOf<OnDrawIssue>()
    
    override fun visitLineNumber(line: Int, start: Label?) {
        lineNumber = line
        super.visitLineNumber(line, start)
    }
    
    override fun visitTypeInsn(opcode: Int, type: String) {
        if (extension.detectObjectCreation.get() && opcode == Opcodes.NEW) {
            // 检测对象创建
            if (isProblematicObjectCreation(type)) {
                reportIssue(
                OnDrawIssueType.OBJECT_CREATION,
                "在 $methodName 中创建对象: $type",
                "避免在绘制方法中创建新对象，考虑对象复用或预创建"
            )
            }
        }
        super.visitTypeInsn(opcode, type)
    }
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        // 检测方法调用
        if (extension.detectMethodCalls.get()) {
            checkProblematicMethodCall(owner, name, descriptor)
        }
        
        // 检测文件 I/O 操作
        if (extension.detectFileIO.get()) {
            checkFileIOOperation(owner, name)
        }
        
        // 检测网络操作
        if (extension.detectNetworkOps.get()) {
            checkNetworkOperation(owner, name)
        }
        
        // 检测耗时操作
        if (extension.detectTimeConsumingOps.get()) {
            checkExpensiveOperation(owner, name)
        }
        
        // 检测自定义方法模式
        checkCustomMethodPatterns(owner, name)
        
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        // 检测字段访问模式
        if (extension.detectTimeConsumingOps.get()) {
            checkFieldAccess(opcode, owner, name, descriptor)
        }
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }
    
    override fun visitEnd() {
        // 方法分析结束，报告所有发现的问题
        detectedIssues.forEach { issue ->
            onIssueFound(issue)
            // 根据 verbose 配置决定是否输出详细信息
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] ${issue.type}: ${issue.message} 在 ${issue.location}")
            }
        }
        super.visitEnd()
    }
    
    /**
     * 检查是否是有问题的对象创建
     */
    private fun isProblematicObjectCreation(type: String): Boolean {
        val problematicTypes = setOf(
            "java/lang/String",
            "java/lang/StringBuilder",
            "java/lang/StringBuffer",
            "java/util/ArrayList",
            "java/util/HashMap",
            "java/util/HashSet",
            "android/graphics/Paint",
            "android/graphics/Path",
            "android/graphics/Rect",
            "android/graphics/RectF",
            "android/graphics/Matrix",
            "android/graphics/Bitmap",
            "android/graphics/Canvas",
            "java/io/File",
            "java/io/FileInputStream",
            "java/io/FileOutputStream"
        )
        
        return problematicTypes.contains(type) || 
               type.startsWith("java/util/") ||
               type.startsWith("java/io/") ||
               type.startsWith("java/net/")
    }
    
    /**
     * 检查有问题的方法调用
     */
    private fun checkProblematicMethodCall(owner: String, name: String, descriptor: String) {
        // 检查字符串操作
        if (owner == "java/lang/String" && (name == "concat" || name == "valueOf" || name == "format")) {
            reportIssue(
                OnDrawIssueType.STRING_OPERATION,
                "在 $methodName 中进行字符串操作: $owner.$name",
                "避免在绘制方法中进行字符串拼接，考虑预先准备字符串"
            )
        }
        
        // 检查集合操作
        if (owner.startsWith("java/util/") && (name == "add" || name == "remove" || name == "clear" || name == "put")) {
            reportIssue(
                OnDrawIssueType.COLLECTION_OPERATION,
                "在 $methodName 中进行集合操作: $owner.$name",
                "避免在绘制方法中修改集合，考虑在其他地方预处理数据"
            )
        }
        
        // 检查反射调用
        if (owner.startsWith("java/lang/reflect/")) {
            reportIssue(
                OnDrawIssueType.REFLECTION,
                "在 $methodName 中使用反射: $owner.$name",
                "反射调用性能开销大，避免在绘制方法中使用"
            )
        }
    }
    
    /**
     * 检查文件 I/O 操作
     */
    private fun checkFileIOOperation(owner: String, name: String) {
        val ioClasses = setOf(
            "java/io/File",
            "java/io/FileInputStream",
            "java/io/FileOutputStream",
            "java/io/BufferedReader",
            "java/io/BufferedWriter",
            "java/nio/file/Files"
        )
        
        if (ioClasses.any { owner.startsWith(it) }) {
            reportIssue(
                OnDrawIssueType.FILE_IO,
                "在 $methodName 中进行文件 I/O 操作: $owner.$name",
                "文件 I/O 操作会阻塞 UI 线程，应该在后台线程中进行"
            )
        }
    }
    
    /**
     * 检查网络操作
     */
    private fun checkNetworkOperation(owner: String, name: String) {
        val networkClasses = setOf(
            "java/net/URL",
            "java/net/URLConnection",
            "java/net/HttpURLConnection",
            "okhttp3/",
            "retrofit2/",
            "com/android/volley/"
        )
        
        if (networkClasses.any { owner.startsWith(it) }) {
            reportIssue(
                OnDrawIssueType.NETWORK_OPERATION,
                "在 $methodName 中进行网络操作: $owner.$name",
                "网络操作会严重阻塞 UI 线程，必须在后台线程中进行"
            )
        }
    }
    
    /**
     * 检查耗时操作
     */
    private fun checkExpensiveOperation(owner: String, name: String) {
        // 检查数据库操作
        if (owner.contains("database") || owner.contains("sqlite") || owner.contains("room")) {
            reportIssue(
                OnDrawIssueType.DATABASE_OPERATION,
                "在 $methodName 中进行数据库操作: $owner.$name",
                "数据库操作耗时较长，应该在后台线程中进行"
            )
        }
        
        // 检查图片解码
        if (owner == "android/graphics/BitmapFactory" && name.startsWith("decode")) {
            reportIssue(
                OnDrawIssueType.BITMAP_DECODE,
                "在 $methodName 中解码图片: $owner.$name",
                "图片解码耗时较长，应该预先解码或使用缓存"
            )
        }
        
        // 检查复杂计算
        if (owner == "java/lang/Math" && (name == "pow" || name == "sqrt" || name == "sin" || name == "cos")) {
            reportIssue(
                OnDrawIssueType.COMPLEX_CALCULATION,
                "在 $methodName 中进行复杂数学计算: $owner.$name",
                "复杂计算应该预先进行或缓存结果"
            )
        }
    }
    
    /**
     * 检查自定义方法模式
     */
    private fun checkCustomMethodPatterns(owner: String, name: String) {
        extension.customMethodPatterns.get().forEach { pattern ->
            val fullMethodName = "$owner.$name"
            if (matchesPattern(fullMethodName, pattern)) {
                reportIssue(
                    OnDrawIssueType.CUSTOM_PATTERN,
                    "匹配自定义模式: $fullMethodName",
                    "检测到自定义的可疑方法调用模式"
                )
            }
        }
    }
    
    /**
     * 检查字段访问
     */
    private fun checkFieldAccess(opcode: Int, owner: String, name: String, descriptor: String) {
        // 检查静态字段访问（可能涉及类加载）
        if (opcode == Opcodes.GETSTATIC && owner.startsWith("java/lang/System")) {
            reportIssue(
                OnDrawIssueType.SYSTEM_CALL,
                "在 $methodName 中访问系统属性: $owner.$name",
                "系统调用可能有性能开销，考虑缓存结果"
            )
        }
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
    
    /**
     * 报告发现的问题
     */
    private fun reportIssue(type: OnDrawIssueType, message: String, suggestion: String) {
        val issue = OnDrawIssue(
            type = type,
            className = className,
            methodName = methodName,
            lineNumber = lineNumber,
            message = message,
            suggestion = suggestion,
            location = "$className.$methodName:$lineNumber"
        )
        detectedIssues.add(issue)
    }
}