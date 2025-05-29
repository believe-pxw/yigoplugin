package example.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import example.psi.MyLanguageTypes;

public class MyLanguageCompletionContributor extends CompletionContributor {

    public MyLanguageCompletionContributor() {
        // 关键字补全
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(MyLanguageTypes.IDENTIFIER),
                new KeywordCompletionProvider());

        // 函数补全
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .withParent(PlatformPatterns.psiElement(MyLanguageTypes.IDENTIFIER)),
                new FunctionCompletionProvider());

        // 变量补全
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(MyLanguageTypes.IDENTIFIER),
                new VariableCompletionProvider());
    }
}