package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import example.doc.ParaTableDocumentationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DomainReference extends PsiReferenceBase<XmlAttributeValue> {

    private final String domainKey;

    public DomainReference(@NotNull XmlAttributeValue element, TextRange rangeInElement) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.domainKey = element.getValue();
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = myElement.getProject();
        PsiElement domainPsi = ParaTableDocumentationProvider.getDomainPsi(project, domainKey);
        if (domainPsi == null) {
            return null;
        }
        return ((XmlTag) domainPsi).getAttribute("Key").getValueElement();
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}