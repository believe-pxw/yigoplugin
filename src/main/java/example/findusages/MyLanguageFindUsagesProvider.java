package example.findusages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.TokenSet;
import example.MyLanguageLexerAdapter;
import example.psi.MyLanguageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MyLanguageFindUsagesProvider implements FindUsagesProvider {

    // 定义哪些 token 是标识符、注释和字符串字面量，用于构建词语索引
    private static final TokenSet IDENTIFIERS = TokenSet.create(MyLanguageTypes.IDENTIFIER, MyLanguageTypes.MACRO_IDENTIFIER);
    private static final TokenSet COMMENTS = TokenSet.EMPTY; // 你的语法中没有明确的注释，如果需要可以添加
    private static final TokenSet LITERALS = TokenSet.create(MyLanguageTypes.SINGLE_QUOTED_STRING, MyLanguageTypes.DOUBLE_QUOTED_STRING, MyLanguageTypes.NUMBER);

    @Override
    public @Nullable WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(new MyLanguageLexerAdapter(), IDENTIFIERS, COMMENTS, LITERALS);
    }

    // 判断给定 PSI 元素是否支持查找用法
    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        // 我们主要关注 PsiNamedElement，特别是我们语法中的 IDENTIFIER，它通常代表变量名
        return psiElement instanceof PsiNamedElement &&
                (psiElement.getNode().getElementType() == MyLanguageTypes.IDENTIFIER ||
                        psiElement.getNode().getElementType() == MyLanguageTypes.MACRO_IDENTIFIER);
    }

    // 获取元素类型，用于在 "Find Usages" 结果窗口中显示
    @Override
    public @Nullable String getHelpId(@NotNull PsiElement psiElement) {
        return null; // 通常不需要自定义帮助ID
    }

    // 返回元素类型的描述性名称（例如 "variable", "function"）
    @Override
    public @NotNull String getType(@NotNull PsiElement element) {
        if (element.getNode().getElementType() == MyLanguageTypes.IDENTIFIER) {
            // 根据上下文判断是变量声明还是引用
            // 这里我们简化处理，都称为 "Variable"
            return "Variable";
        }
        if (element.getNode().getElementType() == MyLanguageTypes.MACRO_IDENTIFIER) {
            return "Macro";
        }
        return "";
    }

    // 返回元素的描述性名称，通常是元素的名称本身
    @Override
    public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
        if (element instanceof PsiNamedElement) {
            return ((PsiNamedElement) element).getName();
        }
        return element.getText();
    }

    // 返回元素的文本表示，用于在 "Find Usages" 结果中显示
    @Override
    public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        return element.getText();
    }
}