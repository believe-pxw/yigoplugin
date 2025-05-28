package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import example.psi.MyLanguageVariableDeclaration;
import org.jetbrains.annotations.NotNull;

// 4. 变量补全提供者
class VariableCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        PsiElement element = parameters.getPosition();

        // 获取当前作用域内的变量
        collectVariablesInScope(element, result);
    }

    private void collectVariablesInScope(PsiElement element, CompletionResultSet result) {
        // 遍历AST树，收集变量声明
        PsiElement current = element;
        while (current != null) {
            if (current instanceof MyLanguageVariableDeclaration) {
                MyLanguageVariableDeclaration varDecl = (MyLanguageVariableDeclaration) current;
                String varName = varDecl.getIdentifier().getText();

                result.addElement(LookupElementBuilder.create(varName)
                        .withTypeText("variable")
                        .withIcon(Icons.VARIABLE_ICON));
            }
            current = current.getParent();
        }
    }
}