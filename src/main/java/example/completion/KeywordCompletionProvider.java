package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

class KeywordCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final String[] KEYWORDS = {
            "if", "else", "while", "var", "true", "false", "parent", "container"
    };

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        for (String keyword : KEYWORDS) {
            result.addElement(LookupElementBuilder.create(keyword)
                    .withBoldness(true)
                    .withTypeText("keyword")
                    .withIcon(Icons.KEYWORD_ICON));
        }
    }
}