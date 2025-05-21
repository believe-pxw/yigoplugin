// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import com.github.believepxw.yigo.example.psi.MyLanguageExpression;
import com.github.believepxw.yigo.example.psi.MyLanguageExpressionStatement;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

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
