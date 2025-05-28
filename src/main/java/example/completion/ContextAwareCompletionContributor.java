package example.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import example.psi.MyLanguageTypes;
// 6. 高级补全：上下文感知
public class ContextAwareCompletionContributor extends CompletionContributor {

    public ContextAwareCompletionContributor() {
        // 在ConfirmMsg的第三个参数位置提供特殊补全
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .inside(PlatformPatterns.psiElement(MyLanguageTypes.CONFIRM_MSG_CALL))
                        .afterLeaf(PlatformPatterns.psiElement(MyLanguageTypes.COMMA)),
                new ConfirmMsgParameterProvider());

        // 在对象字面量中提供回调函数补全
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .inside(PlatformPatterns.psiElement(MyLanguageTypes.CALLBACK_OBJECT)),
                new CallbackCompletionProvider());
    }
}