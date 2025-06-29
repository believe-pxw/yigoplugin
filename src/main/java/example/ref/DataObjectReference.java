package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import example.index.DataObjectIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataObjectReference extends PsiReferenceBase<PsiElement> {

    private final String dataObjectKey;

    public DataObjectReference(@NotNull PsiElement element, TextRange rangeInElement,String dataObjectKey) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.dataObjectKey = dataObjectKey;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = myElement.getProject();
        return DataObjectIndex.findDataObjectDefinition(project, dataObjectKey);
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}