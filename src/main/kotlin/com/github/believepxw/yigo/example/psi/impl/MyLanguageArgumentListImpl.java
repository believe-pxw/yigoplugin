// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import java.util.List;

import com.github.believepxw.yigo.example.psi.MyLanguageArgumentList;
import com.github.believepxw.yigo.example.psi.MyLanguageExpression;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

public class MyLanguageArgumentListImpl extends ASTWrapperPsiElement implements MyLanguageArgumentList {

  public MyLanguageArgumentListImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitArgumentList(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<MyLanguageExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageExpression.class);
  }

}
