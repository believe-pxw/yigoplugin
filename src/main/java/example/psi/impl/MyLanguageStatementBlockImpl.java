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

public class MyLanguageStatementBlockImpl extends ASTWrapperPsiElement implements MyLanguageStatementBlock {

  public MyLanguageStatementBlockImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitStatementBlock(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public MyLanguageBlockStatement getBlockStatement() {
    return findChildByClass(MyLanguageBlockStatement.class);
  }

  @Override
  @Nullable
  public MyLanguageStatement getStatement() {
    return findChildByClass(MyLanguageStatement.class);
  }

}
