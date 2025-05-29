package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        XmlFile hostXmlFile = getHostXmlFile(element);
        if (hostXmlFile == null) {
            return;
        }
        // 遍历AST树，收集变量声明
        XmlTag rootTag = hostXmlFile.getRootTag();
        ArrayList<String> vars = new ArrayList<>();
        if (rootTag != null) {
            findVariableDefinitionRecursive(rootTag, vars);
            for (String var : vars) {
                result.addElement(LookupElementBuilder.create(var)
                        .withTypeText("variable")
                        .withIcon(Icons.VARIABLE_ICON));
            }
        }
    }

    public static XmlFile getHostXmlFile(PsiElement element) {
        InjectedLanguageManager manager = InjectedLanguageManager.getInstance(element.getProject());
        PsiFile hostFile = manager.getTopLevelFile(element);
        return hostFile instanceof XmlFile ? (XmlFile) hostFile : null;
    }

    Set<String> variableDefinitionTagNames = new HashSet<>((
            Arrays.asList(
                    "Dict", "DynamicDict", "TextEditor", "TextArea", "CheckBox", "ComboBox",
                    "CheckListBox", "DatePicker", "UTCDatePicker", "MonthPicker", "TimePicker",
                    "Button", "NumberEditor", "Label", "TextButton", "RadioButton", "PasswordEditor",
                    "Image", "WebBrowser", "RichEditor", "HyperLink", "Separator", "DropdownButton",
                    "Icon", "Custom", "BPMGraph", "Dynamic", "Carousel", "EditView", "Gantt", // Existing usage tags
                    "Variable", "VarDef", "GridCell" // Add common variable definition tag names if they are different
            )
    ));

    private void findVariableDefinitionRecursive(XmlTag currentTag, List<String> vars){
        // Check if the current tag is a variable definition itself.
        // Assuming variable definitions have a 'name' attribute and are among the specified tags.
        // If your variable declarations use a different attribute (e.g., 'id'), change "name" below.
        if (variableDefinitionTagNames.contains(currentTag.getLocalName())) {
            vars.add(currentTag.getAttributeValue("Key"));
        }
        // Recursively check all sub-tags
        XmlTag[] subTags = currentTag.getSubTags();
        for (XmlTag subTag : subTags) {
            findVariableDefinitionRecursive(subTag, vars);
        }

    }
}