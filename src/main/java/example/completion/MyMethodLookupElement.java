package example.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import example.completion.func.MethodDetails;
import example.completion.func.ParameterInfo;
import org.jetbrains.annotations.NotNull;

public class MyMethodLookupElement extends LookupElement {

    private final MethodDetails methodDetails;
    private final String lookupString;

    public MyMethodLookupElement(@NotNull MethodDetails methodDetails) {
        this.methodDetails = methodDetails;
        this.lookupString = methodDetails.getKey();
    }

    @NotNull
    @Override
    public String getLookupString() {
        return lookupString; // LookupString用于匹配，通常是短名
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        // 设置主文本（代码完成列表中显示的主要内容）
        presentation.setItemText(methodDetails.getTemplate()); // 显示方法名
        // 设置尾部文本（通常用于显示参数或返回类型）
        StringBuilder tailText = new StringBuilder("(");
        if (methodDetails.getParas() != null) {
            for (int i = 0; i < methodDetails.getParas().size(); i++) {
                ParameterInfo param = methodDetails.getParas().get(i);
                tailText.append(param.getKey()); // 在尾部文本中显示参数名
                if (i < methodDetails.getParas().size() - 1) {
                    tailText.append(", ");
                }
            }
        }
        tailText.append(")");
        if (methodDetails.getReturnValueType() != null && !methodDetails.getReturnValueType().isEmpty()) {
            tailText.append(" : ").append(methodDetails.getReturnValueType());
        }
        presentation.setTailText(tailText.toString());

        // 设置类型文本（通常用于显示方法描述）
        presentation.setTypeText(methodDetails.getDesc());
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), methodDetails.getTemplate());
        context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
    }
}