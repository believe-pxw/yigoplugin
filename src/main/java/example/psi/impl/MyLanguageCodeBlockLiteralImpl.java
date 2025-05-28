// This is a generated file. Not intended for manual editing.
package example.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static example.psi.MyLanguageTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import example.psi.*;

public class MyLanguageCodeBlockLiteralImpl extends ASTWrapperPsiElement implements MyLanguageCodeBlockLiteral {

  public MyLanguageCodeBlockLiteralImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitCodeBlockLiteral(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<MyLanguageExpressionSequence> getExpressionSequenceList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageExpressionSequence.class);
  }

  @Override
  @NotNull
  public List<MyLanguageIfStatement> getIfStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageIfStatement.class);
  }

  @Override
  @NotNull
  public List<MyLanguageVariableAssignment> getVariableAssignmentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageVariableAssignment.class);
  }

  @Override
  @NotNull
  public List<MyLanguageVariableDeclaration> getVariableDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageVariableDeclaration.class);
  }

  @Override
  @NotNull
  public List<MyLanguageWhileStatement> getWhileStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageWhileStatement.class);
  }

}
