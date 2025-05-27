package example.ref;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Import your ParaTableDocumentationProvider to access caches if needed, or re-implement finding logic.
// For simplicity, we'll reuse the caching mechanism from the DocumentationProvider.
import example.doc.ParaTableDocumentationProvider;

/**
 * Represents a reference from an ItemKey attribute value to a DataObject's XmlTag.
 */
public class DataObjectReference extends PsiReferenceBase<XmlAttributeValue> {

    private final String dataObjectKey;
    // We need a way to get to the DataObject PsiElement.
    // It's best to keep the DataObject finding logic separate or access a shared cache.
    // For this example, we'll assume ParaTableDocumentationProvider's cache is accessible
    // or you have a dedicated service for parsing/caching PsiElements.
    // Let's create a simple cache for this reference class itself for demonstration.
    private static final Map<Project, Map<String, PsiElement>> cachedDataObjectPsiElements = new ConcurrentHashMap<>();

    public DataObjectReference(@NotNull XmlAttributeValue element, TextRange rangeInElement) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.dataObjectKey = element.getValue();
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = myElement.getProject();
        return getDataObjectPsi(project, dataObjectKey);
    }

    public static PsiElement getDataObjectPsi(Project project, String dataObjectKey) {
        if (!cachedDataObjectPsiElements.containsKey(project)) {
            // Re-use or adapt the logic from ParaTableDocumentationProvider's populateDataObjectCache
            populateDataObjectCache(project);
        }
        // Return the PsiElement from the cache
        return cachedDataObjectPsiElements.getOrDefault(project, Collections.emptyMap()).get(dataObjectKey);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        // This is used for autocompletion.
        // You could return a list of all known DataObject keys here.
        // For simplicity, we'll return an empty array.
        return new Object[0];
    }

    // This method is a simplified version of populateDataObjectCache from ParaTableDocumentationProvider.
    // In a production plugin, you'd extract this caching logic into a shared service.
    private static synchronized void populateDataObjectCache(Project project) {
        // Prevent concurrent modification and re-population
        if (cachedDataObjectPsiElements.containsKey(project)) {
            return;
        }

        Map<String, PsiElement> projectDataObjectPsiElements = new ConcurrentHashMap<>();
        Collection<VirtualFile> xmlFiles = FileTypeIndex.getFiles(XmlFileType.INSTANCE, GlobalSearchScope.projectScope(project));

        for (VirtualFile virtualFile : xmlFiles) {
            if (virtualFile.getName().endsWith(".xml")) { // Consider all XML files initially
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile instanceof XmlFile) {
                    XmlFile formFile = (XmlFile) psiFile;
                    XmlTag rootTag = formFile.getRootTag();
                    if (rootTag != null && "Form".equals(rootTag.getName())) { // Check for <Form> root tag
                        XmlTag dataSourceTag = rootTag.findFirstSubTag("DataSource");
                        if (dataSourceTag != null) {
                            for (XmlTag dataObjectTag : dataSourceTag.findSubTags("DataObject")) {
                                String key = dataObjectTag.getAttributeValue("Key");
                                if (key != null) {
                                    projectDataObjectPsiElements.put(key, dataObjectTag);
                                }
                            }
                        }
                    } else if (rootTag != null && "DataObject".equals(rootTag.getName())) {
                        String key = rootTag.getAttributeValue("Key");
                        if (key != null) {
                            projectDataObjectPsiElements.put(key, rootTag);
                        }
                    }
                }
            }
        }
        cachedDataObjectPsiElements.put(project, projectDataObjectPsiElements);
    }
}