package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.ide.highlighter.XmlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OperationRefKeyReference extends PsiReferenceBase<XmlAttributeValue> {

    private final String refKey;

    public OperationRefKeyReference(@NotNull XmlAttributeValue element, TextRange textRange) {
        super(element, textRange);
        this.refKey = element.getValue();
    }

    @Override
    public @Nullable PsiElement resolve() {
        if (refKey == null || refKey.trim().isEmpty()) {
            return null;
        }

        Project project = getElement().getProject();

        // 查找所有XML文件
        Collection<VirtualFile> xmlFiles = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                GlobalSearchScope.projectScope(project));

        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile virtualFile : xmlFiles) {
            // 优先查找CommonDef.xml文件
            if (virtualFile.getName().equals("CommonDef.xml")) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile instanceof XmlFile) {
                    XmlAttributeValue foundOperation = findOperationByKey((XmlFile) psiFile, refKey);
                    if (foundOperation != null) {
                        return foundOperation;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 在XML文件中查找指定Key的Operation
     */
    private XmlAttributeValue findOperationByKey(XmlFile xmlFile, String key) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) {
            return null;
        }

        // 查找OperationCollection
        List<XmlTag> operationCollections = findTagsByName(rootTag, "OperationCollection");

        for (XmlTag operationCollection : operationCollections) {
            XmlTag[] operations = operationCollection.findSubTags("Operation");
            for (XmlTag operation : operations) {
                XmlAttribute keyAttr = operation.getAttribute("Key");
                if (keyAttr != null && key.equals(keyAttr.getValue())) {
                    return operation.getAttribute("Key").getValueElement();
                }
            }
        }

        return null;
    }

    /**
     * 递归查找指定名称的标签
     */
    private List<XmlTag> findTagsByName(XmlTag parent, String tagName) {
        List<XmlTag> result = new ArrayList<>();

        // 直接子标签
        XmlTag[] directChildren = parent.findSubTags(tagName);
        for (XmlTag child : directChildren) {
            result.add(child);
        }

        // 递归查找所有子标签
        XmlTag[] allChildren = parent.getSubTags();
        for (XmlTag child : allChildren) {
            result.addAll(findTagsByName(child, tagName));
        }

        return result;
    }

    @Override
    public Object @NotNull [] getVariants() {
        // 返回所有可用的Operation Key作为代码补全选项
        List<String> variants = new ArrayList<>();
        Project project = getElement().getProject();

        Collection<VirtualFile> xmlFiles = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                GlobalSearchScope.projectScope(project));

        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile virtualFile : xmlFiles) {
            PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile instanceof XmlFile) {
                collectOperationKeys((XmlFile) psiFile, variants);
            }
        }

        return variants.toArray();
    }

    /**
     * 收集XML文件中所有Operation的Key
     */
    private void collectOperationKeys(XmlFile xmlFile, List<String> variants) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) {
            return;
        }

        List<XmlTag> operationCollections = findTagsByName(rootTag, "OperationCollection");

        for (XmlTag operationCollection : operationCollections) {
            XmlTag[] operations = operationCollection.findSubTags("Operation");
            for (XmlTag operation : operations) {
                XmlAttribute keyAttr = operation.getAttribute("Key");
                if (keyAttr != null && keyAttr.getValue() != null) {
                    String key = keyAttr.getValue();
                    if (!variants.contains(key)) {
                        variants.add(key);
                    }
                }
            }
        }
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return getElement().getParent();
    }

    /**
     * 检查当前元素是否在Operation标签的RefKey属性中
     */
    public static boolean isValidRefKeyContext(XmlAttributeValue attributeValue) {
        XmlAttribute attribute = (XmlAttribute) attributeValue.getParent();
        if (attribute == null || !"RefKey".equals(attribute.getName())) {
            return false;
        }

        XmlTag parentTag = attribute.getParent();
        return parentTag != null && "Operation".equals(parentTag.getName());
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}