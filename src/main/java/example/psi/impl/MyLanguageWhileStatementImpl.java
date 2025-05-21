// This is a generated file. Not intended for manual editing.
package example.psi.impl;

import example.psi.MyLanguageExpression;
import example.psi.MyLanguageExpressionStatement;
import example.psi.MyLanguageIfStatement;
import example.psi.MyLanguageVariableDeclaration;
import example.psi.MyLanguageVisitor;
import example.psi.MyLanguageWhileStatement;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import example.psi.*;

public class MyLanguageWhileStatementImpl extends ASTWrapperPsiElement implements MyLanguageWhileStatement {

  public MyLanguageWhileStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitWhileStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public MyLanguageExpression getExpression() {
    return findNotNullChildByClass(MyLanguageExpression.class);
  }

  @Override
  @Nullable
  public MyLanguageExpressionStatement getExpressionStatement() {
    return findChildByClass(MyLanguageExpressionStatement.class);
  }

  @Override
  @Nullable
  public MyLanguageIfStatement getIfStatement() {
    return findChildByClass(MyLanguageIfStatement.class);
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
