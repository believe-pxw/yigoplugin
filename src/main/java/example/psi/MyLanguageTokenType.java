package example.psi;

import com.intellij.psi.tree.IElementType;
import example.MyLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MyLanguageTokenType extends IElementType {
    public MyLanguageTokenType(@NotNull @NonNls String debugName) {
        super(debugName, MyLanguage.INSTANCE); // 这里的 MyLanguage.INSTANCE 应该是你的语言实例
    }

    // 可以在这里重写 toString() 方法以便调试
    @Override
    public String toString() {
        return "MyLanguageTokenType." + super.toString();
    }
}
