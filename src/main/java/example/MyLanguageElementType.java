package example;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MyLanguageElementType extends IElementType {
    public MyLanguageElementType(@NotNull @NonNls String debugName) {
        super(debugName, MyLanguage.INSTANCE); // 这里的 MyLanguage.INSTANCE 应该是你的语言实例
    }
}
