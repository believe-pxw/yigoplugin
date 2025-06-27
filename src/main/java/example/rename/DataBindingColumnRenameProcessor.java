package example.rename;

import com.github.believepxw.yigo.ref.VariableReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import example.findusages.DataBindingFindUsagesHandler;
import example.findusages.FieldFindUsagesHandler;
import example.ref.DataBindingColumnReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DataBindingColumnRenameProcessor extends RenamePsiElementProcessor {

    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        // 只有当元素是 XmlAttributeValue 并且是 DataBindingColumnReference 的定义时，才处理
        // 或者你可以让它处理所有 DataBindingColumnReference 关联的 XmlAttributeValue
        if (element instanceof XmlAttributeValue) {
            // 这里需要更精确地判断，确保这个 XmlAttributeValue 确实代表一个 Column Key
            // 最简单的方法是检查它的引用是否是 DataBindingColumnReference 并且是定义
            // 但是，对于重命名操作，通常是从定义处发起。
            // 简单起见，我们假设任何作为 DataBindingColumnReference 定义的 XmlAttributeValue 都可以被处理。
            return element.getReference() instanceof DataBindingColumnReference || element.getReference() instanceof VariableReference;
        }
        return false;
    }

    @Override
    public void renameElement(@NotNull PsiElement element, @NotNull String newName, UsageInfo @NotNull [] usages, @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
        if (element instanceof XmlAttributeValue) {
            renameRelatedJavaElements(element, newName, ((XmlAttributeValue) element).getValue());
        }
        super.renameElement(element, newName, usages, listener);

    }

    /**
     * 重命名相关的Java元素
     */
    private void renameRelatedJavaElements(@NotNull PsiElement element, @NotNull String newName, String oldName) {
        // 获取所有相关的 Java 元素
        PsiReference reference = element.getReference();
        PsiElement[] psiElements;
        if (reference instanceof DataBindingColumnReference) {
            psiElements = DataBindingFindUsagesHandler.getPsiElements((XmlAttributeValue) element);
        } else {
            psiElements = FieldFindUsagesHandler.getPsiElements((XmlAttributeValue) element);
        }
        for (PsiElement psiElement : psiElements) {
            if (element == psiElement) {
                continue;
            }
            if (psiElement != null) {
                String tmpNewName = newName;
                if (psiElement instanceof com.intellij.psi.PsiField) {
                    // 重命名字段
                    tmpNewName = newName;
                } else if (psiElement instanceof com.intellij.psi.PsiMethod) {
                    com.intellij.psi.PsiMethod method = (com.intellij.psi.PsiMethod) psiElement;
                    String methodName = method.getName();
                    // 处理getter方法
                    if (methodName.equals("get" + oldName)) {
                        tmpNewName = "get" + newName;
                    }
                    // 处理setter方法
                    else if (methodName.equals("set" + oldName)) {
                        tmpNewName = "set" + newName;
                    }
                    // 处理Loader类中的方法
                    else if (methodName.equals(oldName)) {
                        tmpNewName = newName;
                    }
                }
                Map<? extends PsiElement, String> allRenames = new HashMap<>();
                UsageInfo[] usages = RenameUtil.findUsages(psiElement, tmpNewName, false, false, allRenames);
                RenameUtil.doRename(psiElement, tmpNewName, usages, element.getProject(), null);
            }
        }
    }
}