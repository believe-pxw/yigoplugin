package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
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
            return myElement;
        }
        return domainPsi;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}