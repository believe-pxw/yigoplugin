// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguagePrimaryExpression extends PsiElement {

  @Nullable
  MyLanguageConstant getConstant();

  @Nullable
  MyLanguageExpression getExpression();

  @Nullable
  MyLanguageFunctionCall getFunctionCall();

  @Nullable
  MyLanguageVariableReference getVariableReference();

}
