// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi.impl;

import com.github.believepxw.yigo.example.psi.MyLanguageArgumentList;
import com.github.believepxw.yigo.example.psi.MyLanguageFunctionCall;
import com.github.believepxw.yigo.example.psi.MyLanguageJavaMethodCall;
import com.github.believepxw.yigo.example.psi.MyLanguageParentCall;
import com.github.believepxw.yigo.example.psi.MyLanguageVisitor;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import static com.github.believepxw.yigo.example.psi.MyLanguageTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.example.psi.*;

public class MyLanguageFunctionCallImpl extends ASTWrapperPsiElement implements MyLanguageFunctionCall {

  public MyLanguageFunctionCallImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitFunctionCall(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public MyLanguageArgumentList getArgumentList() {
    return findChildByClass(MyLanguageArgumentList.class);
  }

  @Override
  @Nullable
  public MyLanguageJavaMethodCall getJavaMethodCall() {
    return findChildByClass(MyLanguageJavaMethodCall.class);
  }

  @Override
  @Nullable
  public MyLanguageParentCall getParentCall() {
    return findChildByClass(MyLanguageParentCall.class);
  }

  @Override
  @Nullable
  public PsiElement getIdentifier() {
    return findChildByType(IDENTIFIER);
  }

  @Override
  @Nullable
  public PsiElement getMacroIdentifier() {
    return findChildByType(MACRO_IDENTIFIER);
  }

}
