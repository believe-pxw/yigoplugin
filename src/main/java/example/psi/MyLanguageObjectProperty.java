// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageObjectProperty extends PsiElement {

  @Nullable
  MyLanguageCodeBlockLiteral getCodeBlockLiteral();

  @Nullable
  MyLanguageExpression getExpression();

  @Nullable
  MyLanguageObjectLiteral getObjectLiteral();

  @NotNull
  PsiElement getIdentifier();

}
