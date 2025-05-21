// This is a generated file. Not intended for manual editing.
package com.github.believepxw.yigo.example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageIfStatement extends PsiElement {

  @NotNull
  MyLanguageExpression getExpression();

  @NotNull
  List<MyLanguageExpressionStatement> getExpressionStatementList();

  @NotNull
  List<MyLanguageIfStatement> getIfStatementList();

  @NotNull
  List<MyLanguageVariableDeclaration> getVariableDeclarationList();

  @NotNull
  List<MyLanguageWhileStatement> getWhileStatementList();

}
