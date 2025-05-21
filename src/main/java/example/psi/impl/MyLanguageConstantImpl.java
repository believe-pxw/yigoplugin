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

public class MyLanguageConstantImpl extends ASTWrapperPsiElement implements MyLanguageConstant {

  public MyLanguageConstantImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitConstant(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getBraceQuotedString() {
    return findChildByType(BRACE_QUOTED_STRING);
  }

  @Override
  @Nullable
  public PsiElement getDoubleQuotedString() {
    return findChildByType(DOUBLE_QUOTED_STRING);
  }

  @Override
  @Nullable
  public PsiElement getNumberLiteral() {
    return findChildByType(NUMBER_LITERAL);
  }

  @Override
  @Nullable
  public PsiElement getSingleQuotedString() {
    return findChildByType(SINGLE_QUOTED_STRING);
  }

}
