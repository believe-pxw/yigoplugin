package example;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import example.psi.MyLanguageTypes;
import org.jetbrains.annotations.NotNull;

public class MyLanguageReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // 注册对 MACRO_IDENTIFIER 元素的引用解析
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(MyLanguageTypes.MACRO_IDENTIFIER), // 匹配宏标识符的 PSI 元素
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                        // 确保元素是 Macro_IDENTIFIER 类型
                        if (element.getNode().getElementType() == MyLanguageTypes.MACRO_IDENTIFIER) {
                            String macroName = element.getText();
                            // 返回一个 MacroReference 实例
                            return new PsiReference[]{new MacroReference(element, macroName)};
                        }
                        return PsiReference.EMPTY_ARRAY;
                    }
                });
    }
}