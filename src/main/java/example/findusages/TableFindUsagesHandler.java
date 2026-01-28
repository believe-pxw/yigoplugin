package example.findusages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TableFindUsagesHandler extends FindUsagesHandler {

    private final XmlAttributeValue xmlAttributeValue;

    public TableFindUsagesHandler(@NotNull PsiElement psiElement) {
        super(psiElement);
        this.xmlAttributeValue = (XmlAttributeValue) psiElement;
    }

    @Override
    public PsiElement @NotNull [] getPrimaryElements() {
        return getPsiElements(xmlAttributeValue, false);
    }

    public static PsiElement @NotNull [] getPsiElements(XmlAttributeValue xmlAttributeValue,boolean isRename) {
        // 返回所有相关的Java元素
        List<PsiElement> elements = new ArrayList<>();
        elements.add(xmlAttributeValue);
        String tableKey = xmlAttributeValue.getValue();
        Project project = xmlAttributeValue.getProject();
        GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(project);
        @NotNull PsiClass[] classesByName = shortNamesCache.getClassesByName(tableKey, searchScope);
        PsiClass clazz = null;
        for (PsiClass psiClass : classesByName) {
            String qualifiedName = psiClass.getQualifiedName();
            // 例如：只查找特定包下的类
            if (qualifiedName != null && qualifiedName.contains("billentity")) {
                clazz = psiClass;
            }
        }
        if (clazz != null) {
            if (isRename) {
                PsiField field = clazz.findFieldByName(tableKey, true);
                if (field != null) {
                    elements.add(field);
                }
                // --- 新增代码：查找字面量等于 columnKey 的地方 ---
                // 遍历clazz中的所有PsiLiteralExpression
                Collection<PsiLiteralExpression> literalExpressions = PsiTreeUtil.findChildrenOfType(clazz, PsiLiteralExpression.class);
                for (PsiLiteralExpression literal : literalExpressions) {
                    Object value = literal.getValue();
                    if (value instanceof String && tableKey.equals(value)) {
                        elements.add(literal);
                    }
                }
            }
            //rename的时候要放后面
            elements.add(clazz);
        }

        return elements.toArray(new PsiElement[0]);
    }


    @Override
    public boolean processElementUsages(@NotNull PsiElement element, @NotNull Processor<? super UsageInfo> processor, @NotNull FindUsagesOptions options) {
        ExcludeModuleUtil.enhanceFindUsagesOptions(element, options);
        return com.intellij.psi.search.searches.ReferencesSearch.search(element, options.searchScope).forEach(reference -> {
            PsiElement refElement = reference.getElement();

            // --- 过滤逻辑开始 ---

            if (PsiTreeUtil.getParentOfType(refElement, PsiJavaCodeReferenceElement.class) != null) {
                PsiElement parent = refElement.getParent();
                // 如果父级是一个引用表达式，且访问的是静态内容
                if (parent instanceof com.intellij.psi.PsiReferenceExpression) {
                    // 将符合条件的引用包装成 UsageInfo 传给 IDE 面板
                    return processor.process(new UsageInfo(reference));
                }
            } else if (refElement != element && PsiTreeUtil.getParentOfType(refElement, XmlElement.class) != null) {
                return processor.process(new UsageInfo(reference));
            }
            return true; // 继续下一个
        });
    }
}