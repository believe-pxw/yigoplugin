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

public class MyLanguageWhileStatementImpl extends ASTWrapperPsiElement implements MyLanguageWhileStatement {

  public MyLanguageWhileStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MyLanguageVisitor visitor) {
    visitor.visitWhileStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MyLanguageVisitor) accept((MyLanguageVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public MyLanguageExpression getExpression() {
    return findNotNullChildByClass(MyLanguageExpression.class);
  }

  @Override
  @NotNull
  public MyLanguageStatementBlock getStatementBlock() {
    return findNotNullChildByClass(MyLanguageStatementBlock.class);
  }

}
