// This is a generated file. Not intended for manual editing.
package example.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static example.psi.MyLanguageTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import example.psi.*;

public class MyLanguageRegularFunctionCallImpl extends ASTWrapperPsiElement implements MyLanguageRegularFunctionCall {

  public MyLanguageRegularFunctionCallImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitRegularFunctionCall(this);
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
  public MyLanguageIifFunctionCall getIifFunctionCall() {
    return findChildByClass(MyLanguageIifFunctionCall.class);
  }

  @Override
  @Nullable
  public MyLanguageJavaMethodCall getJavaMethodCall() {
    return findChildByClass(MyLanguageJavaMethodCall.class);
  }

  @Override
  @Nullable
  public MyLanguageMacroCallExpression getMacroCallExpression() {
    return findChildByClass(MyLanguageMacroCallExpression.class);
  }

  @Override
  @Nullable
  public PsiElement getContainerKeyword() {
    return findChildByType(CONTAINER_KEYWORD);
  }

  @Override
  @Nullable
  public PsiElement getIdentifier() {
    return findChildByType(IDENTIFIER);
  }

  @Override
  @Nullable
  public PsiElement getParentKeyword() {
    return findChildByType(PARENT_KEYWORD);
  }

}
