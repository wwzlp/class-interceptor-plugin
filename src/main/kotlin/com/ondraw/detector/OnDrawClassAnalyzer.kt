package com.ondraw.detector

import org.objectweb.asm.*

/**
 * OnDraw 类分析器
 * 使用 ASM 分析类中的 onDraw 方法，检测性能问题
 */
class OnDrawClassAnalyzer(
    nextVisitor: ClassVisitor,
    private val className: String,
    private val extension: ClassInterceptorExtension,
    private val onIssueFound: (OnDrawIssue) -> Unit
) : ClassVisitor(Opcodes.ASM7, nextVisitor) {
    
    private var isViewClass = false
    private var superClassName: String? = null
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        superClassName = superName
        
        // 检查是否是 View 的子类
        isViewClass = isViewSubclass(superName, interfaces)
        
        // 根据 verbose 配置决定是否输出详细信息
        if (extension.verbose.get()) {
            println("[OnDrawClassInterceptor] 分析类: $className, 父类: ${superName ?: "无"}, 是否View子类: $isViewClass")
        }
        
        super.visit(version, access, name, signature, superName, interfaces)
    }
    
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor? {
        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        
        // 只分析 onDraw 方法
        if (name == "onDraw" && isViewClass) {
            if (extension.verbose.get()) {
                println("[OnDrawClassInterceptor] 发现 onDraw 方法: $className.$name")
            }
            
            return OnDrawMethodAnalyzer(
                methodVisitor,
                className,
                name,
                extension,
                onIssueFound
            )
        }
        
        return methodVisitor
    }
    
    /**
     * 检查是否是 View 的子类
     */
    private fun isViewSubclass(superName: String?, interfaces: Array<String>?): Boolean {
        // 检查直接继承
        superName?.let { parent ->
            if (isKnownViewClass(parent)) {
                return true
            }
        }
        
        // 检查接口实现（虽然 View 通常不是接口）
        interfaces?.forEach { interfaceName ->
            if (isKnownViewClass(interfaceName)) {
                return true
            }
        }
        
        // 检查类名是否包含 View 相关关键词
        val classSimpleName = className.substringAfterLast('.')
        return classSimpleName.contains("View") || 
               classSimpleName.contains("Layout") ||
               classSimpleName.contains("Button") ||
               classSimpleName.contains("Text") ||
               classSimpleName.contains("Image") ||
               classSimpleName.contains("Custom")
    }
    
    /**
     * 检查是否是已知的 View 类
     */
    private fun isKnownViewClass(className: String): Boolean {
        val viewClasses = setOf(
            "android/view/View",
            "android/view/ViewGroup",
            "android/widget/TextView",
            "android/widget/Button",
            "android/widget/ImageView",
            "android/widget/LinearLayout",
            "android/widget/RelativeLayout",
            "android/widget/FrameLayout",
            "android/widget/ScrollView",
            "android/widget/ListView",
            "android/widget/RecyclerView",
            "androidx/recyclerview/widget/RecyclerView",
            "androidx/appcompat/widget/AppCompatTextView",
            "androidx/appcompat/widget/AppCompatButton",
            "androidx/appcompat/widget/AppCompatImageView",
            "androidx/constraintlayout/widget/ConstraintLayout",
            "androidx/cardview/widget/CardView",
            "com/google/android/material/textview/MaterialTextView",
            "com/google/android/material/button/MaterialButton"
        )
        
        return viewClasses.contains(className) || 
               className.startsWith("android/view/") ||
               className.startsWith("android/widget/") ||
               className.startsWith("androidx/") && className.contains("widget")
    }
}