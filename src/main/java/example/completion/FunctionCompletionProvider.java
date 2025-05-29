package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import example.index.MacroNameIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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
        PsiElement element = parameters.getPosition();
        XmlFile hostXmlFile = VariableCompletionProvider.getHostXmlFile(element);
        if (hostXmlFile == null) {
            registerCommonDefMacro(result, element.getProject());
            return;
        }
        if (hostXmlFile.getRootTag() == null) {
            registerCommonDefMacro(result, element.getProject());
            return;
        }
        XmlTag macroCollectionTag = hostXmlFile.getRootTag().findFirstSubTag("MacroCollection");
        if (macroCollectionTag != null) {
            for (XmlTag macroTag : macroCollectionTag.findSubTags("Macro")) {
                String keyAttribute = macroTag.getAttributeValue("Key");
                result.addElement(LookupElementBuilder.create(keyAttribute)
                        .withTypeText("macro")
                        .withIcon(Icons.MACRO_ICON));
            }
        }
        registerCommonDefMacro(result, element.getProject());
    }

    public static void registerCommonDefMacro(CompletionResultSet result, Project project) {
        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(MacroNameIndex.KEY, project);
        for (String allKey : allKeys) {
            result.addElement(LookupElementBuilder.create(allKey)
                    .withTypeText("Macro in CommonDef")
                    .withIcon(Icons.MACRO_ICON));
        }
    }
}