package example.findusages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
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
        return getPsiElements(xmlAttributeValue);
    }

    public static PsiElement @NotNull [] getPsiElements(XmlAttributeValue xmlAttributeValue) {
        // 返回所有相关的Java元素
        List<PsiElement> elements = new ArrayList<>();
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
            elements.add(field);
            PsiMethod[] getMethod = clazz.findMethodsByName("get" + columnKey, true);
            for (PsiMethod method : getMethod) {
                elements.add(method);
            }
            PsiMethod[] setMethod = clazz.findMethodsByName("set" + columnKey, true);
            for (PsiMethod method : setMethod) {
                elements.add(method);
            }
        }

        return elements.toArray(new PsiElement[0]);
    }
}