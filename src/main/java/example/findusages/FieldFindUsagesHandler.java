package example.findusages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FieldFindUsagesHandler extends FindUsagesHandler {

    private final XmlAttributeValue xmlAttributeValue;

    public FieldFindUsagesHandler(@NotNull PsiElement psiElement) {
        super(psiElement);
        this.xmlAttributeValue = (XmlAttributeValue) psiElement;
    }

    @Override
    public PsiElement @NotNull [] getPrimaryElements() {
        return getPsiElements(xmlAttributeValue);
    }

    public static PsiElement @NotNull [] getPsiElements(XmlAttributeValue xmlAttributeValue) {
        List<PsiElement> elements = new ArrayList<>();
        elements.add(xmlAttributeValue);
        XmlFile containingFile = (XmlFile) xmlAttributeValue.getContainingFile();
        if (containingFile.getRootTag().getName().equals("Form")) {
            String formKey = containingFile.getRootTag().getAttributeValue("Key");
            String fieldKey = xmlAttributeValue.getText().replace("\"", "");

            Project project = xmlAttributeValue.getProject();
            GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
            PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(project);
            @NotNull PsiClass[] classesByName = shortNamesCache.getClassesByName(formKey, searchScope);
            PsiClass clazz = null;
            for (PsiClass psiClass : classesByName) {
                String qualifiedName = psiClass.getQualifiedName();
                // 例如：只查找特定包下的类
                if (qualifiedName != null && qualifiedName.contains("billentity")) {
                    clazz = psiClass;
                }
            }
            if (clazz != null) {
                PsiField field = clazz.findFieldByName(fieldKey, true);
                elements.add(field);
                PsiMethod[] getMethod = clazz.findMethodsByName("get" + fieldKey, true);
                for (PsiMethod method : getMethod) {
                    elements.add(method);
                }
                PsiMethod[] setMethod = clazz.findMethodsByName("set" + fieldKey, true);
                for (PsiMethod method : setMethod) {
                    elements.add(method);
                }
                classesByName = shortNamesCache.getClassesByName(formKey + "_Loader", searchScope);
                PsiClass loadClazz = null;
                for (PsiClass psiClass : classesByName) {
                    String qualifiedName = psiClass.getQualifiedName();
                    // 例如：只查找特定包下的类
                    if (qualifiedName != null && qualifiedName.contains("billentity")) {
                        loadClazz = psiClass;
                    }
                }
                if (loadClazz != null) {
                    PsiMethod[] methodsByName = loadClazz.findMethodsByName(fieldKey, true);
                    if (methodsByName.length > 0) {
                        for (PsiMethod method : methodsByName) {
                            elements.add(method);
                        }
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
        return super.processElementUsages(element, processor, options);
    }
}