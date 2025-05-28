package example.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import org.jetbrains.annotations.NotNull;

class IIFInsertHandler implements InsertHandler<LookupElement> {

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        String template = "(, , )";
        context.getDocument().insertString(context.getTailOffset(), template);
        context.getEditor().getCaretModel().moveToOffset(context.getTailOffset() - template.length() + 1);
    }
}