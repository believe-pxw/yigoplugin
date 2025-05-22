package example.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MacroReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private final String macroName;

    public MacroReference(@NotNull PsiElement element,TextRange textRange, String macroName) {
        super(element, textRange);
        this.macroName = macroName;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        // 直接获取当前文件
        PsiFile containingFile = myElement.getContainingFile();

        if (containingFile instanceof XmlFile) { // 确保是XML文件
            XmlFile currentXmlFile = (XmlFile) containingFile;
            // 在当前XML文件中查找宏定义
            PsiElement macroTag = findMacroTagInFile(currentXmlFile, macroName);
            if (macroTag != null) {
                return new ResolveResult[]{new PsiElementResolveResult(macroTag)};
            }else{
                XmlTag macroDefinition = MacroNameIndex.findMacroDefinition(myElement.getProject(), macroName);
                if (macroDefinition != null) {
                    return new ResolveResult[]{new PsiElementResolveResult(macroDefinition)};
                }
            }
        }
        return ResolveResult.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0];
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return super.handleElementRename(newElementName);
    }

    // --- 自定义查找宏定义的方法 (保持不变) ---

    // 示例：在 XML 文件中查找指定的宏标签
    private PsiElement findMacroTagInFile(XmlFile xmlFile, String macroName) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) return null;

        // 查找 MacroCollection
        XmlTag macroCollectionTag = rootTag.findFirstSubTag("MacroCollection");
        if (macroCollectionTag != null) {
            for (XmlTag macroTag : macroCollectionTag.findSubTags("Macro")) {
                String keyAttribute = macroTag.getAttributeValue("Key");
                if (keyAttribute != null && keyAttribute.equals(macroName)) {
                    return macroTag.getAttribute("Key").getLastChild(); // 找到匹配的 <Macro> 标签
                }
            }
        }
        return null;
    }
}