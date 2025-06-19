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

public class MyLanguageCodeBlockLiteralImpl extends ASTWrapperPsiElement implements MyLanguageCodeBlockLiteral {

  public MyLanguageCodeBlockLiteralImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitCodeBlockLiteral(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<MyLanguageComment> getCommentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageComment.class);
  }

  @Override
  @NotNull
  public List<MyLanguageStatement> getStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MyLanguageStatement.class);
  }

}
