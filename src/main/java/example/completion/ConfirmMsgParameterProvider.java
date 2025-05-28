package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

class ConfirmMsgParameterProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        // 检查是否在第三个参数位置
        if (isThirdParameter(parameters.getPosition())) {
            // 提供双大括号参数格式
            result.addElement(LookupElementBuilder.create("{{GetValue('')}}")
                    .withPresentableText("{{GetValue('')}}")
                    .withTypeText("parameter template")
                    .withInsertHandler((context2, item) -> {
                        // 移动光标到引号内
                        context2.getEditor().getCaretModel()
                                .moveToOffset(context2.getTailOffset() - 3);
                    }));
        }

        // 提供样式选项（第四个参数）
        if (isFourthParameter(parameters.getPosition())) {
            String[] styles = {"'OK'", "'YES_NO'", "'YES_NO_CANCEL'"};
            for (String style : styles) {
                result.addElement(LookupElementBuilder.create(style)
                        .withTypeText("style option"));
            }
        }
    }

    private boolean isThirdParameter(PsiElement element) {
        // 实现参数位置检测逻辑
        return true; // 简化实现
    }

    private boolean isFourthParameter(PsiElement element) {
        // 实现参数位置检测逻辑
        return true; // 简化实现
    }
}