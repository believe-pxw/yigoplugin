// This is a generated file. Not intended for manual editing.
package example.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageFunctionCall extends PsiElement {

  @Nullable
  MyLanguageArgumentList getArgumentList();

  @Nullable
  MyLanguageJavaMethodCall getJavaMethodCall();

  @Nullable
  MyLanguageParentCall getParentCall();

  @Nullable
  PsiElement getIdentifier();

  @Nullable
  PsiElement getMacroIdentifier();

}
