package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

class CallbackCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        String[] callbackKeys = {"OK", "YES", "NO", "CANCEL"};

        for (String key : callbackKeys) {
            result.addElement(LookupElementBuilder.create(key + ": {}")
                    .withPresentableText(key)
                    .withTypeText("callback")
                    .withInsertHandler((insertContext, item) -> {
                        // 移动光标到大括号内
                        insertContext.getEditor().getCaretModel()
                                .moveToOffset(insertContext.getTailOffset() - 1);
                    }));
        }
    }
}