package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import example.doc.ParaTableDocumentationProvider;
import example.index.DataElementIndex;
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
        PsiElement psi = DataElementIndex.findDEDefinition(project, dataElementKey);
        if (psi == null) {
            return null;
        }
        return ((XmlTag) psi).getAttribute("Key").getValueElement();
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}