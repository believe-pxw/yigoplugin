package example.ref;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import example.index.FormIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FormReference extends PsiReferenceBase<PsiElement> {

    private final String formKey;

    public FormReference(@NotNull PsiElement element, TextRange rangeInElement,String formKey) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.formKey = formKey;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = myElement.getProject();
        return FormIndex.findFormDefinition(project, formKey);
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}