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

public class MyLanguageObjectPropertyImpl extends ASTWrapperPsiElement implements MyLanguageObjectProperty {

  public MyLanguageObjectPropertyImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitObjectProperty(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public MyLanguageCodeBlockLiteral getCodeBlockLiteral() {
    return findChildByClass(MyLanguageCodeBlockLiteral.class);
  }

  @Override
  @Nullable
  public MyLanguageExpression getExpression() {
    return findChildByClass(MyLanguageExpression.class);
  }

  @Override
  @Nullable
  public MyLanguageObjectLiteral getObjectLiteral() {
    return findChildByClass(MyLanguageObjectLiteral.class);
  }

  @Override
  @NotNull
  public PsiElement getIdentifier() {
    return findNotNullChildByType(IDENTIFIER);
  }

}
