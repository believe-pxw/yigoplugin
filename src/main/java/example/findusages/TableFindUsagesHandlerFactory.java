package example.findusages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

public class TableFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

    @Override
    public boolean canFindUsages(@NotNull PsiElement element) {
        return element instanceof XmlAttributeValue &&
                isTableDefination(element);
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
        return new TableFindUsagesHandler(element);
    }

    private boolean isTableDefination(PsiElement element) {
        PsiElement parent = element.getParent().getParent();
        if (parent instanceof XmlTag) {
            XmlTag tag =(XmlTag) parent;
            if (tag.getName().equals("Table")) {
                return true;
            }
        }
        return false;
    }
}