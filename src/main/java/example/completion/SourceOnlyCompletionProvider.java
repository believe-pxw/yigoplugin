package example.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

class SourceOnlyCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        Project project = parameters.getPosition().getProject();
        addSourceEntityContextActionMethods(project, result);
    }

    private void addSourceEntityContextActionMethods(Project project, CompletionResultSet result) {
        // 创建只包含源码的搜索范围
        GlobalSearchScope sourceScope = createSourceOnlyScope(project);

        PsiClass entityContextActionClass = JavaPsiFacade.getInstance(project)
                .findClass("com.bokesoft.erp.entity.util.EntityContextAction", sourceScope);

        if (entityContextActionClass == null) {
            return;
        }

        // 在源码范围内搜索继承者
        Query<PsiClass> inheritors = ClassInheritorsSearch.search(
                entityContextActionClass,
                sourceScope,
                true);

        Set<String> addedMethods = new HashSet<>();

        for (PsiClass inheritor : inheritors) {
            // 双重检查确保在源码中
            if (isClassInSourceCode(inheritor, project)) {
                addMethodsFromClass(inheritor, entityContextActionClass, addedMethods, result, project);
            }
        }
    }

    private GlobalSearchScope createSourceOnlyScope(Project project) {
        // 项目源码范围
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        return projectScope;
    }

    private boolean isClassInSourceCode(PsiClass psiClass, Project project) {
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return false;
        }

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        // 检查是否在源码或测试源码目录中
        return fileIndex.isInSourceContent(virtualFile) ||
                fileIndex.isInTestSourceContent(virtualFile);
    }

    private void addMethodsFromClass(PsiClass inheritor,
                                     PsiClass baseClass,
                                     Set<String> addedMethods,
                                     CompletionResultSet result,
                                     Project project) {

        // 只获取当前类中定义的方法，而不是所有继承的方法
        PsiMethod[] ownMethods = inheritor.getMethods();

        for (PsiMethod method : ownMethods) {
            if (shouldIncludeMethod(method, baseClass) &&
                    isMethodInSourceCode(method, project) &&
                    !addedMethods.contains(method.getName())) {

                addedMethods.add(method.getName());
                result.addElement(createLookupElement(method));
            }
        }
    }

    private boolean isMethodInSourceCode(PsiMethod method, Project project) {
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && isClassInSourceCode(containingClass, project);
    }

    private boolean shouldIncludeMethod(PsiMethod method, PsiClass baseClass) {
        // 排除构造方法
        if (method.isConstructor()) {
            return false;
        }

        // 排除私有方法
        if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
            return false;
        }

        // 排除Object类的基本方法
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null &&
                "java.lang.Object".equals(containingClass.getQualifiedName())) {
            return false;
        }

        return method.hasModifierProperty(PsiModifier.PUBLIC) ||
                method.hasModifierProperty(PsiModifier.PROTECTED);
    }

    private LookupElement createLookupElement(PsiMethod method) {
        String fullMethodName = getFullMethodName(method);
        return LookupElementBuilder
                .create(fullMethodName)
                .withIcon(method.getIcon(0))
                .withTypeText(getMethodSignature(method))
                .withTailText(getMethodTailText(method))
                .withInsertHandler(new MethodInsertHandler(method));
    }
    private String getFullMethodName(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return method.getName();
        }

        String qualifiedClassName = containingClass.getQualifiedName();
        if (qualifiedClassName == null) {
            return method.getName();
        }
        if (qualifiedClassName.equals("com.bokesoft.erp.ShortNameFunction")) {
            return method.getName();
        }

        return qualifiedClassName + "." + method.getName();
    }

    private String getMethodSignature(PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length == 0) {
            return "()";
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters[i].getType().getPresentableText());
        }
        sb.append(")");
        return sb.toString();
    }

    private String getMethodTailText(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null ? " - " + containingClass.getName() : "";
    }
}