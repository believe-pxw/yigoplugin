package com.github.believepxw.yigo.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.util.*

/**
 * 为XML中特定标签的'Key'属性提供变量引用解析。
 * 假设变量定义是在XML文件中的任意位置，满足特定标签名且具有'name'属性。
 *
 * @param element 引用所在的PSI元素，通常是 XmlAttributeValue。
 * @param textRangeInElement 引用文本在 element 中的范围。
 * @param variableName 要解析的变量名称。
 */
class VariableReference(
    element: PsiElement, // Keep as PsiElement as it's the base for XmlAttributeValue
    textRangeInElement: TextRange,
    private val variableName: String
) : PsiReferenceBase<PsiElement>(element, textRangeInElement) {

    companion object {
        val variableDefinitionTagNames: Set<String> = Collections.unmodifiableSet(
            setOf(
                "Dict", "DynamicDict", "TextEditor", "TextArea", "CheckBox", "ComboBox",
                "CheckListBox", "DatePicker", "UTCDatePicker", "MonthPicker", "TimePicker",
                "Button", "NumberEditor", "Label", "TextButton", "RadioButton", "PasswordEditor",
                "Image", "WebBrowser", "RichEditor", "HyperLink", "Separator", "DropdownButton",
                "Icon", "Custom", "BPMGraph", "Dynamic", "Carousel", "EditView", "Gantt", // Existing usage tags
                "Variable", "VarDef", "GridCell" // Add common variable definition tag names if they are different
            )
        )
    }

    /**
     * 解析引用，返回它指向的PSI元素（即变量的声明）。
     *
     * @return 如果解析成功，返回声明的PsiElement；否则返回null。
     */
    override fun resolve(): PsiElement? {
        //Key属性直接返回自己
        if (element is XmlAttributeValue && element.parent.firstChild.text == "Key") {
            return element
        }
        val containingFile = element.containingFile as? XmlFile ?: return null

        // 从整个XML文件的根标签开始递归查找变量定义
        val rootTag = containingFile.document?.rootTag ?: return null
        return findVariableDefinitionRecursive(rootTag, variableName)
    }

    /**
     * 递归查找变量定义。
     *
     * @param currentTag 当前正在检查的XmlTag。
     * @param variableName 要查找的变量名称。
     * @return 匹配的 XmlTag 如果找到，否则返回null。
     */
    private fun findVariableDefinitionRecursive(currentTag: XmlTag, variableName: String): PsiElement? {
        if (currentTag.localName in variableDefinitionTagNames && currentTag.getAttributeValue("Key") == variableName) {
            return currentTag.getAttribute("Key")?.lastChild
        }

        // Recursively check all sub-tags
        for (subTag in currentTag.subTags) {
            val foundTag = findVariableDefinitionRecursive(subTag, variableName)
            if (foundTag != null) {
                return foundTag
            }
        }
        return null
    }

    override fun isSoft(): Boolean {
        return true
    }
}