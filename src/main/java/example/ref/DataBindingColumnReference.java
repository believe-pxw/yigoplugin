package example.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import example.index.DataObjectIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DataBindingColumnReference extends PsiReferenceBase<PsiElement> {

    private final String columnKey;
    private boolean isDefinition;

    public DataBindingColumnReference(@NotNull XmlAttributeValue element, TextRange textRange, boolean isDefinition) {
        super(element, textRange);
        this.columnKey = element.getValue();
        this.isDefinition = isDefinition;
    }

    @Override
    public @Nullable PsiElement resolve() {
        if (isDefinition) {
            return getElement();
        }
        XmlAttributeValue element = (XmlAttributeValue) getElement();
        XmlTag dataBindingTag = getParentDataBindingTag(element);

        if (dataBindingTag == null) {
            return null;
        }

        // 首先检查DataBinding是否有TableKey属性
        XmlAttribute tableKeyAttr = dataBindingTag.getAttribute("TableKey");
        String tableKey = null;

        if (tableKeyAttr != null && tableKeyAttr.getValue() != null) {
            tableKey = tableKeyAttr.getValue();
        } else {
            // 如果没有TableKey，向上查找GridRow的TableKey
            tableKey = findTableKeyFromGridRow(dataBindingTag);
        }

        if (tableKey == null) {
            return null;
        }

        // 在指定的Table中查找对应的Column
        return findColumnInTable(element, tableKey, columnKey);
    }

    /**
     * 获取包含当前元素的DataBinding标签
     */
    private XmlTag getParentDataBindingTag(XmlElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof XmlTag) {
                XmlTag tag = (XmlTag) parent;
                if ("DataBinding".equals(tag.getName()) || "Condition".equals(tag.getName())) {
                    return tag;
                }
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * 从GridRow中查找TableKey
     */
    private String findTableKeyFromGridRow(XmlTag dataBindingTag) {
        PsiElement current = dataBindingTag.getParent();

        while (current != null) {
            if (current instanceof XmlTag) {
                XmlTag tag = (XmlTag) current;
                if ("GridRow".equals(tag.getName())) {
                    XmlAttribute tableKeyAttr = tag.getAttribute("TableKey");
                    if (tableKeyAttr != null) {
                        return tableKeyAttr.getValue();
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 在指定的Table中查找Column
     */
    private XmlAttributeValue findColumnInTable(XmlElement startElement, String tableKey, String columnKey) {
        // 找到根元素（Form）
        XmlTag rootTag = getRootFormTag(startElement);
        if (rootTag == null) {
            return null;
        }

        // 查找DataSource
        XmlTag dataSourceTag = findChildTagByName(rootTag, "DataSource");
        if (dataSourceTag == null) {
            return null;
        }
        String refKey = dataSourceTag.getAttributeValue("RefObjectKey");
        XmlTag dataObjectTag;
        if (refKey != null) {
            XmlAttributeValue dataObjectDefinition = DataObjectIndex.findDataObjectDefinition(startElement.getProject(), refKey);
            dataObjectTag = (XmlTag) dataObjectDefinition.getParent().getParent();
        } else {
            dataObjectTag = findChildTagByName(dataSourceTag, "DataObject");

        }
        // 查找DataObject
        if (dataObjectTag == null) {
            return null;
        }

        // 查找TableCollection
        XmlTag tableCollectionTag = findChildTagByName(dataObjectTag, "TableCollection");
        if (tableCollectionTag == null) {
            return null;
        }

        // 查找指定的Table
        XmlTag targetTable = null;
        XmlTag[] tables = tableCollectionTag.findSubTags("Table");
        for (XmlTag table : tables) {
            XmlAttribute keyAttr = table.getAttribute("Key");
            if (keyAttr != null && tableKey.equals(keyAttr.getValue())) {
                targetTable = table;
                break;
            }
        }

        if (targetTable == null) {
            XmlTag embedTableCollection = findChildTagByName(dataObjectTag, "EmbedTableCollection");
            if (embedTableCollection != null) {
                XmlTag[] subTags = embedTableCollection.findSubTags("EmbedTable");
                for (XmlTag subTag : subTags) {
                    if (Objects.equals(subTag.getAttributeValue("TableKeys"), tableKey)) {
                        String objectKey = subTag.getAttributeValue("ObjectKey");
                        XmlAttributeValue dataObjectDefinition = DataObjectIndex.findDataObjectDefinition(startElement.getProject(), objectKey);
                        dataObjectTag = (XmlTag) dataObjectDefinition.getParent().getParent();
                        XmlTag[] subTags1 = dataObjectTag.findSubTags("TableCollection");
                        for (XmlTag xmlTag : subTags1) {
                            XmlTag[] subTags2 = xmlTag.findSubTags("Table");
                            for (XmlTag tag : subTags2) {
                                String key = tag.getAttributeValue("Key");
                                if (key.equals(tableKey)) {
                                    targetTable = tag;
                                }
                            }
                        }
                    }
                }
            }
            if (targetTable == null) {
                return null;
            }
        }

        // 在Table中查找指定的Column
        XmlTag[] columns = targetTable.findSubTags("Column");
        for (XmlTag column : columns) {
            XmlAttribute keyAttr = column.getAttribute("Key");
            if (keyAttr != null && columnKey.equals(keyAttr.getValue())) {
                return column.getAttribute("Key").getValueElement();
            }
        }

        return null;
    }

    /**
     * 获取根Form标签
     */
    private XmlTag getRootFormTag(XmlElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof XmlTag) {
                XmlTag tag = (XmlTag) current;
                if ("Form".equals(tag.getName())) {
                    return tag;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 按名称查找子标签
     */
    private XmlTag findChildTagByName(XmlTag parent, String tagName) {
        XmlTag[] children = parent.findSubTags(tagName);
        return children.length > 0 ? children[0] : null;
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}