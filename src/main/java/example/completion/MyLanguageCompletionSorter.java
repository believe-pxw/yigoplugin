package example.completion;


import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import org.jetbrains.annotations.NotNull;

// 补全结果排序和过滤
public class MyLanguageCompletionSorter extends CompletionSorter {

    public static CompletionSorter create() {
        return CompletionSorter.emptySorter()
                .weigh(new LookupElementWeigher("mylang.priority") {
                    @Override
                    public Integer weigh(@NotNull LookupElement element) {
                        String lookupString = element.getLookupString();

                        // 关键字优先级最高
                        if (isKeyword(lookupString)) return 100;

                        // 函数次之
                        if (isFunction(lookupString)) return 90;

                        // 变量最后
                        if (isVariable(lookupString)) return 80;

                        return 0;
                    }

                    private boolean isKeyword(String text) {
                        return text.matches("if|else|while|var|true|false");
                    }

                    private boolean isFunction(String text) {
                        return text.contains("(") || text.startsWith("Macro_");
                    }

                    private boolean isVariable(String text) {
                        return text.matches("[a-zA-Z_][a-zA-Z0-9_]*");
                    }
                });
    }

    @Override
    public CompletionSorter weighBefore(@NotNull String beforeId, LookupElementWeigher... weighers) {
        return null;
    }

    @Override
    public CompletionSorter weighAfter(@NotNull String afterId, LookupElementWeigher... weighers) {
        return null;
    }

    @Override
    public CompletionSorter weigh(LookupElementWeigher weigher) {
        return null;
    }
}