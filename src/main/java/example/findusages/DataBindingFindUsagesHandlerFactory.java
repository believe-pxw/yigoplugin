package example.findusages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

public class DataBindingFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

    @Override
    public boolean canFindUsages(@NotNull PsiElement element) {
        return element instanceof XmlAttributeValue &&
                isDataBindingColumnKey(element);
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
        return new DataBindingFindUsagesHandler(element);
    }

    private boolean isDataBindingColumnKey(PsiElement element) {
        XmlTag tag =(XmlTag) element.getParent().getParent();
        if (tag.getName().equals("Column")) {
            return true;
        }
        return false;
    }
}