// This is a generated file. Not intended for manual editing.
package example.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MyLanguageBlockStatement extends PsiElement {

  @NotNull
  List<MyLanguageComment> getCommentList();

  @NotNull
  List<MyLanguageStatement> getStatementList();

}
