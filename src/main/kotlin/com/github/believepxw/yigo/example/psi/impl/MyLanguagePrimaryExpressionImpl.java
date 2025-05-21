// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import com.github.believepxw.yigo.example.psi.MyLanguageConstant;
import com.github.believepxw.yigo.example.psi.MyLanguageExpression;
import com.github.believepxw.yigo.example.psi.MyLanguageFunctionCall;
import com.github.believepxw.yigo.example.psi.MyLanguagePrimaryExpression;
import com.github.believepxw.yigo.example.psi.MyLanguageVariableReference;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

public class MyLanguagePrimaryExpressionImpl extends ASTWrapperPsiElement implements MyLanguagePrimaryExpression {

  public MyLanguagePrimaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitPrimaryExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public MyLanguageConstant getConstant() {
    return findChildByClass(MyLanguageConstant.class);
  }

  @Override
  @Nullable
  public MyLanguageExpression getExpression() {
    return findChildByClass(MyLanguageExpression.class);
  }

  @Override
  @Nullable
  public MyLanguageFunctionCall getFunctionCall() {
    return findChildByClass(MyLanguageFunctionCall.class);
  }

  @Override
  @Nullable
  public MyLanguageVariableReference getVariableReference() {
    return findChildByClass(MyLanguageVariableReference.class);
  }

}
