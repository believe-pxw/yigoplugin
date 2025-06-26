package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        @NotNull PsiFile[] psiFile = FilenameIndex.getFilesByName(myElement.getProject(), "ParaTable.xml", GlobalSearchScope.projectScope(project));
        for (PsiFile file : psiFile) {
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