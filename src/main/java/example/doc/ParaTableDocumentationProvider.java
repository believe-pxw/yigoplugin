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
import example.index.DataElementIndex;
import example.index.DataObjectIndex;
import example.index.DomainIndex;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.intellij.lang.documentation.DocumentationProvider;

import static example.index.AttrConstant.*;

public class ParaTableDocumentationProvider implements DocumentationProvider {


    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (originalElement == null) {
            return null;
        }

        // Check if the element is an XmlAttributeValue
        if (originalElement.getNode().getElementType().toString().equals("XML_ATTRIBUTE_VALUE_TOKEN")) {
            PsiElement parent = originalElement.getParent().getParent();
            if (parent instanceof XmlAttribute) {
                XmlAttribute attribute = (XmlAttribute) parent;
                Project project = element.getProject();
                if (GROUP_KEY_ATTRIBUTE.equals(attribute.getName())) {
                    // 获取属性值，并去除引号
                    String groupKeyValue = originalElement.getText();
                    return getParaGroupDoc(project, groupKeyValue);
                }
                // Handle DataElement 'DataElementKey'
                else if (DATA_ELEMENT_KEY_ATTRIBUTE.equals(attribute.getName())) {
                    String dataElementKeyValue = originalElement.getText();
                    return generateDataElementDocumentation(project, dataElementKeyValue);
                }
            }
        }
        return null;
    }

    public static String getParaGroupDoc(Project project, String groupKeyValue) {
        return findAndGenerateParaGroupDocumentation(project, groupKeyValue);
    }

    // --- ParaTable Documentation Logic (your existing logic) ---
    private static String findAndGenerateParaGroupDocumentation(Project project, String groupKey) {
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
        return null; // If no matching ParaGroup is found
    }

    private static String formatParaGroupDocumentation(XmlTag paraGroupTag) {
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

    // --- DataElement and Domain Documentation Logic ---

    private String generateDataElementDocumentation(Project project, String dataElementKey) {

        StringBuilder docBuilder = new StringBuilder();

        // Get DataElement information
        Map<String, String> dataElementInfo = getDataElementInfo(project, dataElementKey);
        if (dataElementInfo != null) {
            String dataElementCaption = dataElementInfo.get(CAPTION_ATTRIBUTE);
            String domainKey = dataElementInfo.get("DomainKey"); // In DataElement, the Key attribute is the DomainKey

            docBuilder.append("<h3>DataElement: ").append(dataElementKey).append("</h3>");
            docBuilder.append("<ul>");
            docBuilder.append("<li><b>Caption:</b> ").append(dataElementCaption != null ? dataElementCaption : "N/A").append("</li>");
            docBuilder.append("<li><b>DomainKey:</b> ").append(domainKey != null ? domainKey : "N/A").append("</li>");
            docBuilder.append("</ul>");

            // Get Domain information if DomainKey is present
            if (domainKey != null) {
                Map<String, String> domainInfo = getDomainInfo(project, domainKey);
                if (domainInfo != null) {
                    String domainCaption = domainInfo.get(CAPTION_ATTRIBUTE);
                    String refControlType = domainInfo.get(REF_CONTROL_TYPE_ATTRIBUTE);
                    String itemKey = domainInfo.get("ItemKey");
                    String groupKey = domainInfo.get("groupKey");
                    String comboBoxItem = domainInfo.get("comboBoxItem");

                    docBuilder.append("<h4>Domain: ").append(domainKey).append("</h4>");
                    docBuilder.append("<ul>");
                    docBuilder.append("<li><b>Caption:</b> ").append(domainCaption != null ? domainCaption : "N/A").append("</li>");
                    docBuilder.append("<li><b>RefControlType:</b> ").append(refControlType != null ? refControlType : "N/A").append("</li>");
                    if (itemKey != null) {
                        docBuilder.append("<li><b>ItemKey:</b> <a href=\"psi_element://dataObjectKey/").append(itemKey).append("\">").append(itemKey).append("</a></li>");
                    }
                    if (groupKey != null) {
                        docBuilder.append("<li><b>groupKey:</b> ").append(groupKey).append("</li>");
                    }
                    if (comboBoxItem != null && !comboBoxItem.isEmpty()) {
                        docBuilder.append("<li>").append(comboBoxItem).append("</li>");
                    }
                    docBuilder.append("</ul>");
                } else {
                    docBuilder.append("<p><i>No matching Domain found for key: ").append(domainKey).append("</i></p>");
                }
            }
            return docBuilder.toString();
        } else {
            return docBuilder.append("<p><i>No matching DataElement found for key: ").append(dataElementKey).append("</i></p>").toString();
        }
    }


    private static Map<String, String> getDataElementInfo(Project project, String elementKey) {
        XmlTag dataElementTag = DataElementIndex.findDEDefinition(project, elementKey);
        if (dataElementTag != null) {
            String key = dataElementTag.getAttributeValue(KEY_ATTRIBUTE);
            if (key != null) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(KEY_ATTRIBUTE, key);
                Optional.ofNullable(dataElementTag.getAttributeValue(CAPTION_ATTRIBUTE))
                        .ifPresent(val -> attributes.put(CAPTION_ATTRIBUTE, val));
                Optional.ofNullable(dataElementTag.getAttributeValue("DomainKey")) // Assuming DomainKey is also a key here
                        .ifPresent(val -> attributes.put("DomainKey", val)); // Keep the specific DomainKey attribute if it's explicitly there
                return attributes;
            }
        }
        return null;
    }

    private static Map<String, String> getDomainInfo(Project project, String domainKey) {
        XmlTag domainTag = DomainIndex.findDomainDefinition(project, domainKey);
        if (domainTag != null) {
            String key = domainTag.getAttributeValue(KEY_ATTRIBUTE);
            if (key != null) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(KEY_ATTRIBUTE, key);
                Optional.ofNullable(domainTag.getAttributeValue(CAPTION_ATTRIBUTE))
                        .ifPresent(val -> attributes.put(CAPTION_ATTRIBUTE, val));
                Optional.ofNullable(domainTag.getAttributeValue(REF_CONTROL_TYPE_ATTRIBUTE))
                        .ifPresent(val -> attributes.put(REF_CONTROL_TYPE_ATTRIBUTE, val));
                Optional.ofNullable(domainTag.getAttributeValue(DATA_TYPE_ATTRIBUTE))
                        .ifPresent(val -> attributes.put(DATA_TYPE_ATTRIBUTE, val));
                Optional.ofNullable(domainTag.getAttributeValue("ItemKey"))
                        .ifPresent(val -> attributes.put("ItemKey", val));
                String comboBoxItemStr = "";
                String groupKey = domainTag.getAttributeValue("GroupKey");
                if (groupKey != null) {
                    comboBoxItemStr = getParaGroupDoc(project, groupKey);
                    attributes.put("groupKey", groupKey);
                }
                XmlTag[] subTags = domainTag.getSubTags();
                if (subTags.length > 0) {
                    comboBoxItemStr = formatParaGroupDocumentation(domainTag);
                }
                if (comboBoxItemStr != null && comboBoxItemStr.length() > 0) {
                    attributes.put("comboBoxItem", comboBoxItemStr);
                }
                return attributes;
            }
        }
        return null;
    }


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
        if (link.startsWith("dataObjectKey/")) {
            String key = link.substring("dataObjectKey/".length());
            XmlAttributeValue dataObjectDefinition = DataObjectIndex.findDataObjectDefinition(psiManager.getProject(), key);
            if (dataObjectDefinition != null) {
                return dataObjectDefinition.getContainingFile();
            }
            return null;
        } else {
            return null;
        }
    }
}