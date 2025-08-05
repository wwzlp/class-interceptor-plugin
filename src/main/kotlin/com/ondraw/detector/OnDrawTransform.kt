package com.ondraw.detector

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * OnDraw Transform 类
 * 使用 Transform API 进行字节码转换，兼容 AGP 7.0.4
 */
class OnDrawTransform(private val extension: ClassInterceptorExtension) : Transform() {
    
    override fun getName(): String = "OnDrawClassInterceptor"
    
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }
    
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }
    
    override fun isIncremental(): Boolean = false
    
    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        
        // 从 TransformInvocation 中获取变体名称
        val variantName = getVariantName(transformInvocation)
        
        if (extension.verbose.get()) {
            println("[OnDrawClassInterceptor] Transform 开始执行 - 变体: $variantName")
        }
        
        // 清空之前的问题记录
        OnDrawClassVisitorFactory.clearAllIssues()
        
        val outputProvider = transformInvocation.outputProvider
        
        // 清空输出目录
        outputProvider.deleteAll()
        
        // 处理所有输入
        transformInvocation.inputs.forEach { input ->
            // 处理目录输入
            input.directoryInputs.forEach { directoryInput ->
                processDirectory(directoryInput, outputProvider, variantName)
            }
            
            // 处理 JAR 输入
            input.jarInputs.forEach { jarInput ->
                processJar(jarInput, outputProvider, variantName)
            }
        }
        
        // 在 Transform 完成后生成报告
        val projectDir = transformInvocation.context.temporaryDir.parentFile.parentFile.parentFile
        
        if (extension.verbose.get()) {
            println("[OnDrawClassInterceptor] 项目目录: ${projectDir.absolutePath}")
            println("[OnDrawClassInterceptor] 报告输出目录: ${extension.reportOutputDir.get()}")
        }
        
        OnDrawClassVisitorFactory.generateReports(extension, variantName, projectDir)
        
        if (extension.verbose.get()) {
            println("[OnDrawClassInterceptor] Transform 执行完成 - 变体: $variantName")
        }
    }
    
    /**
     * 从 TransformInvocation 中获取变体名称
     */
    private fun getVariantName(transformInvocation: TransformInvocation): String {
        // 尝试从输出路径中解析变体名称
        val outputDir = transformInvocation.outputProvider.getContentLocation(
            "main",
            getInputTypes(),
            getScopes(),
            Format.DIRECTORY
        )
        
        // 从路径中提取变体名称，例如：/path/to/build/intermediates/transforms/OnDrawClassInterceptor/debug/...
        val pathSegments = outputDir.absolutePath.split(File.separator)
        val transformIndex = pathSegments.indexOfLast { it == "OnDrawClassInterceptor" }
        
        return if (transformIndex >= 0 && transformIndex + 1 < pathSegments.size) {
            pathSegments[transformIndex + 1]
        } else {
            // 如果无法从路径解析，尝试从临时目录路径解析
            val tempDir = transformInvocation.context.temporaryDir.absolutePath
            when {
                tempDir.contains("/debug/") -> "debug"
                tempDir.contains("/release/") -> "release"
                else -> "unknown"
            }
        }
    }
    
    /**
     * 处理目录输入
     */
    private fun processDirectory(directoryInput: DirectoryInput, outputProvider: TransformOutputProvider, variantName: String) {
        val outputDir = outputProvider.getContentLocation(
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY
        )
        
        if (directoryInput.file.isDirectory) {
            directoryInput.file.walkTopDown().forEach { file ->
                if (file.isFile && file.name.endsWith(".class")) {
                    val relativePath = directoryInput.file.toURI().relativize(file.toURI()).path
                    val outputFile = File(outputDir, relativePath)
                    
                    // 确保输出目录存在
                    outputFile.parentFile.mkdirs()
                    
                    // 处理 class 文件
                    processClassFile(file, outputFile, variantName)
                } else if (file.isFile) {
                    // 复制非 class 文件
                    val relativePath = directoryInput.file.toURI().relativize(file.toURI()).path
                    val outputFile = File(outputDir, relativePath)
                    outputFile.parentFile.mkdirs()
                    file.copyTo(outputFile, overwrite = true)
                }
            }
        }
    }
    
    /**
     * 处理 JAR 输入
     */
    private fun processJar(jarInput: JarInput, outputProvider: TransformOutputProvider, variantName: String) {
        val outputJar = outputProvider.getContentLocation(
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
        
        // 确保输出目录存在
        outputJar.parentFile.mkdirs()
        
        JarFile(jarInput.file).use { inputJar ->
            JarOutputStream(FileOutputStream(outputJar)).use { outputJarStream ->
                inputJar.entries().asSequence().forEach { entry ->
                    val inputStream = inputJar.getInputStream(entry)
                    val outputEntry = JarEntry(entry.name)
                    outputJarStream.putNextEntry(outputEntry)
                    
                    if (entry.name.endsWith(".class") && !entry.isDirectory) {
                        // 处理 class 文件
                        val classBytes = inputStream.readBytes()
                        val transformedBytes = processClassBytes(classBytes, entry.name, variantName)
                        outputJarStream.write(transformedBytes)
                    } else {
                        // 复制其他文件
                        inputStream.copyTo(outputJarStream)
                    }
                    
                    outputJarStream.closeEntry()
                }
            }
        }
    }
    
    /**
     * 处理单个 class 文件
     */
    private fun processClassFile(inputFile: File, outputFile: File, variantName: String) {
        val classBytes = inputFile.readBytes()
        val transformedBytes = processClassBytes(classBytes, inputFile.name, variantName)
        outputFile.writeBytes(transformedBytes)
    }
    
    /**
     * 处理 class 字节码
     */
    private fun processClassBytes(classBytes: ByteArray, className: String, variantName: String): ByteArray {
        return try {
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 开始处理类: $className")
            }
            
            val classReader = ClassReader(classBytes)
            // 使用 COMPUTE_MAXS 而不是 COMPUTE_FRAMES，避免在 AGP 4.2.2 中的类型解析问题
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
            
            // 创建 ClassVisitor
            val classVisitor = OnDrawClassVisitorFactory.createClassVisitor(
                classWriter, 
                className,
                extension,
                variantName
            )
            
            // 访问类，不使用 EXPAND_FRAMES
            classReader.accept(classVisitor, 0)
            
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 成功处理类: $className")
            }
            
            classWriter.toByteArray()
        } catch (e: Exception) {
            // 输出完整的异常信息，包括异常类型和详细信息
            val errorMessage = buildString {
                append("[OnDrawClassInterceptor] 处理类 $className 时出错:\n")
                append("异常类型: ${e.javaClass.simpleName}\n")
                append("异常信息: ${e.message ?: "无详细信息"}\n")
                if (extension.verbose.get()) {
                    append("堆栈跟踪:\n")
                    e.stackTrace.take(5).forEach { stackElement ->
                        append("  at $stackElement\n")
                    }
                }
            }
            
            println(errorMessage)
            
            // 如果处理失败，返回原始字节码
            classBytes
        }
    }
}