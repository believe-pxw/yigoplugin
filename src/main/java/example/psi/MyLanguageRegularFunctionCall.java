// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageRegularFunctionCall extends PsiElement {

  @Nullable
  MyLanguageArgumentList getArgumentList();

  @Nullable
  MyLanguageIifFunctionCall getIifFunctionCall();

  @Nullable
  MyLanguageJavaMethodCall getJavaMethodCall();

  @Nullable
  MyLanguageMacroCallExpression getMacroCallExpression();

  @Nullable
  PsiElement getContainerKeyword();

  @Nullable
  PsiElement getIdentifier();

  @Nullable
  PsiElement getParentKeyword();

}
