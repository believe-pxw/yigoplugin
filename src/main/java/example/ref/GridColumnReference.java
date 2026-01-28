package example.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GridColumnReference extends PsiReferenceBase<PsiElement> {

    private final String key;

    public GridColumnReference(@NotNull PsiElement element, TextRange rangeInElement, String key) {
        super(element, rangeInElement);
        // The attribute value is "SD_DateCategory", so we extract it.
        this.key = key;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return findTagByKeyRecursive(Objects.requireNonNull(((XmlFile) myElement.getContainingFile()).getRootTag()), "GridCell", key);
    }

    private PsiElement findTagByKeyRecursive(XmlTag currentTag, String tagName, String key) {
        if (currentTag.getLocalName().equals(tagName) && Objects.equals(currentTag.getAttributeValue("Key"), key)) {
            return Objects.requireNonNull(currentTag.getAttribute("Key")).getLastChild();
        }
        for (XmlTag subTag : currentTag.getSubTags()) {
            PsiElement found = findTagByKeyRecursive(subTag, tagName, key);
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}
