package example.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class FormDefinitionReference extends PsiReferenceBase<PsiElement> {

    private final String formKey;

    public FormDefinitionReference(@NotNull PsiElement element, TextRange rangeInElement, String formKey) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.formKey = formKey;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return myElement;
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}