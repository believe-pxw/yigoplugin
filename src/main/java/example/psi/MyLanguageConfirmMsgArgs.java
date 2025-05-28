// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageConfirmMsgArgs extends PsiElement {

  @Nullable
  MyLanguageCallbackObject getCallbackObject();

  @NotNull
  List<MyLanguageExpression> getExpressionList();

  @Nullable
  MyLanguageMessageParamsExpression getMessageParamsExpression();

}
