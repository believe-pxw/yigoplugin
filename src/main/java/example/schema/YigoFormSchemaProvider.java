package example.schema;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class YigoFormSchemaProvider extends XmlSchemaProvider {
    public static List<String> xsdFileRootTags = Arrays.asList("CommonDef", "DataMigration", "DataObject", "Entry", "Form", "Map", "DataElementDef", "DomainDef");

    @Override
    public @Nullable XmlFile getSchema(@NotNull @NonNls String s, com.intellij.openapi.module.@Nullable Module module, @NotNull PsiFile psiFile) {
        if (psiFile instanceof XmlFile xmlFile) {
            final Project project = psiFile.getProject();
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag != null) {
                if (xsdFileRootTags.contains(rootTag.getName())) {
                    Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName(rootTag.getName() + ".xsd", GlobalSearchScope.projectScope(project));
                    if (!virtualFiles.isEmpty()) {
                        return (XmlFile) PsiManager.getInstance(project).findFile(virtualFiles.iterator().next());
                    }
                }
            }

        }
        return null;
    }

    /**
     * This is the "trigger". IDEA calls this method for every XML file to ask
     * if our provider should handle it.
     *
     * @param file The XML file being checked.
     * @return true if the root tag is <Form>, false otherwise.
     */
    @Override
    public boolean isAvailable(@NotNull XmlFile file) {
        final XmlTag rootTag = file.getRootTag();
        // Check if the file has a root tag and if its name is "Form".
        // This is how we identify the XMLs that should use our schema.
        return rootTag != null && xsdFileRootTags.contains(rootTag.getName());
    }
}