package example.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import example.MyLanguageFile;
import org.jetbrains.annotations.NotNull;

public class MyLanguageLiveTemplateContext extends TemplateContextType {

    public MyLanguageLiveTemplateContext() {
        super("MyLanguage", "MyLanguage");
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
        PsiFile file = templateActionContext.getFile();
        return file instanceof MyLanguageFile;
    }
}