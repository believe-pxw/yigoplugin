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
  public MyLanguageConfirmMsgCall getConfirmMsgCall() {
    return findChildByClass(MyLanguageConfirmMsgCall.class);
  }

  @Override
  @Nullable
  public MyLanguageRegularFunctionCall getRegularFunctionCall() {
    return findChildByClass(MyLanguageRegularFunctionCall.class);
  }

  @Override
  @Nullable
  public PsiElement getContainerKeyword() {
    return findChildByType(CONTAINER_KEYWORD);
  }

  @Override
  @Nullable
  public PsiElement getParentKeyword() {
    return findChildByType(PARENT_KEYWORD);
  }

}
