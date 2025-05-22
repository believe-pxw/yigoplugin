package example.doc;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.intellij.lang.documentation.DocumentationProvider;


public class ParaTableDocumentationProvider implements DocumentationProvider { // 直接实现 DocumentationProvider 接口

    private static final String PARATABLE_FILENAME_PREFIX = "ParaTable";
    private static final String PARA_GROUP_TAG = "ParaGroup";
    private static final String ITEM_TAG = "Item";
    private static final String KEY_ATTRIBUTE = "Key";
    private static final String CAPTION_ATTRIBUTE = "Caption";
    private static final String VALUE_ATTRIBUTE = "Value";
    private static final String GROUP_KEY_ATTRIBUTE = "GroupKey";

    // 缓存解析结果，避免重复查找和解析文件
    private final Map<Project, Map<String, String>> cachedParaGroupDocs = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        // Check if the element is an XmlAttributeValue of a GroupKey attribute
        if (originalElement.getNode().getElementType().toString().equals("XML_ATTRIBUTE_VALUE_TOKEN")) {
            PsiElement parent = originalElement.getParent().getParent();
            if (parent instanceof XmlAttribute) {
                XmlAttribute attribute = (XmlAttribute) parent;
                if (GROUP_KEY_ATTRIBUTE.equals(attribute.getName())) {
                    // 获取属性值，并去除引号
                    String groupKeyValue = originalElement.getText();

                    Project project = element.getProject();
                    // 尝试从缓存获取，如果没有则查找并生成
                    return cachedParaGroupDocs
                            .computeIfAbsent(project, p -> new ConcurrentHashMap<>())
                            .computeIfAbsent(groupKeyValue, key -> findAndGenerateParaGroupDocumentation(project, key));
                }
            }
        }
        return null;
    }

    // 查找所有 ParaTable XML 文件并生成文档
    private String findAndGenerateParaGroupDocumentation(Project project, String groupKey) {
        Collection<VirtualFile> xmlFiles = FileTypeIndex.getFiles(XmlFileType.INSTANCE, GlobalSearchScope.projectScope(project));

        for (VirtualFile virtualFile : xmlFiles) {
            if (virtualFile.getName().startsWith(PARATABLE_FILENAME_PREFIX) && virtualFile.getName().endsWith(".xml")) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile instanceof XmlFile) {
                    XmlFile paraTableFile = (XmlFile) psiFile;
                    XmlTag rootTag = paraTableFile.getRootTag();
                    if (rootTag != null) {
                        for (XmlTag paraGroupTag : rootTag.findSubTags(PARA_GROUP_TAG)) {
                            String keyAttribute = paraGroupTag.getAttributeValue(KEY_ATTRIBUTE);
                            if (groupKey.equals(keyAttribute)) {
                                return formatParaGroupDocumentation(paraGroupTag);
                            }
                        }
                    }
                }
            }
        }
        return null; // 如果没有找到匹配的 ParaGroup
    }

    // 格式化 ParaGroup 的文档内容
    private String formatParaGroupDocumentation(XmlTag paraGroupTag) {
        StringBuilder docBuilder = new StringBuilder();
        docBuilder.append("<h3>表单类型: ").append(paraGroupTag.getAttributeValue(CAPTION_ATTRIBUTE)).append("</h3>");
        docBuilder.append("<table>");
        docBuilder.append("<tr><th>Key</th><th>Caption</th><th>Value</th></tr>");

        for (XmlTag itemTag : paraGroupTag.findSubTags(ITEM_TAG)) {
            String itemKey = itemTag.getAttributeValue(KEY_ATTRIBUTE);
            String itemCaption = itemTag.getAttributeValue(CAPTION_ATTRIBUTE);
            String itemValue = itemTag.getAttributeValue(VALUE_ATTRIBUTE);
            docBuilder.append("<tr>");
            docBuilder.append("<td>").append(itemKey != null ? itemKey : "").append("</td>");
            docBuilder.append("<td>").append(itemCaption != null ? itemCaption : "").append("</td>");
            docBuilder.append("<td>").append(itemValue != null ? itemValue : "").append("</td>");
            docBuilder.append("</tr>");
        }
        docBuilder.append("</table>");
        return docBuilder.toString();
    }

    // 实现 DocumentationProvider 接口的其他方法
    // 在这个场景下，这些方法通常返回 null 或空集合，因为我们只关心 generateDoc
    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        return null;
    }
}
