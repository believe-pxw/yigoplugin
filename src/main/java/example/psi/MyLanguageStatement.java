// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageStatement extends PsiElement {

  @Nullable
  MyLanguageComment getComment();

  @Nullable
  MyLanguageExpressionSequence getExpressionSequence();

  @Nullable
  MyLanguageIfStatement getIfStatement();

  @Nullable
  MyLanguageReturnStatement getReturnStatement();

  @Nullable
  MyLanguageVariableAssignment getVariableAssignment();

  @Nullable
  MyLanguageVariableDeclaration getVariableDeclaration();

  @Nullable
  MyLanguageWhileStatement getWhileStatement();

}
