package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ProcessingContext;
import example.psi.MyLanguageVariableDeclaration;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class SmartCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        Project project = parameters.getOriginalFile().getProject();

        // 基于索引的智能补全
        addVariablesFromIndex(parameters,project, result);
        addFunctionsFromIndex(project, result);

        // 基于当前文件上下文的补全
        addContextualCompletions(parameters, result);
    }

    private void addVariablesFromIndex(CompletionParameters parameters,Project project, CompletionResultSet result) {
        // 使用StubIndex获取项目中所有变量
        PsiFile originalFile = parameters.getOriginalFile();
        result.addElement(LookupElementBuilder.create("test")
                .withTypeText("variable")
                .withIcon(Icons.VARIABLE_ICON));
    }

    private void addFunctionsFromIndex(Project project, CompletionResultSet result) {
        // 类似地添加函数补全
        // 这里可以扫描所有可用的函数
    }

    private void addContextualCompletions(CompletionParameters parameters,
                                          CompletionResultSet result) {
        PsiElement position = parameters.getPosition();

        // 根据当前位置的上下文提供不同的补全
        if (isInConfirmMsgContext(position)) {
            addConfirmMsgSpecificCompletions(result);
        } else if (isInExpressionContext(position)) {
            addExpressionCompletions(result);
        }
    }

    private boolean isInConfirmMsgContext(PsiElement position) {
        // 检查是否在ConfirmMsg函数调用内
        PsiElement parent = position.getParent();
        while (parent != null) {
            if (parent.getText().startsWith("ConfirmMsg")) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private boolean isInExpressionContext(PsiElement position) {
        // 检查是否在表达式上下文中
        return true; // 简化实现
    }

    private void addConfirmMsgSpecificCompletions(CompletionResultSet result) {
        // 为ConfirmMsg提供特定的补全项
        result.addElement(LookupElementBuilder.create("GetValue('')")
                .withPresentableText("GetValue")
                .withTypeText("function")
                .withInsertHandler((context, item) -> {
                    context.getEditor().getCaretModel()
                            .moveToOffset(context.getTailOffset() - 2);
                }));
    }

    private void addExpressionCompletions(CompletionResultSet result) {
        // 为表达式上下文提供补全
        String[] operators = {"+", "-", "*", "/", "==", "!=", "&&", "||"};
        for (String op : operators) {
            result.addElement(LookupElementBuilder.create(op)
                    .withTypeText("operator"));
        }
    }
}