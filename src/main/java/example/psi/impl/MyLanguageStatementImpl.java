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

public class MyLanguageStatementImpl extends ASTWrapperPsiElement implements MyLanguageStatement {

  public MyLanguageStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public MyLanguageComment getComment() {
    return findChildByClass(MyLanguageComment.class);
  }

  @Override
  @Nullable
  public MyLanguageExpressionSequence getExpressionSequence() {
    return findChildByClass(MyLanguageExpressionSequence.class);
  }

  @Override
  @Nullable
  public MyLanguageIfStatement getIfStatement() {
    return findChildByClass(MyLanguageIfStatement.class);
  }

  @Override
  @Nullable
  public MyLanguageReturnStatement getReturnStatement() {
    return findChildByClass(MyLanguageReturnStatement.class);
  }

  @Override
  @Nullable
  public MyLanguageVariableAssignment getVariableAssignment() {
    return findChildByClass(MyLanguageVariableAssignment.class);
  }

  @Override
  @Nullable
  public MyLanguageVariableDeclaration getVariableDeclaration() {
    return findChildByClass(MyLanguageVariableDeclaration.class);
  }

  @Override
  @Nullable
  public MyLanguageWhileStatement getWhileStatement() {
    return findChildByClass(MyLanguageWhileStatement.class);
  }

}
