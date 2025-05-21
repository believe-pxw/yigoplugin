// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import com.github.believepxw.yigo.example.psi.MyLanguageVariableReference;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import static com.github.believepxw.yigo.example.psi.MyLanguageTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

public class MyLanguageVariableReferenceImpl extends ASTWrapperPsiElement implements MyLanguageVariableReference {

  public MyLanguageVariableReferenceImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitVariableReference(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getIdentifier() {
    return findNotNullChildByType(IDENTIFIER);
  }

}
