// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageCodeBlockLiteral extends PsiElement {

  @NotNull
  List<MyLanguageExpressionSequence> getExpressionSequenceList();

  @NotNull
  List<MyLanguageIfStatement> getIfStatementList();

  @NotNull
  List<MyLanguageVariableAssignment> getVariableAssignmentList();

  @NotNull
  List<MyLanguageVariableDeclaration> getVariableDeclarationList();

  @NotNull
  List<MyLanguageWhileStatement> getWhileStatementList();

}
