package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class ParaGroupReference extends PsiReferenceBase<PsiElement> {

    private final String paraGroupKey;

    public ParaGroupReference(@NotNull PsiElement element, TextRange rangeInElement, String paraGroupKey) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.paraGroupKey = paraGroupKey;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = myElement.getProject();
        Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName("ParaTable.xml", GlobalSearchScope.projectScope(project));
        for (VirtualFile virtualFile: virtualFiles) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            XmlFile xmlFile = (XmlFile) file;
            XmlTag rootTag = xmlFile.getRootTag();
            for (XmlTag subTag : rootTag.getSubTags()) {
                if (subTag.getAttributeValue("Key").equals(paraGroupKey)) {
                    return subTag.getAttribute("Key").getValueElement();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}