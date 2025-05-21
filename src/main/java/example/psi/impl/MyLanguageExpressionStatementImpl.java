// This is a generated file. Not intended for manual editing.
package example.psi.impl;

import example.psi.MyLanguageExpression;
import example.psi.MyLanguageExpressionStatement;
import example.psi.MyLanguageVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import example.psi.*;

public class MyLanguageExpressionStatementImpl extends ASTWrapperPsiElement implements MyLanguageExpressionStatement {

  public MyLanguageExpressionStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitExpressionStatement(this);
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

}
