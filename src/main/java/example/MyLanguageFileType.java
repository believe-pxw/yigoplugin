package example;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MyLanguageFileType extends LanguageFileType {
    public static final MyLanguageFileType INSTANCE = new MyLanguageFileType();

    private MyLanguageFileType() {
        super(MyLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "MyLanguage File"; // 在文件类型选择中显示的名字
    }

    @NotNull
    @Override
    public String getDescription() {
        return "My custom language file"; // 文件类型的描述
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "mylang"; // 你的语言文件的默认扩展名，例如 .mylang
    }

    @Nullable
    @Override
    public Icon getIcon() {
        // return MyPluginIcons.MY_LANGUAGE_FILE_ICON; // 返回你的文件图标
        return null; // 或者使用默认图标
    }
}