package example.findusages;

import com.github.believepxw.yigo.ref.VariableReference;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

public class FieldFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

    @Override
    public boolean canFindUsages(@NotNull PsiElement element) {
        return element instanceof XmlAttributeValue &&
                isFieldKey(element);
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
        return new FieldFindUsagesHandler(element);
    }

    private boolean isFieldKey(PsiElement element) {
        PsiElement parent = element.getParent().getParent();
        if(parent instanceof XmlTag tag) {
            if (VariableReference.Companion.getVariableDefinitionTagNames().contains(tag.getName())) {
                return true;
            }
        }
        return false;
    }
}