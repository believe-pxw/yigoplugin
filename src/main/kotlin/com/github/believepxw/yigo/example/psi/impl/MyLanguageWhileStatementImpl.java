// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import com.github.believepxw.yigo.example.psi.MyLanguageExpression;
import com.github.believepxw.yigo.example.psi.MyLanguageExpressionStatement;
import com.github.believepxw.yigo.example.psi.MyLanguageIfStatement;
import com.github.believepxw.yigo.example.psi.MyLanguageVariableDeclaration;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import com.github.believepxw.yigo.example.psi.MyLanguageWhileStatement;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

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
