package example.findusages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataBindingFindUsagesHandler extends FindUsagesHandler {

    private final XmlAttributeValue xmlAttributeValue;

    public DataBindingFindUsagesHandler(@NotNull PsiElement psiElement) {
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
        XmlAttributeValue element = xmlAttributeValue;
        PsiElement tableTag = element.getParent().getParent().getParent();
        String tableKey = ((XmlTag) tableTag).getAttributeValue("Key");
        String columnKey = xmlAttributeValue.getValue();
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
            PsiField field = clazz.findFieldByName(columnKey, true);
            if (field != null) {
                elements.add(field);
            }
            if (isRename) {
                // --- 新增代码：查找字面量等于 columnKey 的地方 ---
                // 遍历clazz中的所有PsiLiteralExpression
                Collection<PsiLiteralExpression> literalExpressions = PsiTreeUtil.findChildrenOfType(clazz, PsiLiteralExpression.class);
                for (PsiLiteralExpression literal : literalExpressions) {
                    Object value = literal.getValue();
                    if (value instanceof String && columnKey.equals(value)) {
                        elements.add(literal);
                    }
                }
            }
            PsiMethod[] getMethod = clazz.findMethodsByName("get" + columnKey, true);
            for (PsiMethod method : getMethod) {
                elements.add(method);
            }
            PsiMethod[] setMethod = clazz.findMethodsByName("set" + columnKey, true);
            for (PsiMethod method : setMethod) {
                elements.add(method);
            }
            classesByName = shortNamesCache.getClassesByName(tableKey + "_Loader", searchScope);
            PsiClass loadClazz = null;
            for (PsiClass psiClass : classesByName) {
                String qualifiedName = psiClass.getQualifiedName();
                // 例如：只查找特定包下的类
                if (qualifiedName != null && qualifiedName.contains("billentity")) {
                    loadClazz = psiClass;
                }
            }
            if (loadClazz != null) {
                PsiMethod[] methodsByName = loadClazz.findMethodsByName(columnKey, true);
                if (methodsByName.length > 0) {
                    for (PsiMethod method : methodsByName) {
                        elements.add(method);
                    }
                }
            }

        }

        return elements.toArray(new PsiElement[0]);
    }

    @Override
    public boolean processElementUsages(@NotNull PsiElement element, @NotNull Processor<? super UsageInfo> processor, @NotNull FindUsagesOptions options) {
        ModuleManager moduleManager = ModuleManager.getInstance(element.getProject());
        Module moduleByName = moduleManager.findModuleByName("erp-entity-core");
        if (moduleByName != null) {
            GlobalSearchScope globalSearchScope = GlobalSearchScope.notScope(GlobalSearchScope.moduleScope(moduleByName));
            options.searchScope = options.searchScope.intersectWith(globalSearchScope);
        }
        moduleByName = moduleManager.findModuleByName("erp-entity-business");
        if (moduleByName != null) {
            GlobalSearchScope globalSearchScope = GlobalSearchScope.notScope(GlobalSearchScope.moduleScope(moduleByName));
            options.searchScope = options.searchScope.intersectWith(globalSearchScope);
        }
        return super.processElementUsages(element, processor, options);
    }
}