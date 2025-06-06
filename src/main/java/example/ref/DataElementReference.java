package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import example.doc.ParaTableDocumentationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataElementReference extends PsiReferenceBase<XmlAttributeValue> {

    private final String dataElementKey;

    public DataElementReference(@NotNull XmlAttributeValue element, TextRange rangeInElement) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.dataElementKey = element.getValue();
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = myElement.getProject();
        PsiElement psi = ParaTableDocumentationProvider.getDataElementPsi(project, dataElementKey);
        if (psi == null) {
            return myElement;
        }
        return psi;
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}