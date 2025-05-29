package example.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

class MethodInsertHandler implements InsertHandler<LookupElement> {
    private final PsiMethod method;

    public MethodInsertHandler(PsiMethod method) {
        this.method = method;
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        Editor editor = context.getEditor();
        Document document = editor.getDocument();
        int offset = context.getTailOffset();

        // 插入方法调用的括号
        document.insertString(offset, "()");

        // 如果方法有参数，将光标移到括号内
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length > 0) {
            editor.getCaretModel().moveToOffset(offset + 1);
        } else {
            editor.getCaretModel().moveToOffset(offset + 2);
        }
    }
}