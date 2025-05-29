package com.github.believepxw.yigo.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.IncorrectOperationException
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

    // IMPORTANT: This set defines which XML tags are considered variable *declarations*.
    // Based on your previous description, these are the tags that *use* the variable
    // (e.g., <GridCell Key="myVar"/>).
    // If your variable *declarations* (where you jump TO) have different tag names
    // (e.g., <VariableDef name="myVar"/>), you MUST update this set to reflect those declaration tags.
    // For now, I'll assume your declarations are also the same tags using a 'name' attribute for definition.
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

    /**
     * 解析引用，返回它指向的PSI元素（即变量的声明）。
     *
     * @return 如果解析成功，返回声明的PsiElement；否则返回null。
     */
    override fun resolve(): PsiElement? {
        val containingFile = element.containingFile as? XmlFile ?: return null

        // 从整个XML文件的根标签开始递归查找变量定义
        val rootTag = containingFile.document?.rootTag ?: return null
        return findVariableDefinitionRecursive(rootTag, variableName)
    }

    /**
     * 提供代码补全的建议。
     *
     * @return 建议的对象数组。
     */
    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    /**
     * 处理引用被重命名的情况。
     * 当变量声明被重命名时，引用也应该更新。
     *
     * @param newElementName 新的元素名称。
     * @return 更新后的引用元素（通常是更新后的XmlAttributeValue）。
     * @throws IncorrectOperationException 如果操作不允许。
     */
    override fun handleElementRename(newElementName: String): PsiElement {
        // We expect 'element' to be an XmlAttributeValue. Its parent is XmlAttribute.
        val oldAttribute = element.parent as? XmlAttribute
            ?: throw IncorrectOperationException("Cannot rename: parent of PsiElement is not an XmlAttribute.")

        val newAttribute = XmlElementFactory.getInstance(element.project)
            .createAttribute(oldAttribute.name, newElementName, oldAttribute)

        // Replace the old attribute with the new one
        val replacedAttribute = oldAttribute.replace(newAttribute)

        // Return the XmlAttributeValue of the newly replaced attribute
        return replacedAttribute
    }

    /**
     * 判断引用是否是"软"引用。软引用即使无法解析也不会报错。
     * 对于变量引用，通常是硬引用（false），因为无法解析通常意味着错误。
     */
    override fun isSoft(): Boolean = false


    // --- Helper Methods for Recursive Lookup ---

    /**
     * 递归查找变量定义。
     *
     * @param currentTag 当前正在检查的XmlTag。
     * @param variableName 要查找的变量名称。
     * @return 匹配的 XmlTag 如果找到，否则返回null。
     */
    private fun findVariableDefinitionRecursive(currentTag: XmlTag, variableName: String): PsiElement? {
        // Check if the current tag is a variable definition itself.
        // Assuming variable definitions have a 'name' attribute and are among the specified tags.
        // If your variable declarations use a different attribute (e.g., 'id'), change "name" below.
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
}