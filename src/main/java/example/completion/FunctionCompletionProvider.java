package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

// 3. 函数补全提供者
class FunctionCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        // ConfirmMsg函数补全
        result.addElement(LookupElementBuilder.create("ConfirmMsg")
                .withPresentableText("ConfirmMsg(code, text, params, style, callback)")
                .withTypeText("function")
                .withIcon(Icons.FUNCTION_ICON)
                .withInsertHandler(new ConfirmMsgInsertHandler()));

        // IIF函数补全
        result.addElement(LookupElementBuilder.create("IIF")
                .withPresentableText("IIF(condition, trueValue, falseValue)")
                .withTypeText("function")
                .withIcon(Icons.FUNCTION_ICON)
                .withInsertHandler(new IIFInsertHandler()));

        // Macro函数补全
        addMacroCompletions(result, parameters);
    }

    private void addMacroCompletions(CompletionResultSet result, CompletionParameters parameters) {
        // 动态获取可用的Macro函数
        String[] macros = {
                "Macro_UIOpt_BillTemporarySave_Exp_Formula",
                "Macro_GetCurrentUser",
                "Macro_GetCurrentDate"
        };

        for (String macro : macros) {
            result.addElement(LookupElementBuilder.create(macro)
                    .withTypeText("macro")
                    .withIcon(Icons.MACRO_ICON));
        }
    }
}