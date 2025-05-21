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
