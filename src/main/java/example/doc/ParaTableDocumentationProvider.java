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
import example.index.DataObjectIndex;
import example.ref.DataObjectReference;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.intellij.lang.documentation.DocumentationProvider;

import static example.index.AttrConstant.*;

public class ParaTableDocumentationProvider implements DocumentationProvider {




    // Caches for ParaTable (your existing cache)
    private static final Map<Project, Map<String, String>> cachedParaGroupDocs = new ConcurrentHashMap<>();

    // Caches for DataElement and Domain
    // Project -> DataElementKey -> Map<AttributeName, AttributeValue>
    private static final Map<Project, Map<String, Map<String, String>>> cachedDataElements = new ConcurrentHashMap<>();
    private static final Map<Project, Map<String, PsiElement>> cachedDataElementPsi = new ConcurrentHashMap<>();
    private static final Map<Project, Map<String, PsiElement>> cachedDomainPsi = new ConcurrentHashMap<>();
    // Project -> DomainKey -> Map<AttributeName, AttributeValue>
    private static final Map<Project, Map<String, Map<String, String>>> cachedDomains = new ConcurrentHashMap<>();


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

    public static String getParaGroupDoc(Project project,String groupKeyValue) {
        // 尝试从缓存获取，如果没有则查找并生成
        return cachedParaGroupDocs
                .computeIfAbsent(project, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(groupKeyValue, key -> findAndGenerateParaGroupDocumentation(project, key));
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
        // Ensure caches are populated
        populateDataElementCache(project);
        populateDomainCache(project);

        StringBuilder docBuilder = new StringBuilder();

        // Get DataElement information
        Map<String, String> dataElementInfo = cachedDataElements.getOrDefault(project, Collections.emptyMap()).get(dataElementKey);
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
                Map<String, String> domainInfo = cachedDomains.getOrDefault(project, Collections.emptyMap()).get(domainKey);
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

    public static PsiElement getItemKeyByElementKey(Project project, String elementKey)  {
        populateDataElementCache(project);
        populateDomainCache(project);
        Map<String, String> dataElementInfo = cachedDataElements.getOrDefault(project, Collections.emptyMap()).get(elementKey);
        if (dataElementInfo != null) {
            String domainKey = dataElementInfo.get("DomainKey");
            Map<String, String> domainInfo = cachedDomains.getOrDefault(project, Collections.emptyMap()).get(domainKey);
            if (domainInfo != null) {
                String itemKey1 = domainInfo.get("ItemKey");
                if (itemKey1 != null && !itemKey1.isEmpty()) {
                    return DataObjectIndex.findDataObjectDefinition(project, itemKey1);
                }
            }
        }
        return null;
    }


    public static PsiElement getDataElementPsi(Project project, String elementKey)  {
        populateDataElementCache(project);
        populateDomainCache(project);
        return cachedDataElementPsi.getOrDefault(project, Collections.emptyMap()).get(elementKey);
    }

    public static PsiElement getDomainPsi(Project project, String domainKey)  {
        populateDataElementCache(project);
        populateDomainCache(project);
        return cachedDomainPsi.getOrDefault(project, Collections.emptyMap()).get(domainKey);
    }


    private static synchronized void populateDataElementCache(Project project) {
        // Only populate if not already cached for this project
        if (!cachedDataElements.containsKey(project)) {
            Map<String, Map<String, String>> projectDataElements = new ConcurrentHashMap<>();
            Map<String, PsiElement> projectDataElementPsis = new ConcurrentHashMap<>();
            Collection<VirtualFile> xmlFiles = FileTypeIndex.getFiles(XmlFileType.INSTANCE, GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : xmlFiles) {
                // Check if the file is in the DataElement folder
                if (virtualFile.getParent() != null && DATA_ELEMENT_FOLDER.equals(virtualFile.getParent().getName()) && virtualFile.getName().endsWith(".xml")) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile instanceof XmlFile) {
                        XmlFile dataElementFile = (XmlFile) psiFile;
                        XmlTag rootTag = dataElementFile.getRootTag();
                        if (rootTag != null && DATA_ELEMENT_DEF_TAG.equals(rootTag.getName())) {
                            XmlTag collectionTag = rootTag.findFirstSubTag(DATA_ELEMENT_COLLECTION_TAG);
                            if (collectionTag != null) {
                                for (XmlTag dataElementTag : collectionTag.findSubTags(DATA_ELEMENT_TAG)) {
                                    String key = dataElementTag.getAttributeValue(KEY_ATTRIBUTE);
                                    if (key != null) {
                                        projectDataElementPsis.put(key, dataElementTag);
                                        Map<String, String> attributes = new HashMap<>();
                                        attributes.put(KEY_ATTRIBUTE, key);
                                        Optional.ofNullable(dataElementTag.getAttributeValue(CAPTION_ATTRIBUTE))
                                                .ifPresent(val -> attributes.put(CAPTION_ATTRIBUTE, val));
                                        Optional.ofNullable(dataElementTag.getAttributeValue("DomainKey")) // Assuming DomainKey is also a key here
                                                .ifPresent(val -> attributes.put("DomainKey", val)); // Keep the specific DomainKey attribute if it's explicitly there
                                        projectDataElements.put(key, attributes);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            cachedDataElements.put(project, projectDataElements);
            cachedDataElementPsi.put(project, projectDataElementPsis);
        }
    }

    private static synchronized void populateDomainCache(Project project) {
        // Only populate if not already cached for this project
        if (!cachedDomains.containsKey(project)) {
            Map<String, Map<String, String>> projectDomains = new ConcurrentHashMap<>();
            Map<String, PsiElement> projectDomainPsis = new ConcurrentHashMap<>();
            Collection<VirtualFile> xmlFiles = FileTypeIndex.getFiles(XmlFileType.INSTANCE, GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : xmlFiles) {
                // Check if the file is in the Domain folder
                if (virtualFile.getParent() != null && DOMAIN_FOLDER.equals(virtualFile.getParent().getName()) && virtualFile.getName().endsWith(".xml")) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile instanceof XmlFile) {
                        XmlFile domainFile = (XmlFile) psiFile;
                        XmlTag rootTag = domainFile.getRootTag();
                        if (rootTag != null && DOMAIN_DEF_TAG.equals(rootTag.getName())) {
                            XmlTag collectionTag = rootTag.findFirstSubTag(DOMAIN_COLLECTION_TAG);
                            if (collectionTag != null) {
                                for (XmlTag domainTag : collectionTag.findSubTags(DOMAIN_TAG)) {
                                    String key = domainTag.getAttributeValue(KEY_ATTRIBUTE);
                                    if (key != null) {
                                        projectDomainPsis.put(key, domainTag);
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
                                        projectDomains.put(key, attributes);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            cachedDomains.put(project, projectDomains);
            cachedDomainPsi.put(project, projectDomainPsis);
        }
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