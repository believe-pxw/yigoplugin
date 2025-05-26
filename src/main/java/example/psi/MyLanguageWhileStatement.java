// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageWhileStatement extends PsiElement {

  @Nullable
  MyLanguageBlockStatement getBlockStatement();

  @NotNull
  MyLanguageExpression getExpression();

  @Nullable
  MyLanguageExpressionSequence getExpressionSequence();

  @Nullable
  MyLanguageIfStatement getIfStatement();

  @Nullable
  MyLanguageVariableAssignment getVariableAssignment();

  @Nullable
  MyLanguageVariableDeclaration getVariableDeclaration();

  @Nullable
  MyLanguageWhileStatement getWhileStatement();

}
