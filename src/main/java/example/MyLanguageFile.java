package example;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class MyLanguageFile extends PsiFileBase {
    public MyLanguageFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MyLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return MyLanguageFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "MyLanguage File";
    }
}
