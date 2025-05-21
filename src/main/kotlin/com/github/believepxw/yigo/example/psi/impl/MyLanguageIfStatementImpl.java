// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import java.util.List;

import com.github.believepxw.yigo.example.psi.MyLanguageExpression;
import com.github.believepxw.yigo.example.psi.MyLanguageExpressionStatement;
import com.github.believepxw.yigo.example.psi.MyLanguageIfStatement;
import com.github.believepxw.yigo.example.psi.MyLanguageVariableDeclaration;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import com.github.believepxw.yigo.example.psi.MyLanguageWhileStatement;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

public class MyLanguageIfStatementImpl extends ASTWrapperPsiElement implements MyLanguageIfStatement {

  public MyLanguageIfStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitIfStatement(this);
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
  @NotNull
  public List<MyLanguageExpressionStatement> getExpressionStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageExpressionStatement.class);
  }

  @Override
  @NotNull
  public List<MyLanguageIfStatement> getIfStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageIfStatement.class);
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
